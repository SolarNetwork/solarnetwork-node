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
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.codec.ObjectEncoder;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.dao.BasicMqttMessageEntity;
import net.solarnetwork.common.mqtt.dao.MqttMessageDao;
import net.solarnetwork.common.mqtt.dao.MqttMessageEntity;
import net.solarnetwork.dao.BasicBatchResult;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.upload.flux.FluxFilterConfig;
import net.solarnetwork.node.upload.flux.FluxUploadService;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;
import net.solarnetwork.util.Half;
import net.solarnetwork.util.NumberUtils;

/**
 * Test cases for the {@link FluxUploadService} class.
 * 
 * @author matt
 * @version 2.0
 */
public class FluxUploadServiceTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private IdentityService identityService;
	private ObjectMapper objectMapper;
	private MqttConnectionFactory connectionFactory;
	private MqttConnection connection;
	private MqttMessageDao messageDao;
	private ObjectEncoder encoder;
	private OperationalModesService operationalModeService;
	private DatumFilterService xformService;
	private DatumQueue datumQueue;
	private Long nodeId;
	private FluxUploadService service;

	@Before
	public void setup() {
		connectionFactory = EasyMock.createMock(MqttConnectionFactory.class);
		connection = EasyMock.createMock(MqttConnection.class);
		messageDao = EasyMock.createMock(MqttMessageDao.class);
		identityService = EasyMock.createMock(IdentityService.class);
		encoder = EasyMock.createMock(ObjectEncoder.class);
		operationalModeService = EasyMock.createMock(OperationalModesService.class);
		objectMapper = new ObjectMapper();
		xformService = EasyMock.createMock(DatumFilterService.class);
		datumQueue = EasyMock.createMock(DatumQueue.class);

		nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());

		this.service = new FluxUploadService(connectionFactory, objectMapper, identityService,
				datumQueue);
		this.service.setIncludeVersionTag(false);
		service.setOpModesService(operationalModeService);
		service.setTransformServices(new StaticOptionalServiceCollection<>(singleton(xformService)));
	}

	private void replayAll() {
		EasyMock.replay(connectionFactory, connection, identityService, encoder, operationalModeService,
				messageDao, xformService, datumQueue);
	}

	@After
	public void teardown() {
		EasyMock.verify(connectionFactory, connection, identityService, encoder, operationalModeService,
				messageDao, xformService, datumQueue);
	}

	private void expectMqttConnectionSetup() throws IOException {
		datumQueue.addConsumer(service);
		expect(identityService.getNodeId()).andReturn(nodeId).anyTimes();
		expect(connectionFactory.createConnection(anyObject())).andReturn(connection);
		expect(connection.open()).andReturn(completedFuture(Accepted));
		connection.setConnectionObserver(service);
		expectLastCall().anyTimes();
		expect(connection.isEstablished()).andReturn(true).anyTimes();
	}

	private void assertMessageMetadata(MqttMessage publishedMsg, String sourceId) {
		assertThat("MQTT message published", publishedMsg, notNullValue());
		assertThat("MQTT message topic", publishedMsg.getTopic(),
				equalTo(format("node/%d/datum/0/%s", nodeId, sourceId)));
		assertThat("MQTT message QOS", publishedMsg.getQosLevel(), equalTo(MqttQos.AtMostOnce));
		assertThat("MQTT message retained", publishedMsg.isRetained(),
				equalTo(service.isPublishRetained()));
	}

	private void assertMessagePayloadJson(MqttMessage publishedMsg, Map<String, ?> datum) {
		Map<String, Object> publishedMsgBody = JsonUtils
				.getStringMap(new String(publishedMsg.getPayload(), Charset.forName("UTF-8")));
		assertThat("Published data keys", publishedMsgBody.keySet(), equalTo(datum.keySet()));
		for ( Map.Entry<String, ?> me : datum.entrySet() ) {
			if ( me.getValue() == null ) {
				// ignore null values
				continue;
			}
			Object v = me.getValue();
			if ( v.getClass().isArray() ) {
				// JSON array converted to List in message
				v = Arrays.asList((Object[]) v);
			}
			assertThat("Published data prop " + me.getKey(), publishedMsgBody, hasEntry(me.getKey(), v));
		}
	}

	private void assertMessage(MqttMessage publishedMsg, String sourceId, Map<String, ?> datum) {
		assertMessageMetadata(publishedMsg, sourceId);
		assertMessagePayloadJson(publishedMsg, datum);
	}

	private NodeDatum publishLoop(long length, NodeDatum datum) throws Exception {
		final long end = System.currentTimeMillis() + length;
		NodeDatum result = null;
		while ( System.currentTimeMillis() < end ) {
			datum = datum.copyWithId(new DatumId(datum.getKind(), datum.getObjectId(),
					datum.getSourceId(), Instant.now()));
			service.accept(datum);
			if ( result == null ) {
				result = datum;
			}
			Thread.sleep(200);
		}
		return result;
	}

	@Test
	public void connEstablished_mqttPersistence_none() throws Exception {
		// GIVEN
		service.setMqttMessageDao(new StaticOptionalService<>(messageDao));
		expectMqttConnectionSetup();

		Capture<BatchableDao.BatchCallback<MqttMessageEntity>> callbackCaptor = Capture.newInstance();
		Capture<BatchableDao.BatchOptions> optionsCaptor = Capture.newInstance();
		expect(messageDao.batchProcess(capture(callbackCaptor), capture(optionsCaptor)))
				.andReturn(new BasicBatchResult(0));

		// WHEN
		replayAll();
		service.init();
		service.onMqttServerConnectionEstablished(connection, false);

		// THEN
		assertThat("Callback provided", callbackCaptor.getValue(), is(notNullValue()));
		BatchableDao.BatchOptions options = optionsCaptor.getValue();
		assertThat("Options provided", options, is(notNullValue()));
		assertThat("Options is updatable", options.isUpdatable(), is(equalTo(true)));
		assertThat("Options has dest parameter", options.getParameters(), hasEntry(
				MqttMessageDao.BATCH_OPTION_DESTINATION, service.getMqttConfig().getServerUriValue()));
	}

	@Test
	public void connEstablished_mqttPersistence_lessThanMax() throws Exception {
		// GIVEN
		service.setMqttMessageDao(new StaticOptionalService<>(messageDao));
		expectMqttConnectionSetup();

		BasicMqttMessageEntity entity = new BasicMqttMessageEntity(1L, Instant.now(),
				service.getMqttConfig().getServerUriValue(), "some/topic", false, MqttQos.ExactlyOnce,
				"Hello, world".getBytes());

		Capture<BatchableDao.BatchCallback<MqttMessageEntity>> callbackCaptor = Capture
				.newInstance(CaptureType.ALL);
		Capture<BatchableDao.BatchOptions> optionsCaptor = Capture.newInstance(CaptureType.ALL);
		expect(messageDao.batchProcess(capture(callbackCaptor), capture(optionsCaptor)))
				.andAnswer(new IAnswer<BatchableDao.BatchResult>() {

					@Override
					public BatchableDao.BatchResult answer() throws Throwable {
						int callCount = callbackCaptor.getValues().size();
						if ( callCount == 1 ) {
							callbackCaptor.getValues().get(callCount - 1).handle(entity);
						}
						return new BasicBatchResult(callCount == 1 ? 1 : 0);
					}
				}).times(2);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();
		service.onMqttServerConnectionEstablished(connection, false);

		// THEN
		MqttMessage pubMessage = msgCaptor.getValue();
		assertThat("Cached message published", pubMessage, is(notNullValue()));
		assertThat("Published cached message topic", pubMessage.getTopic(),
				is(equalTo(entity.getTopic())));
		assertThat("Published cached message QoS", pubMessage.getQosLevel(),
				is(equalTo(entity.getQosLevel())));
		assertThat("Published cached message payload",
				Arrays.equals(pubMessage.getPayload(), entity.getPayload()), is(equalTo(true)));
	}

	@Test
	public void connEstablished_mqttPersistence_max() throws Exception {
		// GIVEN
		service.setMqttMessageDao(new StaticOptionalService<>(messageDao));
		service.setCachedMessagePublishMaximum(2);
		expectMqttConnectionSetup();

		List<MqttMessageEntity> entities = new ArrayList<>();
		for ( int i = 0; i < 3; i++ ) {
			BasicMqttMessageEntity entity = new BasicMqttMessageEntity((long) i, Instant.now(),
					service.getMqttConfig().getServerUriValue(), "some/topic", false,
					MqttQos.ExactlyOnce, String.format("Hello, world %d", i).getBytes());
			entities.add(entity);
		}

		// first "page" of cached messages
		Capture<BatchableDao.BatchCallback<MqttMessageEntity>> callbackCaptor = Capture.newInstance();
		Capture<BatchableDao.BatchOptions> optionsCaptor = Capture.newInstance();
		expect(messageDao.batchProcess(capture(callbackCaptor), capture(optionsCaptor)))
				.andAnswer(new IAnswer<BatchableDao.BatchResult>() {

					@Override
					public BatchableDao.BatchResult answer() throws Throwable {
						callbackCaptor.getValue().handle(entities.get(0));
						callbackCaptor.getValue().handle(entities.get(1));
						return new BasicBatchResult(2);
					}
				});

		// second "page"
		Capture<BatchableDao.BatchCallback<MqttMessageEntity>> callbackCaptor2 = Capture.newInstance();
		Capture<BatchableDao.BatchOptions> optionsCaptor2 = Capture.newInstance();
		expect(messageDao.batchProcess(capture(callbackCaptor2), capture(optionsCaptor2)))
				.andAnswer(new IAnswer<BatchableDao.BatchResult>() {

					@Override
					public BatchableDao.BatchResult answer() throws Throwable {
						callbackCaptor2.getValue().handle(entities.get(2));
						return new BasicBatchResult(1);
					}
				});

		// third "page" (no batch callback, so stop processing)
		Capture<BatchableDao.BatchCallback<MqttMessageEntity>> callbackCaptor3 = Capture.newInstance();
		Capture<BatchableDao.BatchOptions> optionsCaptor3 = Capture.newInstance();
		expect(messageDao.batchProcess(capture(callbackCaptor3), capture(optionsCaptor3)))
				.andAnswer(new IAnswer<BatchableDao.BatchResult>() {

					@Override
					public BatchableDao.BatchResult answer() throws Throwable {
						return new BasicBatchResult(0);
					}
				});

		Capture<MqttMessage> msgCaptor = Capture.newInstance(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).times(3);

		// WHEN
		replayAll();
		service.init();
		service.onMqttServerConnectionEstablished(connection, false);

		// THEN
		List<MqttMessage> pubMessages = msgCaptor.getValues();
		assertThat("Published all cached messages", pubMessages, hasSize(entities.size()));
		for ( int i = 0; i < entities.size(); i++ ) {
			MqttMessageEntity entity = entities.get(i);
			MqttMessage pubMessage = pubMessages.get(i);
			assertThat("Cached message published", pubMessage, is(notNullValue()));
			assertThat("Published cached message topic", pubMessage.getTopic(),
					is(equalTo(entity.getTopic())));
			assertThat("Published cached message QoS", pubMessage.getQosLevel(),
					is(equalTo(entity.getQosLevel())));
			assertThat("Published cached message payload",
					Arrays.equals(pubMessage.getPayload(), entity.getPayload()), is(equalTo(true)));
		}
	}

	private static Map<String, Object> datumMap(NodeDatum datum) {
		return datumMap(datum, "_.*");
	}

	private static Map<String, Object> datumMap(NodeDatum datum, String exPattern) {
		Pattern ex = (exPattern != null ? Pattern.compile(exPattern) : null);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Map<String, Object> m = (Map) datum.asSimpleMap();
		for ( Iterator<Entry<String, Object>> itr = m.entrySet().iterator(); itr.hasNext(); ) {
			Entry<String, Object> e = itr.next();
			if ( ex != null && ex.matcher(e.getKey()).matches() ) {
				itr.remove();
			}
		}
		return m;
	}

	@Test
	public void postDatum() throws Exception {
		// GIVEN
		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		assertMessage(publishedMsg, TEST_SOURCE_ID, datumMap(datum));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void postDatum_invalidSourceId() throws Exception {
		// GIVEN
		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		CompletableFuture<Object> f = new CompletableFuture<>();
		f.completeExceptionally(new IllegalArgumentException("Bad topic"));
		expect(connection.publish(capture(msgCaptor))).andReturn((Future) f);

		// WHEN
		replayAll();
		service.init();

		String sourceId = "test#source";
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		assertMessage(publishedMsg, sourceId, datumMap(datum));
	}

	@Test
	public void postDatum_notRetained() throws Exception {
		// GIVEN
		service.setPublishRetained(false);
		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		assertMessage(publishedMsg, TEST_SOURCE_ID, datumMap(datum));
	}

	@Test
	public void postDatum_withHalf() throws Exception {
		// GIVEN
		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		Half h = new Half(1.23f);
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", h);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		Map<String, Object> expectedMap = datumMap(datum);
		expectedMap.put("watts", NumberUtils.bigDecimalForNumber(h));
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

	@Test
	public void postDatum_withVersionTag() throws Exception {
		// GIVEN
		service.setIncludeVersionTag(true);
		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();
		Map<String, Object> expectedMap = datumMap(datum);
		expectedMap.put(FluxUploadService.TAG_VERSION, 2);
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

	@Test
	public void postDatum_globalExcludeProps() throws Exception {
		// GIVEN
		final String excludeRegex = "w.*";
		service.setExcludePropertyNamesRegex(excludeRegex);

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		datum.getSamples().putInstantaneousSampleValue("foo", 3456);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum, excludeRegex);
		expectedMap.remove("watts");
		expectedMap.remove("wattHours");
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

	@Test
	public void postDatum_globalExcludeProps_underscoreOrSourceId() throws Exception {
		// GIVEN
		final String excludeRegex = "(_.*|sourceId)";
		service.setExcludePropertyNamesRegex(excludeRegex);

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum, excludeRegex);
		expectedMap.remove(Datum.SOURCE_ID);
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

	@Test
	public void postDatum_excludeAllProps_anySource() throws Exception {
		// GIVEN
		final String excludeRegex = ".*";
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropExcludeValues(new String[] { excludeRegex });
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		// nothing
	}

	@Test
	public void postDatum_excludeAllProps_specificSource() throws Exception {
		// GIVEN
		final String excludeRegex = ".*";
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setSourceIdRegexValue("^test");
		filter.setPropExcludeValues(new String[] { excludeRegex });
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		SimpleDatum datum2 = SimpleDatum.nodeDatum("not.filtered");
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum2);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum2);
		assertMessage(publishedMsg, datum2.getSourceId(), expectedMap);
	}

	@Test
	public void postDatum_includeProps() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropIncludeValues(new String[] { "^watt" });
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		datum.getSamples().putInstantaneousSampleValue("foo", 3456);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum, "[^w].*");
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

	@Test
	public void postDatum_includeProps_withXformFilter() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setSourceIdRegexValue("test");
		filter.setPropIncludeValues(new String[] { "^watt" });
		filter.configurationChanged(null);
		filter.setTransformServiceUid("test.filter");
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		expect(xformService.getUid()).andReturn("test.filter").anyTimes();
		Capture<DatumSamplesOperations> samplesCaptor = Capture.newInstance();
		DatumSamples xformed = new DatumSamples();
		xformed.putInstantaneousSampleValue("wattsX", 12345);
		xformed.putInstantaneousSampleValue("wattHoursX", 23456);
		xformed.putStatusSampleValue("wattFoo", "bar");
		expect(xformService.filter(anyObject(), capture(samplesCaptor), anyObject())).andReturn(xformed);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		datum.getSamples().putInstantaneousSampleValue("foo", 3456);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> filteredDatum = new LinkedHashMap<>(xformed.getInstantaneous());
		filteredDatum.put("wattFoo", "bar");
		assertMessage(publishedMsg, TEST_SOURCE_ID, filteredDatum);

		DatumSamplesOperations filterInput = samplesCaptor.getValue();
		assertThat("Input watts", filterInput.getSampleInteger(Instantaneous, "watts"),
				is(equalTo(1234)));
		assertThat("Input wattHours", filterInput.getSampleInteger(Accumulating, "wattHours"),
				is(equalTo(2345)));
		assertThat("Input foo", filterInput.getSampleInteger(Instantaneous, "foo"), is(equalTo(3456)));
	}

	@Test
	public void postDatum_excludeProps() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setPropExcludeValues(new String[] { "^watt" });
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		datum.getSamples().putInstantaneousSampleValue("foo", 3456);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum, "(_.*|^watt.*)");
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
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

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		datum.getSamples().putInstantaneousSampleValue("foo", 3456);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum, "(_.*|foo|wattHours)");
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

	@Test
	public void postDatum_throttle_anySource() throws Exception {
		// GIVEN
		FluxFilterConfig filter = new FluxFilterConfig();
		filter.setFrequencySeconds(1);
		filter.configurationChanged(null);
		service.setFilters(new FluxFilterConfig[] { filter });

		expectMqttConnectionSetup();

		Capture<MqttMessage> msgCaptor = Capture.newInstance(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).anyTimes();

		// WHEN
		replayAll();
		service.init();

		List<NodeDatum> publishedDatum = new ArrayList<NodeDatum>(2);
		SimpleDatum datum = SimpleDatum.nodeDatum("s1");
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		// post a bunch of events within the throttle window; only one should be captured
		publishedDatum.add(publishLoop(1000, datum));

		Thread.sleep(200);

		publishedDatum.add(publishLoop(1000, datum));

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("Only 2 MQTT messages published because of throttle filter", publishedMsgs,
				hasSize(2));
		assertMessage(publishedMsgs.get(0), "s1", datumMap(publishedDatum.get(0)));
		assertMessage(publishedMsgs.get(1), "s1", datumMap(publishedDatum.get(1)));
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

		Capture<MqttMessage> msgCaptor = Capture.newInstance(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).anyTimes();

		// WHEN
		replayAll();
		service.init();

		List<NodeDatum> publishedDatum = new ArrayList<NodeDatum>(2);
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		// post a bunch of events within the throttle window; only one should be captured
		publishedDatum.add(publishLoop(1000, datum));

		Thread.sleep(200);

		publishedDatum.add(publishLoop(1000, datum));

		// switch source ID to non-matching
		SimpleDatum datum2 = datum
				.copyWithId(DatumId.nodeId(null, "not.throttled.source", datum.getTimestamp()));
		publishLoop(1000, datum2);

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("More than 2 MQTT messages published because of throttle filter",
				publishedMsgs.size(), greaterThan(2));

		MqttMessage publishedMsg = publishedMsgs.get(0);
		assertMessage(publishedMsg, TEST_SOURCE_ID, datumMap(publishedDatum.get(0)));

		publishedMsg = publishedMsgs.get(1);
		assertMessage(publishedMsg, TEST_SOURCE_ID, datumMap(publishedDatum.get(1)));

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

		Capture<MqttMessage> msgCaptor = Capture.newInstance(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).anyTimes();

		// WHEN
		replayAll();
		service.init();

		List<NodeDatum> publishedDatum = new ArrayList<NodeDatum>(2);
		SimpleDatum datum = SimpleDatum.nodeDatum("s1");
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

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

		Capture<MqttMessage> msgCaptor = Capture.newInstance(CaptureType.ALL);
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null)).anyTimes();

		// WHEN
		replayAll();
		service.init();

		List<NodeDatum> publishedDatum = new ArrayList<NodeDatum>(2);
		SimpleDatum datum = SimpleDatum.nodeDatum("s1");
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		// post a bunch of events within the throttle window; only one should be captured
		publishedDatum.add(publishLoop(1000, datum));

		Thread.sleep(200);

		publishedDatum.add(publishLoop(1000, datum));

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("Only 2 MQTT messages published because of throttle filter", publishedMsgs,
				hasSize(2));
		assertMessage(publishedMsgs.get(0), "s1", datumMap(publishedDatum.get(0)));
		assertMessage(publishedMsgs.get(1), "s1", datumMap(publishedDatum.get(1)));
	}

	private static final class TestInjectorTransformService extends BaseIdentifiable
			implements DatumFilterService {

		private final DatumSamples staticSamples;

		public TestInjectorTransformService(String uid, DatumSamples staticSamples) {
			super();
			setUid(uid);
			this.staticSamples = staticSamples;
		}

		@Override
		public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
				Map<String, Object> parameters) {
			DatumSamples result = new DatumSamples(samples);
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

		DatumSamples s1 = new DatumSamples();
		s1.putInstantaneousSampleValue("foo", 1);
		TestInjectorTransformService x1 = new TestInjectorTransformService("test1.filter", s1);

		DatumSamples s2 = new DatumSamples();
		s2.putInstantaneousSampleValue("bar", 1);
		TestInjectorTransformService x2 = new TestInjectorTransformService("test2.filter", s2);

		service.setTransformServices(new StaticOptionalServiceCollection<>(asList(x1, x2)));

		expectMqttConnectionSetup();

		expect(operationalModeService.isOperationalModeActive("test")).andReturn(true);
		expect(operationalModeService.isOperationalModeActive("!test")).andReturn(false);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum("s1");
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("1 MQTT messages published", publishedMsgs, hasSize(1));

		Map<String, Object> expectedMap = datumMap(datum);
		expectedMap.put("foo", 1);
		assertMessage(publishedMsgs.get(0), "s1", expectedMap);
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

		DatumSamples s1 = new DatumSamples();
		s1.putInstantaneousSampleValue("foo", 1);
		TestInjectorTransformService x1 = new TestInjectorTransformService("test1.filter", s1);

		DatumSamples s2 = new DatumSamples();
		s2.putInstantaneousSampleValue("bar", 1);
		TestInjectorTransformService x2 = new TestInjectorTransformService("test2.filter", s2);

		service.setTransformServices(new StaticOptionalServiceCollection<>(asList(x1, x2)));

		expectMqttConnectionSetup();

		expect(operationalModeService.isOperationalModeActive("test")).andReturn(false);
		expect(operationalModeService.isOperationalModeActive("!test")).andReturn(true);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum("s1");
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

		// THEN
		List<MqttMessage> publishedMsgs = msgCaptor.getValues();
		assertThat("1 MQTT messages published", publishedMsgs, hasSize(1));

		Map<String, Object> expectedMap = datumMap(datum);
		expectedMap.put("bar", 1);
		assertMessage(publishedMsgs.get(0), "s1", expectedMap);
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
		Capture<Object> encoderObjectCaptor = Capture.newInstance();
		expect(encoder.encodeAsBytes(capture(encoderObjectCaptor), isNull())).andReturn(encodedBytes);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

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
		Capture<Object> encoderObjectCaptor = Capture.newInstance();
		expect(encoder.encodeAsBytes(capture(encoderObjectCaptor), isNull())).andReturn(encodedBytes);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

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
		Capture<Object> encoderObjectCaptor = Capture.newInstance();
		expect(encoder.encodeAsBytes(capture(encoderObjectCaptor), isNull())).andReturn(encodedBytes);

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		service.accept(datum);

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

		Capture<MqttMessage> msgCaptor = Capture.newInstance();
		expect(connection.publish(capture(msgCaptor))).andReturn(completedFuture(null));

		// WHEN
		replayAll();
		service.init();

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);
		datum.getSamples().putAccumulatingSampleValue("wattHours", 2345);
		datum.getSamples().putInstantaneousSampleValue("foo", 3456);
		service.accept(datum);

		// THEN
		MqttMessage publishedMsg = msgCaptor.getValue();

		Map<String, Object> expectedMap = datumMap(datum);
		expectedMap.remove("created");
		expectedMap.remove("watts");
		expectedMap.remove("wattHours");
		assertMessage(publishedMsg, TEST_SOURCE_ID, expectedMap);
	}

}
