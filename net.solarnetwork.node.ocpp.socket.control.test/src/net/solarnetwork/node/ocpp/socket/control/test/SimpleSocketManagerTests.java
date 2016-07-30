/* ==================================================================
 * SimpleSocketManagerTests.java - 31/07/2016 9:09:04 AM
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

package net.solarnetwork.node.ocpp.socket.control.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.socket.control.SimpleSocketManager;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link SimpleSocketManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleSocketManagerTests {

	private ChargeSessionManager chargeSessionManager;
	private InstructionHandler instructionHandler;
	private EventAdmin eventAdmin;

	private SimpleSocketManager manager;

	@Before
	public void setup() {
		chargeSessionManager = EasyMock.createMock(ChargeSessionManager.class);
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		instructionHandler = EasyMock.createMock(InstructionHandler.class);

		manager = new SimpleSocketManager();
		manager.setChargeSessionManager(chargeSessionManager);
		manager.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		manager.setInstructionHandlers(Collections.singleton(instructionHandler));
	}

	private void replayAll() {
		replay(chargeSessionManager, eventAdmin, instructionHandler);
	}

	private void verifyAll() {
		verify(chargeSessionManager, eventAdmin, instructionHandler);
	}

	@Test
	public void verifyAllSocketsNoActiveSessions() {
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

		manager.verifyAllSockets();

		verifyAll();

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

		manager.verifyAllSockets();

		verifyAll();

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
}
