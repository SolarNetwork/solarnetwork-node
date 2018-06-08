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

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.ReactorService;
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

		objectMapper = createObjectMapper(null);

		service = new MqttUploadService(objectMapper, identityService, null,
				new StaticOptionalService<ReactorService>(reactorService),
				new StaticOptionalService<InstructionExecutionService>(instructionExecutionService));
		service.setPersistencePath(System.getProperty("java.io.tmpdir"));
	}

	@Override
	@After
	public void teardown() {
		EasyMock.verify(identityService, reactorService, instructionExecutionService);
	}

	private void replayAll() {
		EasyMock.replay(identityService, reactorService, instructionExecutionService);
	}

	private String expectClientStartup() {
		String serverUri = "mqtt://localhost:" + getMqttServerPort();
		expect(identityService.getSolarInMqttUrl()).andReturn(serverUri);
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

	private String dataPubTopic(Long nodeId) {
		return "node/" + nodeId + "/data";
	}

	@Test
	public void uploadWithConnectionToMqttServer() throws IOException {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 123);

		expectClientStartup();

		Long nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		expect(identityService.getNodeId()).andReturn(nodeId).atLeastOnce();

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
		assertThat("Durable session", connMsg.isCleanSession(), equalTo(false));

		assertThat("Published datum", session.publishMessages, hasSize(1));
		InterceptPublishMessage pubMsg = session.publishMessages.get(0);
		assertThat("Public client ID", pubMsg.getClientID(), equalTo(nodeId.toString()));
		assertThat("Publish topic", pubMsg.getTopicName(), equalTo(dataPubTopic(nodeId)));
		assertThat("Publish payload", session.getPublishPayloadStringAtIndex(0),
				equalTo(objectMapper.writeValueAsString(datum)));
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
		String txId = service.uploadDatum(datum);

		// then
		assertThat("TX ID", txId, nullValue());

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(0));
		assertThat("Published datum", session.publishMessages, hasSize(0));
	}

}
