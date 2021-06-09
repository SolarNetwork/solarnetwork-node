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
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.solarnetwork.common.mqtt.MqttConnectReturnCode.Accepted;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
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
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.io.ObjectEncoder;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.node.support.DatumEvents;
import net.solarnetwork.node.upload.flux.FluxFilterConfig;
import net.solarnetwork.node.upload.flux.FluxUploadService;
import net.solarnetwork.util.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link FluxUploadService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class FluxUploadServiceTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private IdentityService identityService;
	private ObjectMapper objectMapper;
	private MqttConnectionFactory connectionFactory;
	private MqttConnection connection;
	private ObjectEncoder encoder;
	private OperationalModesService operationalModeService;
	private Long nodeId;
	private FluxUploadService service;

	@Before
	public void setup() {
		connectionFactory = EasyMock.createMock(MqttConnectionFactory.class);
		connection = EasyMock.createMock(MqttConnection.class);
		identityService = EasyMock.createMock(IdentityService.class);
		encoder = EasyMock.createMock(ObjectEncoder.class);
		operationalModeService = EasyMock.createMock(OperationalModesService.class);
		objectMapper = new ObjectMapper();

		nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());

		this.service = new FluxUploadService(connectionFactory, objectMapper, identityService);
		this.service.setIncludeVersionTag(false);
		service.setOpModesService(operationalModeService);
	}

	private void replayAll() {
		EasyMock.replay(connectionFactory, connection, identityService, encoder, operationalModeService);
	}

	@After
	public void teardown() {
		EasyMock.verify(connectionFactory, connection, identityService, encoder, operationalModeService);
	}

	private void expectMqttConnectionSetup() throws IOException {
		expect(identityService.getNodeId()).andReturn(nodeId).anyTimes();
		expect(connectionFactory.createConnection(anyObject())).andReturn(connection);
		expect(connection.open()).andReturn(completedFuture(Accepted));
		expect(connection.isEstablished()).andReturn(true).anyTimes();
	}

	private void postEvent(Map<String, Object> datumProps) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		for ( Map.Entry<String, ?> me : datumProps.entrySet() ) {
			String k = me.getKey();
			Object v = me.getValue();
			if ( v == null ) {
				continue;
			}
			if ( Datum.SOURCE_ID.equals(k) ) {
				datum.setSourceId(v.toString());
			} else if ( v instanceof Number ) {
				datum.putInstantaneousSampleValue(k, (Number) v);
			} else {
				datum.putStatusSampleValue(k, v);
			}

		}
		Event event = DatumEvents.datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
		service.handleEvent(event);
	}

	private void assertMessageMetadata(MqttMessage publishedMsg, String sourceId) {
		assertThat("MQTT message published", publishedMsg, notNullValue());
		assertThat("MQTT message topic", publishedMsg.getTopic(),
				equalTo(format("node/%d/datum/0/%s", nodeId, sourceId)));
		assertThat("MQTT message QOS", publishedMsg.getQosLevel(), equalTo(MqttQos.AtMostOnce));
		assertThat("MQTT message retained", publishedMsg.isRetained(), equalTo(true));
	}

	private void assertMessagePayloadJson(MqttMessage publishedMsg, Map<String, Object> datum) {
		Map<String, Object> publishedMsgBody = JsonUtils
				.getStringMap(new String(publishedMsg.getPayload(), Charset.forName("UTF-8")));
		assertThat("Published data keys", publishedMsgBody.keySet(), equalTo(datum.keySet()));
		for ( Map.Entry<String, ?> me : datum.entrySet() ) {
			if ( me.getValue() == null ) {
				// ignore null values
				continue;
			}
			assertThat("Published data prop " + me.getKey(), publishedMsgBody,
					hasEntry(me.getKey(), me.getValue()));
		}
	}

	private void assertMessage(MqttMessage publishedMsg, String sourceId, Map<String, Object> datum) {
		assertMessageMetadata(publishedMsg, sourceId);
		assertMessagePayloadJson(publishedMsg, datum);
	}

	private Map<String, Object> publishLoop(long length, Map<String, Object> datum) throws Exception {
		final long end = System.currentTimeMillis() + length;
		Map<String, Object> result = null;
		while ( System.currentTimeMillis() < end ) {
			postEvent(datum);
			if ( result == null ) {
				result = new LinkedHashMap<>(datum);
				result.put(Datum.TIMESTAMP, null);
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
		datum.put(Datum.TIMESTAMP, null);
		assertMessage(publishedMsg, TEST_SOURCE_ID, datum);
	}

	@Test
	public void postDatum_withVersionTag() throws Exception {
		// GIVEN
		service.setIncludeVersionTag(true);
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
		datum.put(FluxUploadService.TAG_VERSION, 2);
		datum.put(Datum.TIMESTAMP, null);
		assertMessage(publishedMsg, TEST_SOURCE_ID, datum);
	}

	@Test
	public void postDatum_globalExcludeProps() throws Exception {
		// GIVEN
		service.setExcludePropertyNamesRegex("w.*");

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
		filteredDatum.put("_DatumType", "net.solarnetwork.node.domain.Datum");
		// @formatter:off
		filteredDatum.put("_DatumTypes", asList(
				"net.solarnetwork.node.domain.Datum",
				"net.solarnetwork.domain.datum.Datum",
				"net.solarnetwork.node.domain.GeneralDatum",
				"net.solarnetwork.domain.datum.GeneralDatum"));
		// @formatter:on
		filteredDatum.remove("watts");
		filteredDatum.remove("wattHours");
		filteredDatum.put(Datum.TIMESTAMP, null);
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);
	}

	@Test
	public void postDatum_globalExcludeProps_underscoreOrSourceId() throws Exception {
		// GIVEN
		service.setExcludePropertyNamesRegex("(_.*|sourceId)");

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
		postEvent(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> filteredDatum = new LinkedHashMap<>(datum);
		filteredDatum.put(Datum.TIMESTAMP, null);
		filteredDatum.remove(Datum.SOURCE_ID);
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);
	}

	@Test
	public void postDatum_excludeAllProps_anySource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropExcludeValues(new String[] { ".*" });
		filter.configurationChanged(null);
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
		filter.configurationChanged(null);
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
		datum.put(Datum.TIMESTAMP, null);
		assertMessage(publishedMsg, "not.filtered", datum);
	}

	@Test
	public void postDatum_includeProps() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropIncludeValues(new String[] { "^watt" });
		filter.configurationChanged(null);
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
	public void postDatum_excludeProps() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropExcludeValues(new String[] { "^watt" });
		filter.configurationChanged(null);
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
		filteredDatum.remove("watts");
		filteredDatum.remove("wattHours");
		filteredDatum.put(Datum.TIMESTAMP, null);
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);
	}

	@Test
	public void postDatum_includeAndExcludeProps() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropIncludeValues(new String[] { "^(created|sourceId)$", "^watt" });
		filter.setPropExcludeValues(new String[] { "^wattHours" });
		filter.configurationChanged(null);
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
		filteredDatum.remove("foo");
		filteredDatum.remove("wattHours");
		filteredDatum.put(Datum.TIMESTAMP, null);
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);
	}

	@Test
	public void postDatum_throttle_anySource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setFrequencySeconds(1);
		filter.configurationChanged(null);
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
		filter.configurationChanged(null);
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

	@Test
	public void postDatum_throttle_anySource_requireMode_notActive() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setFrequencySeconds(1);
		filter.configurationChanged(null);
		filter.setRequiredOperationalMode("test");
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		expect(operationalModeService.isOperationalModeActive("test")).andReturn(false).anyTimes();

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

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("All MQTT messages published because throttle filter required op mode not active",
				publishedMsgs, hasSize(5));
	}

	@Test
	public void postDatum_throttle_anySource_requireMode_active() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setFrequencySeconds(1);
		filter.configurationChanged(null);
		filter.setRequiredOperationalMode("test");
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		expect(operationalModeService.isOperationalModeActive("test")).andReturn(true).anyTimes();

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

	private static final class TestInjectorTransformService extends BaseIdentifiable
			implements GeneralDatumSamplesTransformService {

		private final GeneralDatumSamples staticSamples;

		public TestInjectorTransformService(String uid, GeneralDatumSamples staticSamples) {
			super();
			setUid(uid);
			this.staticSamples = staticSamples;
		}

		@Override
		public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
				Map<String, Object> parameters) {
			GeneralDatumSamples result = new GeneralDatumSamples(samples);
			if ( staticSamples.getInstantaneous() != null ) {
				for ( Map.Entry<String, Number> me : staticSamples.getInstantaneous().entrySet() ) {
					result.putInstantaneousSampleValue(me.getKey(), me.getValue());
				}
			}
			if ( staticSamples.getAccumulating() != null ) {
				for ( Map.Entry<String, Number> me : staticSamples.getAccumulating().entrySet() ) {
					result.putAccumulatingSampleValue(me.getKey(), me.getValue());
				}
			}
			if ( staticSamples.getStatus() != null ) {
				for ( Map.Entry<String, Object> me : staticSamples.getStatus().entrySet() ) {
					result.putStatusSampleValue(me.getKey(), me.getValue());
				}
			}
			if ( staticSamples.getTags() != null ) {
				for ( String t : staticSamples.getTags() ) {
					result.addTag(t);
				}
			}
			return result;
		}

	}

	@Test
	public void postDatum_filters_requireMode_togglePair() throws Exception {
		FluxFilterConfig f1 = new FluxFilterConfig();
		f1.setRequiredOperationalMode("test");
		f1.setTransformServiceUid("test1.filter");
		FluxFilterConfig f2 = new FluxFilterConfig();
		f2.setRequiredOperationalMode("!test");
		f2.setTransformServiceUid("test2.filter");
		service.setFilters(new FluxFilterConfig[] { f1, f2 });

		GeneralDatumSamples s1 = new GeneralDatumSamples();
		s1.putInstantaneousSampleValue("foo", 1);
		TestInjectorTransformService x1 = new TestInjectorTransformService("test1.filter", s1);

		GeneralDatumSamples s2 = new GeneralDatumSamples();
		s2.putInstantaneousSampleValue("bar", 1);
		TestInjectorTransformService x2 = new TestInjectorTransformService("test2.filter", s2);

		service.setTransformServices(new StaticOptionalServiceCollection<>(asList(x1, x2)));

		expectMqttConnectionSetup();

		expect(operationalModeService.isOperationalModeActive("test")).andReturn(true);
		expect(operationalModeService.isOperationalModeActive("!test")).andReturn(false);

		Capture<MqttMessage> msgCaptor = new Capture<>();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, "s1");
		datum.put("watts", 1234);

		postEvent(datum);

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("1 MQTT messages published", publishedMsgs, hasSize(1));

		Map<String, Object> filteredDatum = new LinkedHashMap<>(datum);
		filteredDatum.put(Datum.TIMESTAMP, null);
		filteredDatum.put("foo", 1);
		assertMessage(publishedMsgs.get(0), "s1", filteredDatum);
	}

	@Test
	public void postDatum_filters_requireMode_togglePair_inverted() throws Exception {
		FluxFilterConfig f1 = new FluxFilterConfig();
		f1.setRequiredOperationalMode("test");
		f1.setTransformServiceUid("test1.filter");
		FluxFilterConfig f2 = new FluxFilterConfig();
		f2.setRequiredOperationalMode("!test");
		f2.setTransformServiceUid("test2.filter");
		service.setFilters(new FluxFilterConfig[] { f1, f2 });

		GeneralDatumSamples s1 = new GeneralDatumSamples();
		s1.putInstantaneousSampleValue("foo", 1);
		TestInjectorTransformService x1 = new TestInjectorTransformService("test1.filter", s1);

		GeneralDatumSamples s2 = new GeneralDatumSamples();
		s2.putInstantaneousSampleValue("bar", 1);
		TestInjectorTransformService x2 = new TestInjectorTransformService("test2.filter", s2);

		service.setTransformServices(new StaticOptionalServiceCollection<>(asList(x1, x2)));

		expectMqttConnectionSetup();

		expect(operationalModeService.isOperationalModeActive("test")).andReturn(false);
		expect(operationalModeService.isOperationalModeActive("!test")).andReturn(true);

		Capture<MqttMessage> msgCaptor = new Capture<>();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		Map<String, Object> datum = new HashMap<>(4);
		datum.put(Datum.SOURCE_ID, "s1");
		datum.put("watts", 1234);

		postEvent(datum);

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("1 MQTT messages published", publishedMsgs, hasSize(1));

		Map<String, Object> filteredDatum = new LinkedHashMap<>(datum);
		filteredDatum.put(Datum.TIMESTAMP, null);
		filteredDatum.put("bar", 1);
		assertMessage(publishedMsgs.get(0), "s1", filteredDatum);
	}

	@Test
	public void postDatum_withEncoder_anySource() throws Exception {
		// GIVEN
		service.setDatumEncoders(new StaticOptionalServiceCollection<>(singleton(encoder)));

		final String encoderUid = "test.encoder";

		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setDatumEncoderUid(encoderUid);
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		expect(encoder.getUid()).andReturn(encoderUid);

		final byte[] encodedBytes = "encoded".getBytes();
		Capture<Object> encoderObjectCaptor = new Capture<>();
		expect(encoder.encodeAsBytes(capture(encoderObjectCaptor), isNull())).andReturn(encodedBytes);

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

		assertMessageMetadata(publishedMsg, TEST_SOURCE_ID);
		assertThat("Message encoded via encoder", publishedMsg.getPayload(), sameInstance(encodedBytes));
	}

	@Test
	public void postDatum_withEncoder_matchingSource() throws Exception {
		// GIVEN
		service.setDatumEncoders(new StaticOptionalServiceCollection<>(singleton(encoder)));

		final String encoderUid = "test.encoder";

		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setSourceIdRegexValue("^test");
		filter.setDatumEncoderUid(encoderUid);
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		expect(encoder.getUid()).andReturn(encoderUid);

		final byte[] encodedBytes = "encoded".getBytes();
		Capture<Object> encoderObjectCaptor = new Capture<>();
		expect(encoder.encodeAsBytes(capture(encoderObjectCaptor), isNull())).andReturn(encodedBytes);

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

		assertMessageMetadata(publishedMsg, TEST_SOURCE_ID);
		assertThat("Message encoded via encoder", publishedMsg.getPayload(), sameInstance(encodedBytes));
	}

	@Test
	public void postDatum_withEncoder_matchingSource_multiEncoders() throws Exception {
		// GIVEN
		service.setDatumEncoders(new StaticOptionalServiceCollection<>(singleton(encoder)));

		final String encoderUid = "test.encoder";

		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setSourceIdRegexValue("^not.test");
		filter.setDatumEncoderUid("not.test.encoder");
		filter.configurationChanged(null);

		FluxFilterConfig filter2 = new FluxFilterConfig();
		filter2.setSourceIdRegexValue("^test");
		filter2.setDatumEncoderUid(encoderUid);
		filter2.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter, filter2 });

		expectMqttConnectionSetup();

		expect(encoder.getUid()).andReturn(encoderUid);

		final byte[] encodedBytes = "encoded".getBytes();
		Capture<Object> encoderObjectCaptor = new Capture<>();
		expect(encoder.encodeAsBytes(capture(encoderObjectCaptor), isNull())).andReturn(encodedBytes);

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

		assertMessageMetadata(publishedMsg, TEST_SOURCE_ID);
		assertThat("Message encoded via encoder", publishedMsg.getPayload(), sameInstance(encodedBytes));
	}

	@Test
	public void postDatum_withEncoder_noMatchingSource_filtersStillApplied() throws Exception {
		// GIVEN
		service.setDatumEncoders(new StaticOptionalServiceCollection<>(singleton(encoder)));

		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setDatumEncoderUid("not.test.encoder");
		filter.setPropExcludeValues(new String[] { "^(created|wattHours)$" });
		filter.configurationChanged(null);

		FluxFilterConfig filter2 = new FluxFilterConfig();
		filter2.setDatumEncoderUid("no.no.encoder");
		filter2.setPropExcludeValues(new String[] { "^watts" });
		filter2.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter, filter2 });

		expectMqttConnectionSetup();

		expect(encoder.getUid()).andReturn("foo").anyTimes();

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
		filteredDatum.remove("watts");
		filteredDatum.remove("wattHours");
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);
	}

}
