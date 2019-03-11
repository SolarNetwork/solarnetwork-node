/* ==================================================================
 * MqttUploadService.java - 7/06/2018 7:34:41 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.upload.mqtt;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SSLService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.BaseDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.io.mqtt.support.MqttServiceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.util.JsonUtils;
import net.solarnetwork.util.OptionalService;

/**
 * {@link UploadService} using MQTT.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttUploadService extends MqttServiceSupport
		implements UploadService, MqttCallbackExtended {

	/** The JSON MIME type. */
	public static final String JSON_MIME_TYPE = "application/json";

	/** The MQTT topic template for node instruction subscription. */
	public static final String NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

	/** The MQTT topic template for node data publication. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%s/datum";

	private final IdentityService identityService;
	private final OptionalService<ReactorService> reactorServiceOpt;
	private final OptionalService<InstructionExecutionService> instructionExecutionServiceOpt;
	private final OptionalService<EventAdmin> eventAdminOpt;

	/**
	 * Constructor.
	 * 
	 * @param objectMapper
	 *        the object mapper to use
	 * @param identityService
	 *        the identity service
	 * @param taskScheduler
	 *        an optional task scheduler to auto-connect with, or
	 *        {@literal null} for no auto-connect support
	 * @param sslService
	 *        the optional SSL service
	 * @param reactorService
	 *        the optional reactor service
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @param eventAdminService
	 *        the event admin service
	 */
	public MqttUploadService(ObjectMapper objectMapper, IdentityService identityService,
			TaskScheduler taskScheduler, OptionalService<SSLService> sslService,
			OptionalService<ReactorService> reactorService,
			OptionalService<InstructionExecutionService> instructionExecutionService,
			OptionalService<EventAdmin> eventAdmin) {
		super(objectMapper, taskScheduler, sslService);
		this.identityService = identityService;
		this.reactorServiceOpt = reactorService;
		this.instructionExecutionServiceOpt = instructionExecutionService;
		this.eventAdminOpt = eventAdmin;
	}

	@Override
	protected String getMqttClientId() {
		final Long nodeId = identityService.getNodeId();
		return (nodeId != null ? nodeId.toString() : null);
	}

	@Override
	protected URI getMqttUri() {
		try {
			return new URI(identityService.getSolarInMqttUrl());
		} catch ( NullPointerException | URISyntaxException e1 ) {
			log.error("Invalid MQTT URL: " + identityService.getSolarInMqttUrl());
			return null;
		}
	}

	@Override
	protected MqttConnectOptions createMqttConnectOptions(URI uri) {
		MqttConnectOptions options = super.createMqttConnectOptions(uri);
		options.setCleanSession(false);
		return options;
	}

	@Override
	public String getKey() {
		return "MqttUploadService:" + identityService.getSolarInMqttUrl();
	}

	@Override
	public String uploadDatum(Datum data) {
		final Long nodeId = identityService.getNodeId();
		if ( nodeId != null ) {
			IMqttClient client = getClient();
			ObjectMapper objectMapper = getObjectMapper();
			if ( client != null ) {
				String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, nodeId);
				try {
					JsonNode jsonData = objectMapper.valueToTree(data);
					client.publish(topic, objectMapper.writeValueAsBytes(jsonData), 1, false);
					postDatumUploadedEvent(data, jsonData);
					return DigestUtils.md5DigestAsHex(
							String.format("%tQ;%s", data.getCreated(), data.getSourceId()).getBytes());
				} catch ( MqttException | IOException e ) {
					log.warn("Error posting datum {} via MQTT @ {}, falling back to batch mode: {}",
							data, client.getServerURI(), e.getMessage());
				}
			}
		}
		return null;
	}

	// post DATUM_UPLOADED events; but with the (possibly transformed) uploaded data so we show just
	// what was actually uploaded
	private void postDatumUploadedEvent(Datum datum, JsonNode node) {
		Map<String, Object> props = JsonUtils.getStringMapFromTree(node);
		if ( props != null && !props.isEmpty() ) {
			if ( !(props.get("samples") instanceof Map<?, ?>) ) {
				// no sample data; this must have been filtered out via transform
				return;
			}

			// convert samples, which can contain nested maps for a/i/s 
			@SuppressWarnings("unchecked")
			Map<String, ?> samples = (Map<String, ?>) props.get("samples");
			props.remove("samples");
			for ( Map.Entry<String, ?> me : samples.entrySet() ) {
				Object val = me.getValue();
				if ( val instanceof Map<?, ?> ) {
					@SuppressWarnings("unchecked")
					Map<String, ?> subMap = (Map<String, ?>) val;
					props.putAll(subMap);
				} else {
					props.put(me.getKey(), val);
				}
			}

			String[] types = BaseDatum.getDatumTypes(datum.getClass());
			if ( types != null && types.length > 0 ) {
				props.put(Datum.DATUM_TYPE_PROPERTY, types[0]);
				props.put(Datum.DATUM_TYPES_PROPERTY, types);
			}
			log.debug("Created {} event with props {}", UploadService.EVENT_TOPIC_DATUM_UPLOADED, props);
			postEvent(new Event(UploadService.EVENT_TOPIC_DATUM_UPLOADED, props));
		}
	}

	private void postEvent(Event event) {
		EventAdmin ea = (eventAdminOpt != null ? eventAdminOpt.service() : null);
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	private void subscribeToTopics(IMqttClient client, Long nodeId) throws MqttException {
		final String instructionTopic = String.format(NODE_INSTRUCTION_TOPIC_TEMPLATE, nodeId);
		client.subscribe(instructionTopic);
	}

	private void publishInstructionAck(Instruction instr) {
		IMqttClient client = getClient();
		if ( client != null ) {
			String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, identityService.getNodeId());
			try {
				ObjectMapper objectMapper = getObjectMapper();

				client.publish(topic, objectMapper.writeValueAsBytes(instr), 1, false);
			} catch ( MqttException | IOException e ) {
				log.warn("Error posting instruction status {} via MQTT @ {}: {}", instr,
						client.getServerURI(), e.getMessage());
			}
		}
	}

	private void postInstructionAcks(List<Instruction> instructions) {
		if ( instructions == null || instructions.isEmpty() ) {
			return;
		}
		IMqttClient client = getClient();
		ReactorService reactor = (reactorServiceOpt != null ? reactorServiceOpt.service() : null);
		if ( client == null ) {
			// save locally for batch upload
			if ( reactor != null ) {
				for ( Instruction instr : instructions ) {
					reactor.storeInstruction(instr);
				}
			}
		} else {
			String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, identityService.getNodeId());
			try {
				ObjectMapper objectMapper = getObjectMapper();
				for ( Instruction instr : instructions ) {
					client.publish(topic, objectMapper.writeValueAsBytes(instr), 1, false);
					if ( reactor != null ) {
						// if instructions have a local ID, store ack
						if ( instr.getId() != null && instr.getStatus() != null ) {
							BasicInstruction ackInstr = new BasicInstruction(instr.getId(),
									instr.getTopic(), instr.getInstructionDate(),
									instr.getRemoteInstructionId(), instr.getInstructorId(),
									instr.getStatus().newCopyWithAcknowledgedState(
											instr.getStatus().getInstructionState()));
							reactor.storeInstruction(ackInstr);
						}
					}
				}
			} catch ( MqttException | IOException e ) {
				if ( reactor != null ) {
					log.warn(
							"Error posting instruction statuses {} via MQTT @ {}, will defer acknowledgement: {}",
							instructions, client.getServerURI(), e.getMessage());
					for ( Instruction instr : instructions ) {
						reactor.storeInstruction(instr);
					}
				} else {
					log.error("Error posting instruction statuses {} via MQTT @ {}: {}", instructions,
							client.getServerURI(), e.getMessage());
				}
			}
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			// look for and process instructions from message body, as JSON array
			ReactorService reactor = (reactorServiceOpt != null ? reactorServiceOpt.service() : null);
			if ( reactor != null ) {
				JsonNode root = getObjectMapper().readTree(message.getPayload());
				JsonNode instrArray = root.path("instructions");
				if ( instrArray != null && instrArray.isArray() ) {
					InstructionExecutionService executor = (instructionExecutionServiceOpt != null
							? instructionExecutionServiceOpt.service()
							: null);
					List<Instruction> resultInstructions = new ArrayList<>(8);
					// manually parse instruction, so we can immediately execute
					List<Instruction> instructions = reactor.parseInstructions(
							identityService.getSolarInMqttUrl(), instrArray, JSON_MIME_TYPE, null);
					for ( Instruction instr : instructions ) {
						try {
							InstructionStatus status = null;
							if ( executor != null ) {
								// execute immediately with our executor; pass Executing status back first
								publishInstructionAck(new BasicInstruction(instr.getId(),
										instr.getTopic(), instr.getInstructionDate(),
										instr.getRemoteInstructionId(), instr.getInstructorId(),
										new BasicInstructionStatus(instr.getId(),
												InstructionState.Executing, new Date())));
								status = executor.executeInstruction(instr);
							}
							if ( status == null ) {
								// execution didn't happen, so pass to deferred executor
								status = reactor.processInstruction(instr);
							}
							if ( status == null ) {
								// deferred executor didn't handle, so decline
								status = new BasicInstructionStatus(instr.getId(),
										InstructionStatus.InstructionState.Declined, new Date());
							}
							resultInstructions.add(new BasicInstruction(instr.getId(), instr.getTopic(),
									instr.getInstructionDate(), instr.getRemoteInstructionId(),
									instr.getInstructorId(), status));
						} catch ( Exception e ) {
							log.error("Error handling instruction {}", instr, e);
						}
					}
					postInstructionAcks(resultInstructions);
				}
			}
		} catch ( RuntimeException e ) {
			log.error("Error handling MQTT message on topic {}", topic, e);
		}
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		super.connectComplete(reconnect, serverURI);
		// re-subscribe
		final Long nodeId = identityService.getNodeId();
		final IMqttClient client = getClient();
		if ( nodeId != null && client != null ) {
			try {
				subscribeToTopics(client, nodeId);
			} catch ( MqttException e ) {
				log.error("Error subscribing to node topics: {}", e.getMessage(), e);
				TaskScheduler taskScheduler = getTaskScheduler();
				if ( taskScheduler != null ) {
					taskScheduler.schedule(new Runnable() {

						@Override
						public void run() {
							try {
								client.disconnect();
							} catch ( MqttException e ) {
								log.warn("Error disconnecting from MQTT server @ {}: {}", serverURI,
										e.getMessage());
							}
						}
					}, new Date(System.currentTimeMillis() + 20));
				}
			}
		}
	}

}
