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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.runtime.EventMessageBridge;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link EventMessageBridge} class.
 * 
 * @author matt
 * @version 1.0
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
		Capture<String> destCaptor = new Capture<String>();
		Capture<Object> msgCaptor = new Capture<Object>();
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
		Capture<String> destCaptor = new Capture<String>();
		Capture<Object> msgCaptor = new Capture<Object>();
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

}
