/* ==================================================================
 * OperationalStateManagerTests.java - 15/05/2019 11:28:15 am
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

package net.solarnetwork.node.control.opmode.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.node.service.OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES;
import static net.solarnetwork.node.service.OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.control.opmode.OperationalStateManager;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link OperationalStateManager} class.
 * 
 * @author matt
 * @version 2.0
 */
public class OperationalStateManagerTests {

	private static final String TEST_OP_MODE = "test-mode";
	private static final String TEST_CONTROL_ID_1 = "control.1";
	private static final String TEST_CONTROL_ID_2 = "control.2";
	private static final List<String> TEST_CONTROL_IDS = asList(TEST_CONTROL_ID_1, TEST_CONTROL_ID_2);

	private OperationalStateManager mgr;
	private OperationalModesService opModesService;
	private InstructionExecutionService instrService;

	@Before
	public void setup() {
		opModesService = EasyMock.createMock(OperationalModesService.class);
		instrService = EasyMock.createMock(InstructionExecutionService.class);
		mgr = new OperationalStateManager(
				new StaticOptionalService<OperationalModesService>(opModesService),
				new StaticOptionalService<InstructionExecutionService>(instrService));
		mgr.setMode(TEST_OP_MODE);
		mgr.setEnabledState(DeviceOperatingState.Shutdown);
		mgr.setDisabledState(DeviceOperatingState.Normal);
		mgr.setControlIds(new LinkedHashSet<>(TEST_CONTROL_IDS));
	}

	private void replayAll() {
		EasyMock.replay(instrService, opModesService);
	}

	private void resetAll() {
		EasyMock.reset(instrService, opModesService);
	}

	@After
	public void teardown() {
		EasyMock.verify(instrService, opModesService);
	}

	@Test
	public void enableMode() {
		// given
		Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		expect(instrService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return InstructionUtils.createStatus(
								instructionCaptor.getValues()
										.get(instructionCaptor.getValues().size() - 1),
								InstructionState.Completed);
					}
				}).times(2);

		// when
		replayAll();

		Map<String, Object> evtProps = singletonMap(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES,
				singleton(TEST_OP_MODE));
		Event evt = new Event(EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, evtProps);
		mgr.handleEvent(evt);

		// then
		assertThat("Mode active", mgr.isModeActive(), equalTo(true));

		List<Instruction> instrs = instructionCaptor.getValues();
		assertThat("Instructions submitted", instrs, hasSize(2));
		for ( int i = 0; i < instrs.size(); i++ ) {
			Instruction instr = instrs.get(i);
			String controlId = TEST_CONTROL_IDS.get(i);
			assertThat("Instruction topic " + i, instr.getTopic(),
					equalTo(InstructionHandler.TOPIC_SET_OPERATING_STATE));
			assertThat("Requested " + controlId + " state " + i, instr.getParameterValue(controlId),
					equalTo(String.valueOf(DeviceOperatingState.Shutdown.getCode())));
		}
	}

	@Test
	public void disableMode() {
		// given
		enableMode();
		resetAll();

		Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		expect(instrService.executeInstruction(capture(instructionCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						return InstructionUtils.createStatus(
								instructionCaptor.getValues()
										.get(instructionCaptor.getValues().size() - 1),
								InstructionState.Completed);
					}
				}).times(2);

		// when
		replayAll();

		Map<String, Object> evtProps = singletonMap(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES, emptySet());
		Event evt = new Event(EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, evtProps);
		mgr.handleEvent(evt);

		// then
		assertThat("Mode not active", mgr.isModeActive(), equalTo(false));

		List<Instruction> instrs = instructionCaptor.getValues();
		assertThat("Instructions submitted", instrs, hasSize(2));
		for ( int i = 0; i < instrs.size(); i++ ) {
			Instruction instr = instrs.get(i);
			String controlId = TEST_CONTROL_IDS.get(i);
			assertThat("Instruction topic " + i, instr.getTopic(),
					equalTo(InstructionHandler.TOPIC_SET_OPERATING_STATE));
			assertThat("Requested " + controlId + " state " + i, instr.getParameterValue(controlId),
					equalTo(String.valueOf(DeviceOperatingState.Normal.getCode())));
		}
	}

}
