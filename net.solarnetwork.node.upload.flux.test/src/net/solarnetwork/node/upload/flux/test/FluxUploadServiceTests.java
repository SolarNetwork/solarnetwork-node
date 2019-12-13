/* ==================================================================
 * FluxUploadServiceTests.java - 13/12/2019 1:37:22 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.flux.test;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.solarnetwork.common.mqtt.MqttConnectReturnCode.Accepted;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.upload.flux.FluxFilterConfig;
import net.solarnetwork.node.upload.flux.FluxUploadService;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link FluxUploadService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FluxUploadServiceTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private IdentityService identityService;
	private ObjectMapper objectMapper;
	private MqttConnectionFactory connectionFactory;
	private MqttConnection connection;
	private Long nodeId;
	private FluxUploadService service;

	@Before
	public void setup() {
		connectionFactory = EasyMock.createMock(MqttConnectionFactory.class);
		connection = EasyMock.createMock(MqttConnection.class);
		identityService = EasyMock.createMock(IdentityService.class);
		objectMapper = new ObjectMapper();

		nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());

		this.service = new FluxUploadService(connectionFactory, objectMapper, identityService);
	}

	private void replayAll() {
		EasyMock.replay(connectionFactory, connection, identityService);
	}

	@After
	public void teardown() {
		EasyMock.verify(connectionFactory, connection, identityService);
	}

	private void expectMqttConnectionSetup() throws IOException {
		expect(identityService.getNodeId()).andReturn(nodeId).anyTimes();
		expect(connectionFactory.createConnection(anyObject())).andReturn(connection);
		expect(connection.open()).andReturn(completedFuture(Accepted));
		expect(connection.isEstablished()).andReturn(true).anyTimes();
	}

	private void postEvent(Map<String, Object> datum) {
		datum.put(Datum.TIMESTAMP, System.currentTimeMillis());
		Event event = new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
		service.handleEvent(event);
	}

	private void assertMessage(MqttMessage publishedMsg, String sourceId, Map<String, Object> datum) {
		assertThat("MQTT message published", publishedMsg, notNullValue());
		assertThat("MQTT message topic", publishedMsg.getTopic(),
				equalTo(format("node/%d/datum/0/%s", nodeId, sourceId)));
		assertThat("MQTT message QOS", publishedMsg.getQosLevel(), equalTo(MqttQos.AtMostOnce));
		assertThat("MQTT message retained", publishedMsg.isRetained(), equalTo(true));

		Map<String, Object> publishedMsgBody = JsonUtils
				.getStringMap(new String(publishedMsg.getPayload(), Charset.forName("UTF-8")));
		assertThat("Published data as map", publishedMsgBody, equalTo(datum));
	}

	private Map<String, Object> publishLoop(long length, Map<String, Object> datum) throws Exception {
		final long end = System.currentTimeMillis() + length;
		Map<String, Object> result = null;
		while ( System.currentTimeMillis() < end ) {
			postEvent(datum);
			if ( result == null ) {
				result = new LinkedHashMap<>(datum);
			}
			Thread.sleep(200);
		}
		return result;
	}

	@Test
	public void postDatum() throws Exception {
		// GIVEN
		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = new Capture<>();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, TEST_SOURCE_ID);
		datum.put("watts", 1234);
		postEvent(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		assertMessage(publishedMsg, TEST_SOURCE_ID, datum);
	}

	@Test
	public void postDatum_excludeAllProps_anySource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropExcludeValues(new String[] { ".*" });
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		// WHEN
		replayAll();
		service.init();

		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, TEST_SOURCE_ID);
		datum.put("watts", 1234);
		postEvent(datum);

		// THEN
		// nothing
	}

	@Test
	public void postDatum_excludeAllProps_specificSource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setSourceIdRegexValue("^test");
		filter.setPropExcludeValues(new String[] { ".*" });
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = new Capture<>();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, TEST_SOURCE_ID);
		datum.put("watts", 1234);
		postEvent(datum);

		datum.put(Datum.SOURCE_ID, "not.filtered");
		postEvent(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		assertMessage(publishedMsg, "not.filtered", datum);
	}

	@Test
	public void postDatum_includeProps() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropIncludeValues(new String[] { "^watt" });
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = new Capture<>();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, TEST_SOURCE_ID);
		datum.put("watts", 1234);
		datum.put("wattHours", 2345);
		datum.put("foo", 3456);
		postEvent(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> filteredDatum = new LinkedHashMap<>(datum);
		filteredDatum.remove("created");
		filteredDatum.remove("foo");
		filteredDatum.remove("sourceId");
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);
	}

	@Test
	public void postDatum_throttle_anySource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setFrequencySeconds(1);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = new Capture<>(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).anyTimes();

		// WHEN
		replayAll();
		service.init();

		List<Map<String, Object>> publishedDatum = new ArrayList<Map<String, Object>>(2);
		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, "s1");
		datum.put("watts", 1234);

		// post a bunch of events within the throttle window; only one should be captured
		publishedDatum.add(publishLoop(1000, datum));

		Thread.sleep(200);

		publishedDatum.add(publishLoop(1000, datum));

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("Only 2 MQTT messages published because of throttle filter", publishedMsgs,
				hasSize(2));
		assertMessage(publishedMsgs.get(0), "s1", publishedDatum.get(0));
		assertMessage(publishedMsgs.get(1), "s1", publishedDatum.get(1));
	}

	@Test
	public void postDatum_throttle_specificSource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setSourceIdRegexValue("^test");
		filter.setFrequencySeconds(1);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = new Capture<>(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).anyTimes();

		// WHEN
		replayAll();
		service.init();

		List<Map<String, Object>> publishedDatum = new ArrayList<Map<String, Object>>(2);
		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, TEST_SOURCE_ID);
		datum.put("watts", 1234);

		// post a bunch of events within the throttle window; only one should be captured
		publishedDatum.add(publishLoop(1000, datum));

		Thread.sleep(200);

		publishedDatum.add(publishLoop(1000, datum));

		// switch source ID to non-matching
		datum.put(Datum.SOURCE_ID, "not.throttled.source");
		publishLoop(1000, datum);

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("More than 2 MQTT messages published because of throttle filter",
				publishedMsgs.size(), greaterThan(2));

		MqttMessage publishedMsg = publishedMsgs.get(0);
		assertMessage(publishedMsg, TEST_SOURCE_ID, publishedDatum.get(0));

		publishedMsg = publishedMsgs.get(1);
		assertMessage(publishedMsg, TEST_SOURCE_ID, publishedDatum.get(1));

		// remaining message should be other source
		for ( int i = 2; i < publishedMsgs.size(); i++ ) {
			publishedMsg = publishedMsgs.get(i);
			assertThat("MQTT message topic", publishedMsg.getTopic(),
					equalTo(format("node/%d/datum/0/%s", nodeId, "not.throttled.source")));
		}
	}
}
