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
import java.util.List;
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
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link RfidChargeSessionManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidChargeSessionManagerTests extends AbstractNodeTest {

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

	private void verifyAll() {
		verify(chargeSessionManager, eventAdmin, instructionHandler);
	}

	@Test
	public void startupServiceNoActiveSessions() throws InterruptedException {
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

		// wait for startup thread to complete
		manager.getExecutor().shutdown();
		manager.getExecutor().awaitTermination(2, TimeUnit.SECONDS);

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

		// wait for startup thread to complete
		manager.getExecutor().shutdown();
		manager.getExecutor().awaitTermination(2, TimeUnit.SECONDS);

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
		}
	}

	@Test
	public void startChargeSession() {
	}

}
