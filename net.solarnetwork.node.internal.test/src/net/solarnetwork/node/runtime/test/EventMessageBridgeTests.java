/* ==================================================================
 * EventMessageBridgeTests.java - 25/09/2017 3:39:25 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime.test;

import static org.easymock.EasyMock.capture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.runtime.EventMessageBridge;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link EventMessageBridge} class.
 * 
 * @author matt
 * @version 1.1
 */
public class EventMessageBridgeTests {

	private SimpMessageSendingOperations messageSendingOps;
	private EventMessageBridge eventMessageBridge;

	@Before
	public void setup() {
		messageSendingOps = EasyMock.createMock(SimpMessageSendingOperations.class);

		eventMessageBridge = new EventMessageBridge(
				new StaticOptionalService<SimpMessageSendingOperations>(messageSendingOps));
	}

	@After
	public void teardown() {
		EasyMock.verify(messageSendingOps);
	}

	private void replayAll() {
		EasyMock.replay(messageSendingOps);
	}

	@Test
	public void handleDatumStoredEvent() {
		Capture<String> destCaptor = Capture.newInstance();
		Capture<Object> msgCaptor = Capture.newInstance();
		messageSendingOps.convertAndSend(capture(destCaptor), capture(msgCaptor),
				EasyMock.<Map<String, Object>> isNull());

		replayAll();

		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("sourceId", "test-source");
		data.put("watts", 123);
		data.put("wattHours", 12345L);
		Event event = new Event(DatumDao.EVENT_TOPIC_DATUM_STORED, data);
		eventMessageBridge.handleEvent(event);

		assertThat("Message topic", destCaptor.getValue(), equalTo("/topic/datum/stored/test-source"));
		assertThat("Message body", msgCaptor.getValue(), instanceOf(Result.class));
		Object msgData = ((Result<?>) msgCaptor.getValue()).getData();
		assertThat("Message data", msgData, instanceOf(Map.class));

		Map<String, Object> expectedMsgData = new HashMap<String, Object>(data);
		expectedMsgData.put("event.topics", DatumDao.EVENT_TOPIC_DATUM_STORED);
		assertThat("Message data", msgData, equalTo((Object) expectedMsgData));
	}

	@Test
	public void handleDatumCapturedEvent() {
		Capture<String> destCaptor = Capture.newInstance();
		Capture<Object> msgCaptor = Capture.newInstance();
		messageSendingOps.convertAndSend(capture(destCaptor), capture(msgCaptor),
				EasyMock.<Map<String, Object>> isNull());

		replayAll();

		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("sourceId", "test-source");
		data.put("watts", 123);
		data.put("wattHours", 12345L);
		Event event = new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, data);
		eventMessageBridge.handleEvent(event);

		assertThat("Message topic", destCaptor.getValue(), equalTo("/topic/datum/captured/test-source"));
		assertThat("Message body", msgCaptor.getValue(), instanceOf(Result.class));
		Object msgData = ((Result<?>) msgCaptor.getValue()).getData();
		assertThat("Message data", msgData, instanceOf(Map.class));

		Map<String, Object> expectedMsgData = new HashMap<String, Object>(data);
		expectedMsgData.put("event.topics", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED);
		assertThat("Message data", msgData, equalTo((Object) expectedMsgData));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void handleDatumCapturedEvent_datum() {
		Capture<String> destCaptor = Capture.newInstance();
		Capture<Object> msgCaptor = Capture.newInstance();
		messageSendingOps.convertAndSend(capture(destCaptor), capture(msgCaptor),
				EasyMock.<Map<String, Object>> isNull());

		replayAll();

		SimpleDatum d = SimpleDatum.nodeDatum("test-source", Instant.now(), new DatumSamples());
		d.getSamples().putInstantaneousSampleValue("wattts", 123);
		d.getSamples().putAccumulatingSampleValue("wattHours", 12345L);
		Event event = DatumEvents.datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, d);
		eventMessageBridge.handleEvent(event);

		assertThat("Message topic", destCaptor.getValue(), equalTo("/topic/datum/captured/test-source"));
		assertThat("Message body", msgCaptor.getValue(), instanceOf(Result.class));
		Object msgData = ((Result<?>) msgCaptor.getValue()).getData();
		assertThat("Message data", msgData, instanceOf(Map.class));

		Map<String, Object> expectedMsgData = new LinkedHashMap<>();
		expectedMsgData.putAll(d.asSimpleMap());
		expectedMsgData.put("event.topics", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED);
		expectedMsgData.remove("_DatumTypes");
		for ( Entry<String, Object> me : expectedMsgData.entrySet() ) {
			String prop = me.getKey();
			assertThat("Message data " + prop, (Map<String, ?>) msgData, hasEntry(prop, me.getValue()));
		}
		assertThat("Message data includes datum", (Map<String, ?>) msgData,
				hasEntry(DatumEvents.DATUM_PROPERTY, d));
	}

	@Test
	public void handleDatumCapturedEvent_datum_json() throws Exception {
		Capture<Object> msgCaptor = Capture.newInstance();
		messageSendingOps.convertAndSend(EasyMock.anyString(), capture(msgCaptor),
				EasyMock.<Map<String, Object>> isNull());

		replayAll();

		SimpleDatum d = SimpleDatum.nodeDatum("test-source", Instant.ofEpochMilli(1760000000123L),
				new DatumSamples());
		d.getSamples().putInstantaneousSampleValue("watts", 123);
		d.getSamples().putAccumulatingSampleValue("wattHours", 12345L);
		d.getSamples().putStatusSampleValue("status", "ok");
		d.getSamples().addTag("power");
		Event event = DatumEvents.datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, d);
		eventMessageBridge.handleEvent(event);

		// the node's message converter uses the node's datum ObjectMapper
		JsonNode json = JsonUtils.newDatumObjectMapper().valueToTree(msgCaptor.getValue());
		JsonNode datum = json.path("data").path(DatumEvents.DATUM_PROPERTY);
		assertThat("Datum created", datum.path("created").asText(), equalTo("2025-10-09 08:53:20.123Z"));
		assertThat("Datum source ID", datum.path("sourceId").asText(), equalTo("test-source"));
		assertThat("Datum instantaneous", datum.path("i").path("watts").asInt(), equalTo(123));
		assertThat("Datum accumulating", datum.path("a").path("wattHours").asLong(), equalTo(12345L));
		assertThat("Datum status", datum.path("s").path("status").asText(), equalTo("ok"));
		assertThat("Datum tags", datum.path("t").path(0).asText(), equalTo("power"));
		assertThat("Flat property still included", json.path("data").path("watts").asInt(),
				equalTo(123));
	}

}
