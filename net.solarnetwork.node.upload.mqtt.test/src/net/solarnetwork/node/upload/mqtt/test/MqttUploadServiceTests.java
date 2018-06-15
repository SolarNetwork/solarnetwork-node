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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.reactor.io.json.JsonReactorSerializationService;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.support.DatumSerializer;
import net.solarnetwork.node.support.GeneralNodeDatumSerializer;
import net.solarnetwork.node.support.InstructionSerializer;
import net.solarnetwork.node.support.NodeControlInfoSerializer;
import net.solarnetwork.node.upload.mqtt.MqttUploadService;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link MqttUploadService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttUploadServiceTests extends MqttServerSupport {

	private IdentityService identityService;
	private ReactorService reactorService;
	private InstructionExecutionService instructionExecutionService;
	private EventAdmin eventAdminService;
	private ObjectMapper objectMapper;
	private MqttUploadService service;

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		if ( jsonFactory != null ) {
			factory.setJsonFactory(jsonFactory);
		}
		factory.setSerializers(Arrays.asList(new GeneralNodeDatumSerializer(), new DatumSerializer(),
				new InstructionSerializer(), new NodeControlInfoSerializer()));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setup() {
		setupMqttServer();

		identityService = EasyMock.createMock(IdentityService.class);
		reactorService = EasyMock.createMock(ReactorService.class);
		instructionExecutionService = EasyMock.createMock(InstructionExecutionService.class);
		eventAdminService = EasyMock.createMock(EventAdmin.class);

		objectMapper = createObjectMapper(null);

		service = new MqttUploadService(objectMapper, identityService, null, null,
				new StaticOptionalService<ReactorService>(reactorService),
				new StaticOptionalService<InstructionExecutionService>(instructionExecutionService),
				new StaticOptionalService<EventAdmin>(eventAdminService));
		service.setPersistencePath(System.getProperty("java.io.tmpdir"));
	}

	@Override
	@After
	public void teardown() {
		super.teardown();
		EasyMock.verify(identityService, reactorService, instructionExecutionService, eventAdminService);
	}

	private void replayAll() {
		EasyMock.replay(identityService, reactorService, instructionExecutionService, eventAdminService);
	}

	private String expectClientStartup() {
		String serverUri = "mqtt://localhost:" + getMqttServerPort();
		expect(identityService.getSolarInMqttUrl()).andReturn(serverUri).atLeastOnce();
		return serverUri;
	}

	@Test
	public void serviceKey() {
		// given
		String serverUri = expectClientStartup();

		replayAll();

		// when
		String key = ((UploadService) service).getKey();

		// then
		assertThat("UploadService key", key, equalTo("MqttUploadService:" + serverUri));
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
		datum.putInstantaneousSampleValue("foo", 123);

		expectClientStartup();

		Long nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		expect(identityService.getNodeId()).andReturn(nodeId).atLeastOnce();

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// when
		service.init();
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// then
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(false));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
		assertThat("Publish payload", session.getPublishPayloadStringAtIndex(0),
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
		datum.putInstantaneousSampleValue("foo", 123);

		expectClientStartup();

		Long nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		expect(identityService.getNodeId()).andReturn(nodeId).atLeastOnce();

		Capture<Event> eventCaptor = new Capture<Event>();
		eventAdminService.postEvent(capture(eventCaptor));

		replayAll();

		// when
		service.init();
		String txId = service.uploadDatum(datum);

		stopMqttServer(); // to flush messages

		// then
		assertThat("TX ID", txId, notNullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(false));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Publish client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
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
	public void uploadWithoutConnectionToMqttServer() throws IOException {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 123);

		expectClientStartup();

		Long nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		expect(identityService.getNodeId()).andReturn(nodeId).atLeastOnce();

		replayAll();

		stopMqttServer(); // shut down server

		// when
		service.init();
		String txId = service.uploadDatum(datum);

		// then
		assertThat("TX ID", txId, nullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(0));
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
			return new JsonReactorSerializationService().decodeInstructions(null, instructions,
					MqttUploadService.JSON_MIME_TYPE, null);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void processInstruction() throws Exception {
		// given
		Long nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		TestingMqttCallback mqttCallback = new TestingMqttCallback();
		setupMqttClient("solarnet", mqttCallback);

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 123);

		expectClientStartup();

		expect(identityService.getNodeId()).andReturn(nodeId).atLeastOnce();

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

		replayAll();

		// when
		service.init();

		String instrTopic = instructionTopic(nodeId);
		mqttClient.publish(instrTopic, testInstructions.getBytes("UTF-8"), 1, false);

		Thread.sleep(2000); // allow time for messages to process

		stopMqttServer(); // shut down server

		// then

		// should have published acknowledgement on datum topic
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Published instruction and ack", session.publishMessages, hasSize(2));

		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Instruction client ID", pubMsg.getClientID(), equalTo("solarnet"));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(instructionTopic(nodeId)));

		pubMsg = session.publishMessages.get(1);
		assertThat("Instruction ack client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Instruction topic", pubMsg.getTopicName(), equalTo(datumTopic(nodeId)));
		assertThat("Instruction ack payload", session.getPublishPayloadStringAtIndex(1),
				equalTo("{\"__type__\":\"InstructionStatus\",\"instructionId\":\"4316548\""
						+ ",\"topic\":\"SetControlParameter\",\"status\":\"Completed\"}"));
	}

	@Test
	public void processInstructionLoseConnectionBeforeAck() throws Exception {
		// given
		Long nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		TestingMqttCallback mqttCallback = new TestingMqttCallback();
		setupMqttClient("solarnet", mqttCallback);

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 123);

		expectClientStartup();

		expect(identityService.getNodeId()).andReturn(nodeId).atLeastOnce();

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

			@Override
			public void handleInterceptMessage(InterceptMessage msg) {
				if ( msg instanceof InterceptPublishMessage ) {
					mqttServer.stopServer();
				}
			}
		});

		Long localId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		Capture<Instruction> ackCaptor = new Capture<>();
		expect(reactorService.storeInstruction(capture(ackCaptor))).andReturn(localId);

		replayAll();

		// when
		service.init();

		String instrTopic = instructionTopic(nodeId);
		mqttClient.publish(instrTopic, testInstructions.getBytes("UTF-8"), 1, false);

		Thread.sleep(2000); // allow time for messages to process

		stopMqttServer(); // shut down server

		// then

		// should have published acknoledgement on datum topic
		assertThat("Published instruction and ack", session.publishMessages, hasSize(1));

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

}
