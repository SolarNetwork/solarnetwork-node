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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.charge.rfid.RfidChargeSessionManager;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.CapturingExecutorService;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link RfidChargeSessionManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidChargeSessionManagerTests extends AbstractNodeTest {

	private static final String TEST_RFID_UID = "Test RFID Scanner";
	private static final String TEST_ID_TAG = "TestIdTag";
	private static final String TEST_SOCKET_ID = "/socket/test";
	private static final String TEST_SESSION_ID = "test-session-id";

	private ChargeSessionManager chargeSessionManager;
	private InstructionHandler instructionHandler;
	private EventAdmin eventAdmin;

	private RfidChargeSessionManager manager;

	@Before
	public void setup() {
		chargeSessionManager = EasyMock.createMock(ChargeSessionManager.class);
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		instructionHandler = EasyMock.createMock(InstructionHandler.class);

		manager = new RfidChargeSessionManager();
		manager.setExecutor(new CapturingExecutorService(Executors.newSingleThreadExecutor()));
		manager.setChargeSessionManager(chargeSessionManager);
		manager.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		manager.setInstructionHandlers(Collections.singleton(instructionHandler));
	}

	@After
	public void shutdown() {
		manager.shutdown();
	}

	private void replayAll() {
		replay(chargeSessionManager, eventAdmin, instructionHandler);
	}

	private void resetAll() {
		EasyMock.reset(chargeSessionManager, eventAdmin, instructionHandler);
		if ( manager.getExecutor().isShutdown() ) {
			manager.setExecutor(new CapturingExecutorService(Executors.newSingleThreadExecutor()));
		}
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
		verify(chargeSessionManager, eventAdmin, instructionHandler);

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

	@Test
	public void startupServiceNoActiveSessions() {
		List<String> socketIds = Arrays.asList("/socket/test/1", "/socket/test/2");
		expect(chargeSessionManager.availableSocketIds()).andReturn(socketIds);

		Capture<Instruction> instructionCapt = new Capture<Instruction>(CaptureType.ALL);
		Capture<Event> eventCapt = new Capture<Event>(CaptureType.ALL);

		for ( String socketId : socketIds ) {
			expect(chargeSessionManager.activeChargeSession(socketId)).andReturn(null);
			expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER))
					.andReturn(true);
			expect(instructionHandler.processInstruction(capture(instructionCapt)))
					.andReturn(InstructionState.Completed);
			eventAdmin.postEvent(capture(eventCapt));
		}

		replayAll();

		manager.startup();

		verifyAll(true);

		// for each socket, verify instructions / events
		List<Instruction> instructions = instructionCapt.getValues();
		assertEquals(2, instructions.size());

		List<Event> events = eventCapt.getValues();
		assertEquals(2, events.size());

		for ( int i = 0; i < socketIds.size(); i++ ) {
			String socketId = socketIds.get(i);
			assertEquals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, instructions.get(i).getTopic());
			assertEquals(Boolean.FALSE.toString(), instructions.get(i).getParameterValue(socketId));
			assertEquals(ChargeSessionManager.EVENT_TOPIC_SOCKET_DEACTIVATED, events.get(i).getTopic());
			assertEquals(socketId,
					events.get(i).getProperty(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID));
		}
	}

	@Test
	public void startupServiceActiveSession() throws InterruptedException {
		List<String> socketIds = Arrays.asList("/socket/test/1", "/socket/test/2");
		ChargeSession activeSession = new ChargeSession();
		expect(chargeSessionManager.availableSocketIds()).andReturn(socketIds);

		Capture<Instruction> instructionCapt = new Capture<Instruction>(CaptureType.ALL);
		Capture<Event> eventCapt = new Capture<Event>(CaptureType.ALL);

		for ( String socketId : socketIds ) {
			expect(chargeSessionManager.activeChargeSession(socketId))
					.andReturn(socketId.endsWith("1") ? activeSession : null);
			expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER))
					.andReturn(true);
			expect(instructionHandler.processInstruction(capture(instructionCapt)))
					.andReturn(InstructionState.Completed);
			eventAdmin.postEvent(capture(eventCapt));
		}

		replayAll();

		manager.startup();

		verifyAll(true);

		// for each socket, verify instructions / events
		List<Instruction> instructions = instructionCapt.getValues();
		assertEquals(2, instructions.size());

		List<Event> events = eventCapt.getValues();
		assertEquals(2, events.size());

		for ( int i = 0; i < socketIds.size(); i++ ) {
			String socketId = socketIds.get(i);
			assertEquals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, instructions.get(i).getTopic());
			assertEquals(i == 0 ? Boolean.TRUE.toString() : Boolean.FALSE.toString(),
					instructions.get(i).getParameterValue(socketId));
			assertEquals(
					i == 0 ? ChargeSessionManager.EVENT_TOPIC_SOCKET_ACTIVATED
							: ChargeSessionManager.EVENT_TOPIC_SOCKET_DEACTIVATED,
					events.get(i).getTopic());
			assertEquals(socketId,
					events.get(i).getProperty(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID));
		}
	}

	private Event createRfidEvent(final String msg) {
		Map<String, Object> eventProps = new HashMap<String, Object>(5);
		eventProps.put(RfidChargeSessionManager.EVENT_PARAM_UID, TEST_RFID_UID);
		eventProps.put(RfidChargeSessionManager.EVENT_PARAM_MESSAGE, msg);
		return new Event(RfidChargeSessionManager.TOPIC_RFID_MESSAGE_RECEIVED, eventProps);
	}

	@Test
	public void startChargeSessionNoActiveSessions() throws Exception {
		startupServiceNoActiveSessions();

		resetAll();

		List<String> availableSocketIds = Arrays.asList(TEST_SOCKET_ID);

		// get available sockets
		expect(chargeSessionManager.availableSocketIds()).andReturn(availableSocketIds);

		// for each socket, look for active session
		expect(chargeSessionManager.activeChargeSession(TEST_SOCKET_ID)).andReturn(null).anyTimes();

		// no active sessions, so start one for the available socket
		expect(chargeSessionManager.initiateChargeSession(TEST_ID_TAG, TEST_SOCKET_ID, null))
				.andReturn(TEST_SESSION_ID);

		// and because session started, enable the socket
		Capture<Instruction> instructionCapt = new Capture<Instruction>();
		expect(instructionHandler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER))
				.andReturn(true);
		expect(instructionHandler.processInstruction(capture(instructionCapt)))
				.andReturn(InstructionState.Completed);

		// and then post socket active event
		Capture<Event> eventCapt = new Capture<Event>();
		eventAdmin.postEvent(capture(eventCapt));

		manager.handleEvent(createRfidEvent(TEST_ID_TAG));

		replayAll();

		verifyAll(true);

		Instruction enableSocketInstr = instructionCapt.getValue();
		assertEquals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, enableSocketInstr.getTopic());
		assertEquals(Boolean.TRUE.toString(), enableSocketInstr.getParameterValue(TEST_SOCKET_ID));
		Event enabledSocketEvent = eventCapt.getValue();
		assertEquals(ChargeSessionManager.EVENT_TOPIC_SOCKET_ACTIVATED, enabledSocketEvent.getTopic());
		assertEquals(TEST_SOCKET_ID,
				enabledSocketEvent.getProperty(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID));
	}

}
