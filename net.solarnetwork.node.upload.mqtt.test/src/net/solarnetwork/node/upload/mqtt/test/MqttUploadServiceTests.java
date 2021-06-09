/* ==================================================================
 * MqttUploadServiceTests.java - 8/06/2018 4:01:51 PM
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

package net.solarnetwork.node.upload.mqtt.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.solarnetwork.codec.BasicStreamDatumArraySerializer;
import net.solarnetwork.common.mqtt.BasicMqttConnectionConfig;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttVersion;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.reactor.io.json.JsonReactorSerializationService;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.support.DatumSerializer;
import net.solarnetwork.node.support.GeneralNodeDatumSerializer;
import net.solarnetwork.node.support.InstructionSerializer;
import net.solarnetwork.node.support.NodeControlInfoSerializer;
import net.solarnetwork.node.upload.mqtt.MqttUploadService;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link MqttUploadService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class MqttUploadServiceTests extends MqttServerSupport {

	private static final int MQTT_TIMEOUT = 10;
	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final String TEST_SOLARIN_BASE_URL = "http://localhost:8680";

	private IdentityService identityService;
	private ReactorService reactorService;
	private InstructionExecutionService instructionExecutionService;
	private EventAdmin eventAdminService;
	private DatumMetadataService datumMetadataService;
	private MqttConnectionFactory connectionFactory;
	private ObjectMapper objectMapper;
	private MqttUploadService service;
	private Long nodeId;

	private class TestIdentityService implements IdentityService {

		@Override
		public Long getNodeId() {
			return nodeId;
		}

		@Override
		public Principal getNodePrincipal() {
			return null;
		}

		@Override
		public String getSolarNetHostName() {
			return null;
		}

		@Override
		public Integer getSolarNetHostPort() {
			return null;
		}

		@Override
		public String getSolarNetSolarInUrlPrefix() {
			return null;
		}

		@Override
		public String getSolarInBaseUrl() {
			return TEST_SOLARIN_BASE_URL;
		}

		@Override
		public String getSolarInMqttUrl() {
			return "mqtt://localhost:" + getMqttServerPort();
		}

	}

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		if ( jsonFactory != null ) {
			factory.setJsonFactory(jsonFactory);
		}
		factory.setSerializers(Arrays.asList(new GeneralNodeDatumSerializer(), new DatumSerializer(),
				new InstructionSerializer(), new NodeControlInfoSerializer(),
				BasicStreamDatumArraySerializer.INSTANCE));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setup() throws Exception {
		setupMqttServer();

		identityService = new TestIdentityService();
		reactorService = EasyMock.createMock(ReactorService.class);
		instructionExecutionService = EasyMock.createMock(InstructionExecutionService.class);
		eventAdminService = EasyMock.createMock(EventAdmin.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		objectMapper = createObjectMapper(null);

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);
		this.connectionFactory = factory;

		service = new MqttUploadService(factory, objectMapper, identityService,
				new StaticOptionalService<ReactorService>(reactorService),
				new StaticOptionalService<InstructionExecutionService>(instructionExecutionService),
				new StaticOptionalService<EventAdmin>(eventAdminService),
				new StaticOptionalService<DatumMetadataService>(datumMetadataService));
		service.getMqttConfig().setClientId(TEST_CLIENT_ID);
		service.getMqttConfig().setVersion(MqttVersion.Mqtt311);
		service.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));

		nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());

		Future<?> f = service.startup();
		f.get(MQTT_TIMEOUT, TimeUnit.SECONDS);
	}

	private MqttConnection createMqttClient(String clientId, MqttMessageHandler messageHandler) {
		BasicMqttConnectionConfig config = new BasicMqttConnectionConfig(service.getMqttConfig());
		config.setClientId(clientId);
		MqttConnection conn = connectionFactory.createConnection(config);
		conn.setMessageHandler(messageHandler);
		try {
			conn.open().get(MQTT_TIMEOUT, TimeUnit.SECONDS);
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
		return conn;
	}

	@Override
	@After
	public void teardown() {
		super.teardown();
		EasyMock.verify(reactorService, instructionExecutionService, eventAdminService,
				datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(reactorService, instructionExecutionService, eventAdminService,
				datumMetadataService);
	}

	@Test
	public void serviceKey() {
		// given
		URI serverUri = service.getMqttConfig().getServerUri();

		replayAll();

		// when
		String key = ((UploadService) service).getKey();

		// then
		assertThat("UploadService key", key, equalTo("MqttUploadService:" + serverUri));
	}

	@Test
	public void subscribes() throws Exception {
		replayAll();

		// give a little time for subscription to take
		Thread.sleep(500);

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Subscribed ", session.subscribeMessages, hasSize(1));

		InterceptSubscribeMessage subMsg = session.subscribeMessages.get(0);
		assertThat("Subscribe topic", subMsg.getTopicFilter(), equalTo(instructionTopic(nodeId)));
		assertThat("Subscribe QOS", subMsg.getRequestedQos(), equalTo(MqttQoS.AT_LEAST_ONCE));
	}

	private String datumTopic(Long nodeId) {
		return String.format(MqttUploadService.NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	private String instructionTopic(Long nodeId) {
		return String.format(MqttUploadService.NODE_INSTRUCTION_TOPIC_TEMPLATE, nodeId);
	}

	@Test
	public void uploadWithConnectionToMqttServer() throws IOException {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId("test.source");
		datum.putInstantaneousSampleValue("foo", 123);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, nodeId,
				datum.getSourceId())).andReturn(null);

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// when
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// then
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(true));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));

		datum.addTag(MqttUploadService.TAG_VERSION_2);
		assertThat("Publish payload in stream form", session.getPublishPayloadStringAtIndex(0),
				equalTo(objectMapper.writeValueAsString(datum)));

		Event datumUploadEvent = eventCaptor.getValue();
		assertThat("Event topic", datumUploadEvent.getTopic(),
				equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
		assertThat("Event prop 'foo'", datumUploadEvent.getProperty("foo"), equalTo((Object) 123));
	}

	@Test
	public void uploadWithConnectionToMqttServer_stream() throws IOException {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId("test.source");
		datum.putInstantaneousSampleValue("foo", 123);

		// stream metadata available
		BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"Pacific/Auckland", ObjectDatumKind.Node, nodeId, "test.source", new String[] { "foo" },
				null, null);
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, nodeId,
				datum.getSourceId())).andReturn(meta);

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// WHEN
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(true));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));

		// StreamDatum published
		DatumProperties props = DatumProperties.propertiesFrom(datum, meta);
		BasicStreamDatum streamDatum = new BasicStreamDatum(meta.getStreamId(), datum.getTimestamp(),
				props);
		assertThat("Publish payload", session.getPublishPayloadStringAtIndex(0),
				equalTo(objectMapper.writeValueAsString(streamDatum)));

		Event datumUploadEvent = eventCaptor.getValue();
		assertThat("Event topic", datumUploadEvent.getTopic(),
				equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
		assertThat("Event prop 'foo'", datumUploadEvent.getProperty("foo"), equalTo((Object) 123));
	}

	@Test
	public void uploadWithConnectionToMqttServer_streamMismatch() throws IOException {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId("test.source");
		datum.putInstantaneousSampleValue("foo", 123);

		// stream metadata available
		BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"Pacific/Auckland", ObjectDatumKind.Node, nodeId, "test.source", new String[] { "bar" },
				null, null);
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, nodeId,
				datum.getSourceId())).andReturn(meta);

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// WHEN
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(true));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));

		// fall back to general datum because of stream  mis-match
		datum.addTag(MqttUploadService.TAG_VERSION_2);
		assertThat("Publish payload in general form", session.getPublishPayloadStringAtIndex(0),
				equalTo(objectMapper.writeValueAsString(datum)));

		Event datumUploadEvent = eventCaptor.getValue();
		assertThat("Event topic", datumUploadEvent.getTopic(),
				equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
		assertThat("Event prop 'foo'", datumUploadEvent.getProperty("foo"), equalTo((Object) 123));
	}

	@Test
	public void uploadLocationDatumWithConnectionToMqttServer() throws IOException {
		// given
		Long locationId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setLocationId(locationId);
		datum.setSourceId("test.source");
		datum.putInstantaneousSampleValue("foo", 123);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Location, locationId,
				datum.getSourceId())).andReturn(null);

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// when
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// then
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(true));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));

		datum.addTag(MqttUploadService.TAG_VERSION_2);
		assertThat("Publish payload", session.getPublishPayloadStringAtIndex(0),
				equalTo(objectMapper.writeValueAsString(datum)));

		Event datumUploadEvent = eventCaptor.getValue();
		assertThat("Event topic", datumUploadEvent.getTopic(),
				equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
		assertThat("Event prop 'locationId'", datumUploadEvent.getProperty("locationId"),
				equalTo((Object) locationId));
		assertThat("Event prop 'foo'", datumUploadEvent.getProperty("foo"), equalTo((Object) 123));
	}

	@Test
	public void uploadLocationDatumWithConnectionToMqttServer_stream() throws IOException {
		// GIVEN
		Long locationId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new Date());
		datum.setLocationId(locationId);
		datum.setSourceId("test.source");
		datum.putInstantaneousSampleValue("foo", 123);

		// stream metadata available
		BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"Pacific/Auckland", ObjectDatumKind.Location, locationId, "test.source",
				new String[] { "foo" }, null, null);
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Location, locationId,
				datum.getSourceId())).andReturn(meta);

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// when
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// then
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(true));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));

		// StreamDatum published
		DatumProperties props = DatumProperties.propertiesFrom(datum, meta);
		BasicStreamDatum streamDatum = new BasicStreamDatum(meta.getStreamId(), datum.getTimestamp(),
				props);
		assertThat("Publish payload in stream form", session.getPublishPayloadStringAtIndex(0),
				equalTo(objectMapper.writeValueAsString(streamDatum)));

		Event datumUploadEvent = eventCaptor.getValue();
		assertThat("Event topic", datumUploadEvent.getTopic(),
				equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
		assertThat("Event prop 'locationId'", datumUploadEvent.getProperty("locationId"),
				equalTo((Object) locationId));
		assertThat("Event prop 'foo'", datumUploadEvent.getProperty("foo"), equalTo((Object) 123));
	}

	@Test
	public void uploadWithoutConnectionToMqttServer() throws IOException {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId("test.source");
		datum.putInstantaneousSampleValue("foo", 123);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, nodeId,
				datum.getSourceId())).andReturn(null);

		replayAll();

		stopMqttServer(); // shut down server

		// when
		String txId = service.uploadDatum(datum);

		// then
		assertThat("TX ID", txId, nullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Published datum", session.publishMessages, hasSize(0));
	}

	private String getStringResource(String name) {
		try {
			return FileCopyUtils
					.copyToString(new InputStreamReader(getClass().getResourceAsStream(name), "UTF-8"));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private List<Instruction> parseInstructions(String serverInstructions) {
		try {
			JsonNode node = objectMapper.readTree(serverInstructions);
			JsonNode instructions = node.path("instructions");
			return new JsonReactorSerializationService().decodeInstructions(TEST_SOLARIN_BASE_URL,
					instructions, MqttUploadService.JSON_MIME_TYPE, null);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private static class TestingMqttMessageHandler implements MqttMessageHandler {

		private final List<MqttMessage> messages = Collections.synchronizedList(new ArrayList<>());

		@Override
		public void onMqttMessage(MqttMessage message) {
			messages.add(message);
		}

	}

	@Test
	public void processInstruction() throws Exception {
		// GIVEN
		TestingMqttMessageHandler messageHandler = new TestingMqttMessageHandler();
		MqttConnection solarNetClient = createMqttClient("solarnet", messageHandler);

		// parse instructions
		String testInstructions = getStringResource("instructions-01.json");
		List<Instruction> instructions = parseInstructions(testInstructions);
		assert instructions.size() == 1;
		final String remoteInstructionId = instructions.get(0).getRemoteInstructionId();
		expect(reactorService.parseInstructions(anyObject(), anyObject(JsonNode.class),
				eq(MqttUploadService.JSON_MIME_TYPE), isNull())).andReturn(instructions);

		// persist Executing state
		Capture<Instruction> storeInstructionCaptor = new Capture<>();
		final Long localInstructionId = 98765L;
		expect(reactorService.storeInstruction(capture(storeInstructionCaptor)))
				.andReturn(localInstructionId);

		// execute single instruction
		Capture<Instruction> execInstructionCaptor = new Capture<>();
		InstructionStatus execResultStatus = new BasicInstructionStatus(null,
				InstructionStatus.InstructionState.Completed, new Date());
		expect(instructionExecutionService.executeInstruction(capture(execInstructionCaptor)))
				.andReturn(execResultStatus);

		// save result back to DB
		Capture<Instruction> storeCompletedInstructionCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(storeCompletedInstructionCaptor)))
				.andReturn(localInstructionId);

		// save result back to DB
		Capture<Instruction> storeCompletedInstructionAckCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(storeCompletedInstructionAckCaptor)))
				.andReturn(localInstructionId);

		replayAll();

		// WHEN
		Thread.sleep(1000); // allow time for subscription to take

		String instrTopic = instructionTopic(nodeId);
		solarNetClient.publish(new BasicMqttMessage(instrTopic, false, MqttQos.AtLeastOnce,
				testInstructions.getBytes("UTF-8"))).get(MQTT_TIMEOUT, TimeUnit.SECONDS);

		Thread.sleep(2000); // allow time for messages to process

		stopMqttServer(); // shut down server

		// THEN

		// should have stored Executing status
		Instruction storeInstruction = storeInstructionCaptor.getValue();
		assertThat("Store instruction remote ID", storeInstruction.getRemoteInstructionId(),
				equalTo(remoteInstructionId));
		assertThat("Store instruction has no local ID", storeInstruction.getId(), nullValue());
		assertThat("Store instruction state", storeInstruction.getInstructionState(),
				equalTo(InstructionState.Executing));
		assertThat("Store instruction instructorId", storeInstruction.getInstructorId(),
				equalTo(TEST_SOLARIN_BASE_URL));

		// should have executed Instruction with persisted local ID
		Instruction execInstruction = execInstructionCaptor.getValue();
		assertThat("Exec instruction has persisted local ID", execInstruction.getId(),
				equalTo(localInstructionId));

		// should have stored Completed status
		Instruction completedInstruction = storeCompletedInstructionCaptor.getValue();
		assertThat("Completed instruction has persisted local ID", completedInstruction.getId(),
				equalTo(localInstructionId));
		assertThat("Completed instruction state", completedInstruction.getInstructionState(),
				equalTo(InstructionState.Completed));
		assertThat("Completed instruction no ack state",
				completedInstruction.getStatus().getAcknowledgedInstructionState(), nullValue());

		// should have stored Completed ack status
		Instruction completedInstructionAck = storeCompletedInstructionAckCaptor.getValue();
		assertThat("Completed instruction has persisted local ID", completedInstructionAck.getId(),
				equalTo(localInstructionId));
		assertThat("Completed instruction state", completedInstructionAck.getInstructionState(),
				equalTo(InstructionState.Completed));
		assertThat("Completed instruction ack state",
				completedInstructionAck.getStatus().getAcknowledgedInstructionState(),
				equalTo(InstructionState.Completed));

		// should have published acknowledgement on datum topic
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Published instruction and acks", session.publishMessages, hasSize(3));

		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Instruction client ID", pubMsg.getClientID(), equalTo("solarnet"));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(instructionTopic(nodeId)));

		pubMsg = session.publishMessages.get(1);
		assertThat("Instruction ack client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
		assertThat("Instruction ack payload", session.getPublishPayloadStringAtIndex(1),
				equalTo("{\"__type__\":\"InstructionStatus\",\"id\":\"" + localInstructionId + "\""
						+ ",\"instructionId\":\"" + remoteInstructionId + "\""
						+ ",\"topic\":\"SetControlParameter\",\"status\":\"Executing\"}"));

		pubMsg = session.publishMessages.get(2);
		assertThat("Instruction ack client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
		assertThat("Instruction ack payload", session.getPublishPayloadStringAtIndex(2),
				equalTo("{\"__type__\":\"InstructionStatus\",\"id\":\"" + localInstructionId + "\""
						+ ",\"instructionId\":\"" + remoteInstructionId + "\""
						+ ",\"topic\":\"SetControlParameter\",\"status\":\"Completed\"}"));
	}

	/*- Following test not working after switch to Netty client; haven't found a way
	    to simulate drop of connection in test.
	
	@Test
	public void processInstructionLoseConnectionBeforeAck() throws Exception {
		// given
		TestingMqttMessageHandler messageHandler = new TestingMqttMessageHandler();
		MqttConnection solarNetClient = createMqttClient("solarnet", messageHandler);
	
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 123);
	
		// parse instructions
		String testInstructions = getStringResource("instructions-01.json");
		List<Instruction> instructions = parseInstructions(testInstructions);
		expect(reactorService.parseInstructions(anyObject(), anyObject(JsonNode.class),
				eq(MqttUploadService.JSON_MIME_TYPE), isNull())).andReturn(instructions);
	
		// execute single instruction
		InstructionStatus execResultStatus = new BasicInstructionStatus(null,
				InstructionStatus.InstructionState.Completed, new Date());
		expect(instructionExecutionService.executeInstruction(instructions.get(0)))
				.andReturn(execResultStatus);
	
		TestingInterceptHandler session = getTestingInterceptHandler();
		session.setCallback(new TestingInterceptHandler.Callback() {
	
			int count = 0;
	
			@Override
			public void handleInterceptMessage(InterceptMessage msg) {
				if ( msg instanceof InterceptPublishMessage ) {
					if ( count == 1 ) {
						mqttServer.stopServer();
					}
					count++;
				}
			}
		});
	
		Long localId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		Capture<Instruction> ackCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(ackCaptor))).andReturn(localId);
	
		replayAll();
	
		// when
		Thread.sleep(500); // allow time for subscription to take
	
		String instrTopic = instructionTopic(nodeId);
		try {
			solarNetClient.publish(new BasicMqttMessage(instrTopic, false, MqttQos.AtLeastOnce,
					testInstructions.getBytes("UTF-8")));
		} catch ( Exception e ) {
			log.warn("Exception caught publishing message.", e);
		}
	
		Thread.sleep(5000); // allow time for messages to process
	
		stopMqttServer(); // shut down server
	
		// then
	
		// should have published acknowledgement on datum topic
		assertThat("Published instruction and ack", session.publishMessages, not(hasSize(0)));
	
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Instruction client ID", pubMsg.getClientID(), equalTo("solarnet"));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(instructionTopic(nodeId)));
	
		Instruction instr = instructions.get(0);
		Instruction ackStatus = ackCaptor.getValue();
		assertThat("Instruction status ack local ID", ackStatus.getId(), nullValue());
		assertThat("Instruction status ack topic", ackStatus.getTopic(), equalTo(instr.getTopic()));
		assertThat("Instruction status ack remote ID", ackStatus.getRemoteInstructionId(),
				equalTo(instr.getRemoteInstructionId()));
		assertThat("Instruction status ack date", ackStatus.getInstructionDate(),
				equalTo(instr.getInstructionDate()));
		assertThat("Instruction status ack status", ackStatus.getStatus(), notNullValue());
		assertThat("Instruction status ack status state", ackStatus.getStatus().getInstructionState(),
				equalTo(InstructionStatus.InstructionState.Completed));
		assertThat("Instruction status ack status ack state",
				ackStatus.getStatus().getAcknowledgedInstructionState(), nullValue());
	}
	*/

	@Test
	public void processInstruction_duplicate() throws Exception {
		// GIVEN
		TestingMqttMessageHandler messageHandler = new TestingMqttMessageHandler();
		MqttConnection solarNetClient = createMqttClient("solarnet", messageHandler);

		// parse instructions
		String testInstructions = getStringResource("instructions-01.json");
		List<Instruction> instructions = parseInstructions(testInstructions);
		assert instructions.size() == 1;
		final String remoteInstructionId = instructions.get(0).getRemoteInstructionId();
		expect(reactorService.parseInstructions(anyObject(), anyObject(JsonNode.class),
				eq(MqttUploadService.JSON_MIME_TYPE), isNull())).andReturn(instructions);

		// persist Executing state
		Capture<Instruction> storeInstructionCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(storeInstructionCaptor)))
				.andThrow(new DuplicateKeyException("Duplicate key"));

		replayAll();

		// WHEN
		Thread.sleep(1000); // allow time for subscription to take

		String instrTopic = instructionTopic(nodeId);
		solarNetClient.publish(new BasicMqttMessage(instrTopic, false, MqttQos.AtLeastOnce,
				testInstructions.getBytes("UTF-8"))).get(MQTT_TIMEOUT, TimeUnit.SECONDS);

		Thread.sleep(2000); // allow time for messages to process

		stopMqttServer(); // shut down server

		// THEN

		// should have stored Executing status
		Instruction storeInstruction = storeInstructionCaptor.getValue();
		assertThat("Store instruction remote ID", storeInstruction.getRemoteInstructionId(),
				equalTo(remoteInstructionId));
		assertThat("Store instruction has no local ID", storeInstruction.getId(), nullValue());
		assertThat("Store instruction state", storeInstruction.getInstructionState(),
				equalTo(InstructionState.Executing));
		assertThat("Store instruction instructorId", storeInstruction.getInstructorId(),
				equalTo(TEST_SOLARIN_BASE_URL));

		// should have published acknowledgement on datum topic
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Published instruction and acks", session.publishMessages, hasSize(1));

		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Instruction client ID", pubMsg.getClientID(), equalTo("solarnet"));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(instructionTopic(nodeId)));
	}

	@Test
	public void processInstruction_noHandler() throws Exception {
		// GIVEN
		TestingMqttMessageHandler messageHandler = new TestingMqttMessageHandler();
		MqttConnection solarNetClient = createMqttClient("solarnet", messageHandler);

		// parse instructions
		String testInstructions = getStringResource("instructions-01.json");
		List<Instruction> instructions = parseInstructions(testInstructions);
		assert instructions.size() == 1;
		final String remoteInstructionId = instructions.get(0).getRemoteInstructionId();
		expect(reactorService.parseInstructions(anyObject(), anyObject(JsonNode.class),
				eq(MqttUploadService.JSON_MIME_TYPE), isNull())).andReturn(instructions);

		// persist Executing state
		Capture<Instruction> storeInstructionCaptor = new Capture<>();
		final Long localInstructionId = 98765L;
		expect(reactorService.storeInstruction(capture(storeInstructionCaptor)))
				.andReturn(localInstructionId);

		// execute single instruction
		Capture<Instruction> execInstructionCaptor = new Capture<>();
		expect(instructionExecutionService.executeInstruction(capture(execInstructionCaptor)))
				.andReturn(null);

		// save result back to DB
		Capture<Instruction> storeReceivedInstructionCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(storeReceivedInstructionCaptor)))
				.andReturn(localInstructionId);

		// save ack result back to DB
		Capture<Instruction> storeReceivedAckInstructionCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(storeReceivedAckInstructionCaptor)))
				.andReturn(localInstructionId);

		replayAll();

		// WHEN
		Thread.sleep(1000); // allow time for subscription to take

		String instrTopic = instructionTopic(nodeId);
		solarNetClient.publish(new BasicMqttMessage(instrTopic, false, MqttQos.AtLeastOnce,
				testInstructions.getBytes("UTF-8"))).get(MQTT_TIMEOUT, TimeUnit.SECONDS);

		Thread.sleep(2000); // allow time for messages to process

		stopMqttServer(); // shut down server

		// THEN

		// should have stored Executing status
		Instruction storeInstruction = storeInstructionCaptor.getValue();
		assertThat("Store instruction remote ID", storeInstruction.getRemoteInstructionId(),
				equalTo(remoteInstructionId));
		assertThat("Store instruction has no local ID", storeInstruction.getId(), nullValue());
		assertThat("Store instruction state", storeInstruction.getInstructionState(),
				equalTo(InstructionState.Executing));
		assertThat("Store instruction instructorId", storeInstruction.getInstructorId(),
				equalTo(TEST_SOLARIN_BASE_URL));
		assertThat("Store instruction no ack",
				storeInstruction.getStatus().getAcknowledgedInstructionState(), nullValue());

		// should have executed Instruction with persisted local ID
		Instruction execInstruction = execInstructionCaptor.getValue();
		assertThat("Exec instruction has persisted local ID", execInstruction.getId(),
				equalTo(localInstructionId));

		// should have stored Received status
		Instruction receivedInstruction = storeReceivedInstructionCaptor.getValue();
		assertThat("Received instruction has persisted local ID", receivedInstruction.getId(),
				equalTo(localInstructionId));
		assertThat("Received instruction state", receivedInstruction.getInstructionState(),
				equalTo(InstructionState.Received));
		assertThat("Received instruction no ack",
				receivedInstruction.getStatus().getAcknowledgedInstructionState(), nullValue());

		// should have stored Received status
		Instruction receivedAckInstruction = storeReceivedAckInstructionCaptor.getValue();
		assertThat("Received ack instruction has persisted local ID", receivedAckInstruction.getId(),
				equalTo(localInstructionId));
		assertThat("Received ack instruction state", receivedAckInstruction.getInstructionState(),
				equalTo(InstructionState.Received));
		assertThat("Received ack instruction ack state",
				receivedAckInstruction.getStatus().getAcknowledgedInstructionState(),
				equalTo(InstructionState.Received));

		// should have published acknowledgement on datum topic
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Published instruction and acks", session.publishMessages, hasSize(3));

		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Instruction client ID", pubMsg.getClientID(), equalTo("solarnet"));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(instructionTopic(nodeId)));

		pubMsg = session.publishMessages.get(1);
		assertThat("Instruction ack client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
		assertThat("Instruction ack payload", session.getPublishPayloadStringAtIndex(1),
				equalTo("{\"__type__\":\"InstructionStatus\",\"id\":\"" + localInstructionId + "\""
						+ ",\"instructionId\":\"" + remoteInstructionId + "\""
						+ ",\"topic\":\"SetControlParameter\",\"status\":\"Executing\"}"));

		pubMsg = session.publishMessages.get(2);
		assertThat("Instruction ack client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
		assertThat("Instruction ack payload", session.getPublishPayloadStringAtIndex(2),
				equalTo("{\"__type__\":\"InstructionStatus\",\"id\":\"" + localInstructionId + "\""
						+ ",\"instructionId\":\"" + remoteInstructionId + "\""
						+ ",\"topic\":\"SetControlParameter\",\"status\":\"Received\"}"));
	}
}
