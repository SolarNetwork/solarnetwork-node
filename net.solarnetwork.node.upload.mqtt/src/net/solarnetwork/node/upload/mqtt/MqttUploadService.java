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

import static java.lang.String.format;
import static net.solarnetwork.node.service.DatumEvents.datumEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.BasicMqttProperty;
import net.solarnetwork.common.mqtt.MqttBasicCount;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttPropertyType;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttVersion;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.UploadService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StatTracker;

/**
 * {@link UploadService} using MQTT.
 *
 * @author matt
 * @version 2.3
 */
public class MqttUploadService extends BaseMqttConnectionService
		implements UploadService, MqttMessageHandler, MqttConnectionObserver, SettingSpecifierProvider {

	/** The JSON MIME type. */
	public static final String JSON_MIME_TYPE = "application/json";

	/** The MQTT topic template for node instruction subscription. */
	public static final String NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

	/** The MQTT topic template for node data publication. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%s/datum";

	/** The default value for the {@code includeVersionTag} property. */
	public static final boolean DEFAULT_INCLUDE_VERSION_TAG = true;

	/** A tag to indicate that CBOR encoding v2 is in use. */
	public static final String TAG_VERSION_2 = "_v2";

	/**
	 * The default MQTT version to use.
	 *
	 * @since 1.4
	 */
	public static final MqttVersion DEFAULT_MQTT_VERSION = MqttVersion.Mqtt5;

	/**
	 * A source ID for log messages posted as datum.
	 *
	 * @since 2.1
	 */
	public static final String LOG_SOURCE_ID = "log";

	/**
	 * A source ID prefix for log messages posted as datum.
	 *
	 * @since 2.1
	 */
	public static final String LOG_SOURCE_ID_PREFIX = LOG_SOURCE_ID + "/";

	private final ObjectMapper objectMapper;
	private final IdentityService identityService;
	private final OptionalService<ReactorService> reactorServiceOpt;
	private final OptionalService<InstructionExecutionService> instructionExecutionServiceOpt;
	private final OptionalService<EventAdmin> eventAdminOpt;
	private final OptionalService<DatumMetadataService> datumMetadataServiceOpt;
	private Executor executor;
	private boolean includeVersionTag = DEFAULT_INCLUDE_VERSION_TAG;

	private CompletableFuture<?> startupFuture;

	/**
	 * Constructor.
	 *
	 * @param connectionFactory
	 *        the MQTT connection factory
	 * @param objectMapper
	 *        the object mapper to use
	 * @param identityService
	 *        the identity service
	 * @param reactorService
	 *        the optional reactor service
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @param eventAdmin
	 *        the event admin service
	 * @param datumMetadataService
	 *        the datum metadata service
	 */
	public MqttUploadService(MqttConnectionFactory connectionFactory, ObjectMapper objectMapper,
			IdentityService identityService, OptionalService<ReactorService> reactorService,
			OptionalService<InstructionExecutionService> instructionExecutionService,
			OptionalService<EventAdmin> eventAdmin,
			OptionalService<DatumMetadataService> datumMetadataService) {
		super(connectionFactory, new StatTracker("SolarIn/MQTT", null,
				LoggerFactory.getLogger(MqttUploadService.class), 100));
		this.objectMapper = objectMapper;
		this.identityService = identityService;
		this.reactorServiceOpt = reactorService;
		this.instructionExecutionServiceOpt = instructionExecutionService;
		this.eventAdminOpt = eventAdmin;
		this.datumMetadataServiceOpt = datumMetadataService;
		setPublishQos(MqttQos.AtLeastOnce);
		getMqttConfig().setUid("SolarIn/MQTT");
		getMqttConfig().setVersion(DEFAULT_MQTT_VERSION);
		// we only subscribe to one topic, so set the max topic alias to a small number here
		getMqttConfig().getProperties()
				.addProperty(new BasicMqttProperty<Integer>(MqttPropertyType.TOPIC_ALIAS_MAXIMUM, 7));
		//getMqttConfig().setWireLoggingEnabled(true);
		setDisplayName("SolarIn/MQTT");
	}

	@Override
	public String getDisplayName() {
		URI uri = getMqttConfig().getServerUri();
		if ( uri == null ) {
			return super.getDisplayName();
		}
		return String.format("%s @ %s", super.getDisplayName(), uri);
	}

	@Override
	public synchronized Future<?> startup() {
		if ( startupFuture != null ) {
			return startupFuture;
		}
		// bump to another thread, because getMqttUri() can block
		Executor e = this.executor;
		CompletableFuture<Void> result = new CompletableFuture<Void>();
		Runnable startup = new Runnable() {

			@Override
			public void run() {
				final String clientId = getMqttClientId();
				final URI uri = getMqttUri();
				if ( clientId == null || uri == null ) {
					// defer!
					log.info(
							"Node ID or SolarIn/MQTT URI not available yet, waiting to try to connect to SolarIn/MQTT.");
					try {
						Thread.sleep(60_000L);
					} catch ( InterruptedException e ) {
						// ignore
					}
					if ( e != null ) {
						e.execute(this);
					} else {
						run();
					}
				} else {
					getMqttConfig().setClientId(clientId);
					getMqttConfig().setServerUri(uri);
					Future<?> f = MqttUploadService.super.startup();
					try {
						f.get();
						result.complete(null);
					} catch ( Exception e ) {
						result.completeExceptionally(e);
					} finally {
						synchronized ( MqttUploadService.this ) {
							if ( startupFuture == result ) {
								startupFuture = null;
							}
						}
					}
				}
			}
		};
		if ( e != null ) {
			e.execute(startup);
		} else {
			startup.run();
		}
		this.startupFuture = result;
		return result;
	}

	@Override
	public synchronized void shutdown() {
		if ( startupFuture != null ) {
			try {
				startupFuture.cancel(true);
			} catch ( Exception e ) {
				// ignore
			}
			startupFuture = null;
		}
		super.shutdown();
	}

	private String getMqttClientId() {
		final Long nodeId = identityService.getNodeId();
		return (nodeId != null ? nodeId.toString() : null);
	}

	private URI getMqttUri() {
		final String uri = identityService.getSolarInMqttUrl();
		try {
			return new URI(uri);
		} catch ( NullPointerException e ) {
			// perhaps not configured yet
			return null;
		} catch ( URISyntaxException e ) {
			log.error("Invalid MQTT URL: " + identityService.getSolarInMqttUrl());
			return null;
		}
	}

	@Override
	public String getKey() {
		return "MqttUploadService:" + getMqttConfig().getServerUri();
	}

	@Override
	public String uploadDatum(NodeDatum datum) {
		final Long nodeId = identityService.getNodeId();
		final DatumMetadataService datumMetadataService = OptionalService
				.service(datumMetadataServiceOpt);
		if ( nodeId != null ) {
			MqttConnection conn = connection();
			if ( conn != null ) {
				String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, nodeId);
				try {
					byte[] messageData = null;
					JsonNode jsonData = objectMapper.valueToTree(datum);
					ObjectDatumKind kind = datum.getKind();
					Long objectId = (kind == ObjectDatumKind.Node ? nodeId : datum.getObjectId());
					if ( datumMetadataService != null ) {
						// try to post as stream datum, if metadata available
						ObjectDatumStreamMetadata meta = datumMetadataService
								.getDatumStreamMetadata(kind, objectId, datum.getSourceId());
						if ( meta != null ) {
							// we've got stream metadata: post as this if all properties accounted for
							try {
								DatumProperties datumProps = DatumProperties.propertiesFrom(datum, meta);
								if ( datumProps != null ) {
									BasicStreamDatum streamDatum = new BasicStreamDatum(
											meta.getStreamId(), datum.getTimestamp(), datumProps);
									messageData = objectMapper.writeValueAsBytes(streamDatum);
								}
							} catch ( IllegalArgumentException e ) {
								if ( canLogForDatum(datum) ) {
									log.debug(
											"Unable to post datum as stream datum, falling back to general datum: "
													+ e.getMessage());
								}
							}
						}
					}
					if ( messageData == null ) {
						if ( includeVersionTag ) {
							JsonNode tagsData = jsonData.path("t");
							ArrayNode tagsArrayNode = null;
							if ( tagsData.isArray() ) {
								tagsArrayNode = (ArrayNode) tagsData;
							} else if ( tagsData.isNull() || tagsData.isMissingNode() ) {
								tagsArrayNode = ((JsonNodeCreator) jsonData).arrayNode(1);
								((ObjectNode) jsonData).set("t", tagsArrayNode);
							}
							if ( tagsArrayNode != null ) {
								boolean found = false;
								for ( JsonNode t : tagsArrayNode ) {
									if ( TAG_VERSION_2.equals(t.textValue()) ) {
										found = true;
										break;
									}
								}
								if ( !found ) {
									tagsArrayNode.add(TAG_VERSION_2);
								}
							}
						}
						messageData = objectMapper.writeValueAsBytes(jsonData);
					}
					if ( messageData != null && messageData.length > 0 ) {
						conn.publish(new BasicMqttMessage(topic, false, getPublishQos(), messageData))
								.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
						getMqttStats().increment(
								kind == ObjectDatumKind.Location ? SolarInCountStat.LocationDatumPosted
										: SolarInCountStat.NodeDatumPosted);
						postDatumUploadedEvent(datum, jsonData);
						if ( canLogForDatum(datum) ) {
							log.info("Uploaded datum via MQTT: {}", datum);
						}
					}
					return DigestUtils.md5DigestAsHex(String.format("%tQ;%s;%d;%s", datum.getTimestamp(),
							datum.getSourceId(), objectId, kind).getBytes());
				} catch ( IOException | InterruptedException | ExecutionException
						| TimeoutException e ) {
					if ( canLogForDatum(datum) ) {
						Throwable root = e;
						while ( root.getCause() != null ) {
							root = root.getCause();
						}
						String msg = (root instanceof TimeoutException ? "timeout" : root.getMessage());
						if ( log.isDebugEnabled() ) {
							log.warn("Error posting datum {} via MQTT @ {}, falling back to batch mode",
									datum, getMqttConfig().getServerUri(), e);
						} else {
							log.warn(
									"Error posting datum {} via MQTT @ {}, falling back to batch mode: {}",
									datum, getMqttConfig().getServerUri(), msg);
						}
					}
				}
			}
		}
		return null;
	}

	private static boolean canLogForDatum(NodeDatum datum) {
		return !(LOG_SOURCE_ID.equalsIgnoreCase(datum.getSourceId())
				|| datum.getSourceId().startsWith(LOG_SOURCE_ID_PREFIX));
	}

	// post DATUM_UPLOADED events; but with the (possibly transformed) uploaded data so we show just
	// what was actually uploaded
	private void postDatumUploadedEvent(Datum datum, JsonNode node) {
		Map<String, Object> props = JsonUtils.getStringMapFromTree(node);
		Event event = datumEvent(UploadService.EVENT_TOPIC_DATUM_UPLOADED, datum.getClass(), props);
		postEvent(event);
	}

	private void postEvent(Event event) {
		EventAdmin ea = (eventAdminOpt != null ? eventAdminOpt.service() : null);
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	private boolean publishInstructionAck(MqttConnection conn, Long nodeId, Instruction instr) {
		if ( conn != null && nodeId != null && instr != null && instr.getStatus() != null ) {
			final ReactorService reactor = OptionalService.service(reactorServiceOpt);
			final String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, nodeId);
			final InstructionStatus status = instr.getStatus();
			try {
				conn.publish(new BasicMqttMessage(topic, false, getPublishQos(),
						objectMapper.writeValueAsBytes(status)))
						.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
				getMqttStats().increment(SolarInCountStat.InstructionStatusPosted);
				log.info("Posted Instruction {} [{}] acknowledgement status: {}",
						status.getInstructionId(), instr.getTopic(), instr.getInstructionState());
				return true;
			} catch ( Exception e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				String msg = (root instanceof TimeoutException ? "timeout" : e.getMessage());
				if ( reactor != null ) {
					log.warn(
							"Error posting instruction status {} via MQTT @ {}, will defer acknowledgement: {}",
							instr, getMqttConfig().getServerUri(), msg);
				} else {
					log.error("Error posting instruction status {} via MQTT @ {}: {}", instr,
							getMqttConfig().getServerUri(), msg);
				}
			}
		}
		return false;
	}

	private void postInstructionAck(ReactorService reactor, MqttConnection conn, Long nodeId,
			Instruction instr) {
		if ( instr == null ) {
			return;
		}
		if ( conn == null || nodeId == null ) {
			// save locally for batch upload
			if ( reactor != null ) {
				reactor.storeInstruction(instr);
			}
		} else {
			if ( publishInstructionAck(conn, nodeId, instr) ) {
				if ( reactor != null ) {
					// if instructions have a local ID, store ack
					if ( instr.getId() != null && instr.getStatus() != null ) {
						BasicInstruction ackInstr = new BasicInstruction(instr, instr.getStatus()
								.newCopyWithAcknowledgedState(instr.getStatus().getInstructionState()));
						reactor.storeInstruction(ackInstr);
					}
				}
			} else if ( reactor != null ) {
				reactor.storeInstruction(instr);
			}
		}
	}

	@Override
	public void onMqttMessage(MqttMessage message) {
		final Executor exec = this.executor;
		if ( exec != null ) {
			exec.execute(() -> handleMqttMessage(message));
		} else {
			handleMqttMessage(message);
		}
	}

	private void handleMqttMessage(MqttMessage message) {
		final String topic = message.getTopic();
		// look for and process instructions from message body, as JSON array
		final ReactorService reactor = OptionalService.service(reactorServiceOpt);
		if ( reactor == null ) {
			return;
		}
		final InstructionExecutionService executor = OptionalService
				.service(instructionExecutionServiceOpt);
		final String instructorId = identityService.getSolarInBaseUrl();
		try {
			JsonNode root = objectMapper.readTree(message.getPayload());
			JsonNode instrArray = root.path("instructions");
			if ( instrArray == null || !instrArray.isArray() ) {
				return;
			}

			final MqttConnection conn = connection();
			final Long nodeId = identityService.getNodeId();
			for ( JsonNode instrNode : instrArray ) {
				try {
					net.solarnetwork.domain.Instruction commonInstr = objectMapper.treeToValue(instrNode,
							net.solarnetwork.domain.Instruction.class);
					if ( commonInstr == null ) {
						continue;
					}
					Instruction instr = BasicInstruction.from(commonInstr, instructorId);
					if ( log.isInfoEnabled() ) {
						log.info("Instruction {} {} received with parameters: {}", instr.getId(),
								instr.getTopic(), instr.getParameterMap());
					}
					getMqttStats().increment(SolarInCountStat.InstructionsReceived);

					// check for future execution date
					final Instant executeAt = instr.getExecutionDate();
					final boolean futureExecution = (executeAt != null
							&& executeAt.isAfter(Instant.now()));

					InstructionStatus status = null;
					if ( executor != null && !futureExecution ) {
						// save with Executing state immediately, to prevent reactor job from picking up
						status = new BasicInstructionStatus(instr.getId(), InstructionState.Executing,
								Instant.now());
						instr = new BasicInstruction(instr, status);
						reactor.storeInstruction(instr);

						// execute immediately with our executor; pass Executing status back first
						publishInstructionAck(conn, nodeId, instr);
						status = executor.executeInstruction(instr);

						if ( status == null ) {
							log.info(
									"No handler available for instruction {} {}: deferring to Received state",
									instr.getId(), instr.getTopic());
							// instruction not handled: change instruction status to Received
							status = new BasicInstructionStatus(instr.getId(), InstructionState.Received,
									Instant.now());
						}
					} else {
						// execution didn't happen, so pass to deferred executor
						status = reactor.processInstruction(instr);
						if ( futureExecution ) {
							log.info("Deferred instruction {} {} saved as {} state for execution @ {}",
									instr.getId(), instr.getTopic(), status.getInstructionState(),
									executeAt);
						}
					}
					if ( status == null ) {
						// deferred executor didn't handle, so decline
						status = new BasicInstructionStatus(instr.getId(), InstructionState.Declined,
								Instant.now());
					}
					instr = new BasicInstruction(instr, status);
					reactor.storeInstruction(instr);
					postInstructionAck(reactor, conn, nodeId, instr);
				} catch ( Exception e ) {
					log.warn("Unable to accept instruction JSON [{}]: {}", instrNode, e.toString());
				}
			}
		} catch ( RuntimeException | IOException e ) {
			log.error("Error handling MQTT message on topic {}", topic, e);
		}
	}

	@Override
	public String getPingTestName() {
		return getDisplayName();
	}

	@Override
	public Result performPingTest() throws Exception {
		Result r = super.performPingTest();
		Map<String, Object> props = new LinkedHashMap<>(8);
		if ( r.getProperties() != null ) {
			props.putAll(r.getProperties());
		}
		props.putAll(getMqttStats().allCounts());
		return new PingTestResult(r.isSuccess(), r.getMessage(), props);
	}

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		// ignore
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		Long nodeId = identityService.getNodeId();
		if ( nodeId == null ) {
			return;
		}
		final String instructionTopic = String.format(NODE_INSTRUCTION_TOPIC_TEMPLATE, nodeId);
		Future<?> f = connection.subscribe(instructionTopic, getSubscribeQos(), null);
		try {
			f.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			String msg = (root instanceof TimeoutException ? "timeout" : root.getMessage());
			log.error("Error subscribing to MQTT topic {} @ {}: {}", instructionTopic,
					getMqttConfig().getServerUri(), msg);
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.upload.mqtt";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		SettingSpecifier stats = new BasicTitleSettingSpecifier("status", getStatusMessage(), true,
				true);
		return Collections.singletonList(stats);
	}

	private String getStatusMessage() {
		final MqttConnection conn = connection();
		final boolean connected = (conn != null ? conn.isEstablished() : false);
		final String connMsg = getMessageSource().getMessage(
				format("status.%s", connected ? "connected" : "disconnected"), null,
				Locale.getDefault());
		final URI uri = getMqttUri();
		final StatTracker s = getMqttStats();
		// @formatter:off
		return getMessageSource().getMessage("status.msg",
				new Object[] {
						connMsg,
						uri != null ? uri : "N/A",
						s.get(SolarInCountStat.NodeDatumPosted),
						s.get(SolarInCountStat.LocationDatumPosted),
						s.get(MqttBasicCount.PayloadBytesDelivered),
						s.get(SolarInCountStat.InstructionsReceived),
						s.get(SolarInCountStat.InstructionStatusPosted),
						s.get(MqttBasicCount.PayloadBytesReceived) },
				Locale.getDefault());
		// @formatter:on
	}

	/**
	 * Set an executor to use for internal tasks.
	 *
	 * @param executor
	 *        the executor
	 * @since 1.2
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Get the "include version tag" toggle.
	 *
	 * @return {@literal true} to include the {@literal _v} version tag with
	 *         each datum; defaults to {@link #DEFAULT_INCLUDE_VERSION_TAG}
	 * @since 1.3
	 */
	public boolean isIncludeVersionTag() {
		return includeVersionTag;
	}

	/**
	 * Set the "include version tag" toggle.
	 *
	 * @param includeVersionTag
	 *        {@literal true} to include the {@literal _v} version tag with each
	 *        datum; only disable if you can be sure that all receivers of
	 *        SolarFlux messages interpret the data in the same way
	 * @since 1.3
	 */
	public void setIncludeVersionTag(boolean includeVersionTag) {
		this.includeVersionTag = includeVersionTag;
	}

	/**
	 * Set the MQTT version to use.
	 *
	 * @param mqttVersion
	 *        the version, or {@literal null} for a default version
	 * @since 1.4
	 */
	public void setMqttVersion(MqttVersion mqttVersion) {
		getMqttConfig().setVersion(mqttVersion != null ? mqttVersion : DEFAULT_MQTT_VERSION);
	}

}
