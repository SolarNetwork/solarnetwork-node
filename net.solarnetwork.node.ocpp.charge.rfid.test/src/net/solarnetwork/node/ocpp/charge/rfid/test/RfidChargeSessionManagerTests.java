/* ==================================================================
 * RfidChargeSessionManagerTests.java - 30/07/2016 12:50:40 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.charge.rfid.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.SocketManager;
import net.solarnetwork.node.ocpp.charge.rfid.RfidChargeSessionManager;
import net.solarnetwork.node.ocpp.charge.rfid.RfidSocketMapping;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.CapturingExecutorService;

/**
 * Test cases for the {@link RfidChargeSessionManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidChargeSessionManagerTests extends AbstractNodeTest {

	private static final String TEST_RFID_UID = "Test RFID Scanner";
	private static final String TEST_RFID_UID2 = "Test RFID Scanner 2";
	private static final String TEST_ID_TAG = "TestIdTag";
	private static final String TEST_SOCKET_ID = "/socket/test/1";
	private static final String TEST_SOCKET_ID2 = "/socket/test/2";
	private static final String TEST_SESSION_ID = "test-session-id";

	private ChargeSessionManager chargeSessionManager;
	private SocketManager socketManager;

	private RfidChargeSessionManager manager;

	@Before
	public void setup() {
		chargeSessionManager = EasyMock.createMock(ChargeSessionManager.class);
		socketManager = EasyMock.createMock(SocketManager.class);

		manager = new RfidChargeSessionManager();
		manager.setExecutor(new CapturingExecutorService(Executors.newSingleThreadExecutor()));
		manager.setChargeSessionManager(chargeSessionManager);
		manager.setSocketManager(socketManager);
	}

	@After
	public void shutdown() {
		manager.shutdown();
	}

	private void replayAll() {
		replay(chargeSessionManager, socketManager);
	}

	private List<Future<?>> verifyAll(boolean waitForTasks) {
		List<Future<?>> futures = null;
		if ( waitForTasks ) {
			// wait for startup thread to complete
			manager.getExecutor().shutdown();
			try {
				manager.getExecutor().awaitTermination(2, TimeUnit.MINUTES);
			} catch ( InterruptedException e ) {
				// ignore
			}
			if ( manager.getExecutor() instanceof CapturingExecutorService ) {
				futures = ((CapturingExecutorService) manager.getExecutor()).getCapturedFutures();
			}
		}
		verify(chargeSessionManager, socketManager);

		if ( futures != null ) {
			for ( Future<?> future : futures ) {
				if ( future.isDone() ) {
					// trigger ExecutionException
					try {
						future.get();
					} catch ( InterruptedException e ) {
						// ignore
					} catch ( ExecutionException e ) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		return futures;
	}

	private Event createRfidEvent(final String msg) {
		return createRfidEvent(msg, TEST_RFID_UID);
	}

	private Event createRfidEvent(final String msg, final String uid) {
		Map<String, Object> eventProps = new HashMap<String, Object>(5);
		eventProps.put(RfidChargeSessionManager.EVENT_PARAM_UID, uid);
		eventProps.put(RfidChargeSessionManager.EVENT_PARAM_MESSAGE, msg);
		return new Event(RfidChargeSessionManager.TOPIC_RFID_MESSAGE_RECEIVED, eventProps);
	}

	@Test
	public void startChargeSessionNoActiveSessions() {
		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(null).anyTimes();

		// no active sessions, so start one for the available socket
		expect(chargeSessionManager.initiateChargeSession(TEST_ID_TAG, TEST_SOCKET_ID, null))
				.andReturn(TEST_SESSION_ID);

		// and because session started, enable the socket
		expect(socketManager.adjustSocketEnabledState(TEST_SOCKET_ID, true)).andReturn(true);

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG));

		verifyAll(true);
	}

	@Test
	public void startChargeSessionActiveSessionsNoSocketAvailable() {
		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		ChargeSession activeSession = new ChargeSession();
		activeSession.setIdTag("some.other.id");
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(activeSession);

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG));

		verifyAll(true);
	}

	@Test
	public void startChargeSessionActiveSessionsSecondSocketAvailable() {
		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID, TEST_SOCKET_ID2);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		ChargeSession activeSession = new ChargeSession();
		activeSession.setIdTag("some.other.id");
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(activeSession);

		// first socket has session, so test 2nd socket
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID2)).andReturn(null).anyTimes();

		// start session on the 2nd socket
		expect(chargeSessionManager.initiateChargeSession(TEST_ID_TAG, TEST_SOCKET_ID2, null))
				.andReturn(TEST_SESSION_ID);

		// and because session started, enable the socket
		expect(socketManager.adjustSocketEnabledState(TEST_SOCKET_ID2, true)).andReturn(true);

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG));

		verifyAll(true);
	}

	private void configureRfidMappings() {
		List<RfidSocketMapping> mappings = new ArrayList<RfidSocketMapping>(2);
		mappings.add(new RfidSocketMapping(TEST_RFID_UID, TEST_SOCKET_ID));
		mappings.add(new RfidSocketMapping(TEST_RFID_UID2, TEST_SOCKET_ID2));
		manager.setRfidSocketMappings(mappings);
	}

	@Test
	public void startChargeSessionWithMappingNoMatchingRfidUID() {
		configureRfidMappings();

		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(null).anyTimes();

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG, "Rogue Scanner"));

		verifyAll(true);
	}

	@Test
	public void startChargeSessionWithMappingNoActiveSession() {
		configureRfidMappings();

		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(null).anyTimes();

		// no active sessions, so start one for the available socket
		expect(chargeSessionManager.initiateChargeSession(TEST_ID_TAG, TEST_SOCKET_ID, null))
				.andReturn(TEST_SESSION_ID);

		// and because session started, enable the socket
		expect(socketManager.adjustSocketEnabledState(TEST_SOCKET_ID, true)).andReturn(true);

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG, TEST_RFID_UID));

		verifyAll(true);
	}

	@Test
	public void startChargeSessionWithMappingActiveSessionsSecondSocketAvailable() {
		configureRfidMappings();

		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID, TEST_SOCKET_ID2);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		ChargeSession activeSession = new ChargeSession();
		activeSession.setIdTag("some.other.id");
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(activeSession);

		// first socket has session, so test 2nd socket
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID2)).andReturn(null).anyTimes();

		// session not started though, because RFID UID maps only to socket 1

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG, TEST_RFID_UID));

		verifyAll(true);
	}

	@Test
	public void endChargeSession() {
		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// look for active session on socket
		ChargeSession activeSession = new ChargeSession();
		activeSession.setSessionId(TEST_SESSION_ID);
		activeSession.setIdTag(TEST_ID_TAG);
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(activeSession)
				.anyTimes();

		// found session, so end it
		chargeSessionManager.completeChargeSession(TEST_ID_TAG, TEST_SESSION_ID);

		// and because session ended, disable the socket
		expect(socketManager.adjustSocketEnabledState(TEST_SOCKET_ID, false)).andReturn(true);

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG));

		verifyAll(true);
	}

	@Test
	public void endChargeSessionWithMapping() {
		configureRfidMappings();

		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// look for active session on socket
		ChargeSession activeSession = new ChargeSession();
		activeSession.setSessionId(TEST_SESSION_ID);
		activeSession.setIdTag(TEST_ID_TAG);
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(activeSession)
				.anyTimes();

		// found session, so end it
		chargeSessionManager.completeChargeSession(TEST_ID_TAG, TEST_SESSION_ID);

		// and because session ended, disable the socket
		expect(socketManager.adjustSocketEnabledState(TEST_SOCKET_ID, false)).andReturn(true);

		replayAll();

		manager.handleEvent(createRfidEvent(TEST_ID_TAG, TEST_RFID_UID));

		verifyAll(true);
	}

	@Test
	public void endChargeSessionWithMappingEvenThoughOtherRfidUID() {
		configureRfidMappings();

		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// look for active session on socket
		ChargeSession activeSession = new ChargeSession();
		activeSession.setSessionId(TEST_SESSION_ID);
		activeSession.setIdTag(TEST_ID_TAG);
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(activeSession)
				.anyTimes();

		// found session, so end it
		chargeSessionManager.completeChargeSession(TEST_ID_TAG, TEST_SESSION_ID);

		// and because session ended, disable the socket
		expect(socketManager.adjustSocketEnabledState(TEST_SOCKET_ID, false)).andReturn(true);

		replayAll();

		// now send RFID scan from OTHER device; shouldn't matter because we find the session
		// based on the tag value
		manager.handleEvent(createRfidEvent(TEST_ID_TAG, TEST_RFID_UID2));

		verifyAll(true);
	}

}
