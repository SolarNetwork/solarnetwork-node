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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SSLService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.util.OptionalService;

/**
 * {@link UploadService} using MQTT.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttUploadService implements UploadService, MqttCallbackExtended {

	/** The JSON MIME type. */
	public static final String JSON_MIME_TYPE = "application/json";

	/** The MQTT topic template for node instruction subscription. */
	public static final String NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

	/** The MQTT topic template for node data publication. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%s/datum";

	private static final long MAX_CONNECT_DELAY_MS = 120000L;

	private final ObjectMapper objectMapper;
	private final IdentityService identityService;
	private final TaskScheduler taskScheduler;
	private final OptionalService<SSLService> sslServiceOpt;
	private final OptionalService<ReactorService> reactorServiceOpt;
	private final OptionalService<InstructionExecutionService> instructionExecutionServiceOpt;
	private final AtomicReference<IMqttClient> clientRef;

	private String persistencePath = "var/mqtt";

	private final Logger log = LoggerFactory.getLogger(getClass());

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
	 */
	public MqttUploadService(ObjectMapper objectMapper, IdentityService identityService,
			TaskScheduler taskScheduler, OptionalService<SSLService> sslService,
			OptionalService<ReactorService> reactorService,
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		this.objectMapper = objectMapper;
		this.identityService = identityService;
		this.taskScheduler = taskScheduler;
		this.sslServiceOpt = sslService;
		this.reactorServiceOpt = reactorService;
		this.instructionExecutionServiceOpt = instructionExecutionService;
		this.clientRef = new AtomicReference<IMqttClient>();
	}

	/**
	 * Immediately connect.
	 */
	public void init() {
		if ( taskScheduler != null ) {
			final AtomicLong sleep = new AtomicLong(2000);
			taskScheduler.schedule(new Runnable() {

				@Override
				public void run() {
					try {
						IMqttClient client = client();
						if ( client != null ) {
							return;
						}
					} catch ( RuntimeException e ) {
						// ignore
					}
					long delay = sleep.accumulateAndGet(sleep.get() / 2000, (c, s) -> {
						long d = (s * 2) * 2000;
						if ( d > MAX_CONNECT_DELAY_MS ) {
							d = MAX_CONNECT_DELAY_MS;
						}
						return d;
					});
					log.info("Failed to connect to MQTT server {}, will try again in {}s",
							identityService.getSolarInMqttUrl(), delay / 1000);
					taskScheduler.schedule(this, new Date(System.currentTimeMillis() + delay));
				}
			}, new Date(System.currentTimeMillis() + sleep.get()));
		} else {
			client();
		}
	}

	@Override
	public String getKey() {
		return "MqttUploadService:" + identityService.getSolarInMqttUrl();
	}

	@Override
	public String uploadDatum(Datum data) {
		final Long nodeId = identityService.getNodeId();
		if ( nodeId != null ) {
			IMqttClient client = client();
			if ( client != null ) {
				String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, identityService.getNodeId());
				try {
					client.publish(topic, objectMapper.writeValueAsBytes(data), 1, false);
					return DigestUtils.md5DigestAsHex(
							String.format("%tQ;%s", data.getCreated(), data.getSourceId()).getBytes());
				} catch ( MqttException | IOException e ) {
					log.warn("Error posting datum {} via MQTT @ {}: {}", data, client.getServerURI(),
							e.getMessage());
				}
			}
		}
		return null;
	}

	private void subscribeToTopics(IMqttClient client, Long nodeId) throws MqttException {
		final String instructionTopic = String.format(NODE_INSTRUCTION_TOPIC_TEMPLATE, nodeId);
		client.subscribe(instructionTopic);
	}

	private IMqttClient client() {
		IMqttClient client = clientRef.get();
		if ( client != null ) {
			return client;
		}

		final Long nodeId = identityService.getNodeId();
		if ( nodeId == null ) {
			return null;
		}

		URI uri;
		try {
			uri = new URI(identityService.getSolarInMqttUrl());
		} catch ( NullPointerException | URISyntaxException e1 ) {
			log.error("Invalid MQTT URL: " + identityService.getSolarInMqttUrl());
			return null;
		}

		int port = uri.getPort();
		String scheme = uri.getScheme();
		boolean useSsl = (port == 8883 || "mqtts".equalsIgnoreCase(scheme)
				|| "ssl".equalsIgnoreCase(scheme));

		final String serverUri = (useSsl ? "ssl" : "tcp") + "://" + uri.getHost()
				+ (port > 0 ? ":" + uri.getPort() : "");

		MqttConnectOptions connOptions = new MqttConnectOptions();
		connOptions.setCleanSession(false);
		connOptions.setAutomaticReconnect(true);

		final SSLService sslService = (sslServiceOpt != null ? sslServiceOpt.service() : null);
		if ( useSsl && sslService != null ) {
			connOptions.setSocketFactory(sslService.getSolarInSocketFactory());
		}

		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistencePath);
		MqttClient c = null;
		try {
			c = new MqttClient(serverUri, nodeId.toString(), persistence);
			c.setCallback(this);
			if ( clientRef.compareAndSet(null, c) ) {
				c.connect(connOptions);
				subscribeToTopics(c, nodeId);
				return c;
			}
		} catch ( MqttException e ) {
			log.warn("Error configuring MQTT client", e);
			if ( c != null ) {
				clientRef.compareAndSet(c, null);
			}
		}
		return null;
	}

	@Override
	public void connectionLost(Throwable cause) {
		IMqttClient client = clientRef.get();
		log.info("Connection to MQTT server @ {} lost: {}",
				(client != null ? client.getServerURI() : "N/A"), cause.getMessage());
	}

	private void postInstructionAcks(List<Instruction> instructions) {
		if ( instructions == null || instructions.isEmpty() ) {
			return;
		}
		IMqttClient client = client();
		ReactorService reactor = (reactorServiceOpt != null ? reactorServiceOpt.service() : null);
		if ( client != null ) {
			String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, identityService.getNodeId());
			try {
				client.publish(topic, objectMapper.writeValueAsBytes(instructions), 1, false);
				if ( reactor != null ) {
					// if instructions have a local ID, store ack
					for ( Instruction instr : instructions ) {
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
				JsonNode root = objectMapper.readTree(message.getPayload());
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
								// execute immediately with our executor
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
	public void deliveryComplete(IMqttDeliveryToken token) {
		// nothing to do
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		log.info("{} to MQTT server @ {}", (reconnect ? "Reconnected" : "Connected"), serverURI);
		if ( reconnect ) {
			// re-subscribe
			final Long nodeId = identityService.getNodeId();
			final IMqttClient client = clientRef.get();
			if ( nodeId != null && client != null ) {
				try {
					subscribeToTopics(client, nodeId);
				} catch ( MqttException e ) {
					log.error("Error subscribing to node topics: {}", e.getMessage(), e);
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

	/**
	 * Set the path to store persisted MQTT data.
	 * 
	 * <p>
	 * This directory will be created if it does not already exist.
	 * </p>
	 * 
	 * @param persistencePath
	 *        the path to set; defaults to {@literal var/mqtt}
	 */
	public void setPersistencePath(String persistencePath) {
		this.persistencePath = persistencePath;
	}

}
