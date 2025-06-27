/* ==================================================================
 * OperationalModeSwitchTests.java - 16/05/2019 5:43:04 am
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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static net.solarnetwork.node.service.NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED;
import static net.solarnetwork.node.service.NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED;
import static net.solarnetwork.node.service.OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES;
import static net.solarnetwork.node.service.OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.node.control.opmode.OperationalModeSwitch;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link OperationalModeSwitch} class.
 * 
 * @author matt
 * @version 2.0
 */
public class OperationalModeSwitchTests {

	private static final String TEST_OP_MODE = "test-mode";
	private static final String TEST_CONTROL_ID = "control.1";

	private OperationalModesService opModesService;
	private EventAdmin eventAdmin;
	private OperationalModeSwitch ctl;

	@Before
	public void setup() {
		opModesService = EasyMock.createMock(OperationalModesService.class);
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		ctl = new OperationalModeSwitch(
				new StaticOptionalService<OperationalModesService>(opModesService));
		ctl.setMode(TEST_OP_MODE);
		ctl.setControlId(TEST_CONTROL_ID);
		ctl.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, opModesService);
	}

	private void resetAll() {
		EasyMock.reset(eventAdmin, opModesService);
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin, opModesService);
	}

	@Test
	public void supportedControlIds() {
		replayAll();
		assertThat("Control IDs", ctl.getAvailableControlIds(), contains(TEST_CONTROL_ID));
	}

	@Test
	public void currentInfoWrongControl() {
		replayAll();
		assertThat("No result", ctl.getCurrentControlInfo("not.known"), nullValue());
	}

	@Test
	public void currentInfo() {
		// given
		expect(opModesService.isOperationalModeActive(TEST_OP_MODE)).andReturn(false);

		Capture<Event> eventCaptor = Capture.newInstance();
		eventAdmin.postEvent(capture(eventCaptor));

		// when
		replayAll();
		NodeControlInfo info = ctl.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Info available", info, notNullValue());
		assertThat("Control ID", info.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Prop name", info.getPropertyName(), nullValue());
		assertThat("Read only", info.getReadonly(), equalTo(false));
		assertThat("Type", info.getType(), equalTo(NodeControlPropertyType.Boolean));
		assertThat("Unit", info.getUnit(), nullValue());
		assertThat("Value", info.getValue(), equalTo(Boolean.FALSE.toString()));

		Event ctlCapturedEvent = eventCaptor.getValue();
		verifyControlEvent(ctlCapturedEvent, EVENT_TOPIC_CONTROL_INFO_CAPTURED, false);
	}

	private void verifyControlEvent(Event ctlCapturedEvent, String topic, boolean value) {
		assertThat("Control captured event posted", ctlCapturedEvent, notNullValue());
		assertThat("Event topic", ctlCapturedEvent.getTopic(), equalTo(topic));
		assertThat("Event params", ctlCapturedEvent.getPropertyNames(),
				arrayContainingInAnyOrder(DatumEvents.DATUM_PROPERTY, Datum.DATUM_TYPE_PROPERTY,
						Datum.DATUM_TYPES_PROPERTY, "event.topics"));

		assertThat("Event datum types",
				(String[]) ctlCapturedEvent.getProperty(Datum.DATUM_TYPES_PROPERTY), arrayContaining(
						NodeDatum.class.getName(), net.solarnetwork.domain.datum.Datum.class.getName()));
		assertThat("Event datum type", ctlCapturedEvent.getProperty(Datum.DATUM_TYPE_PROPERTY),
				equalTo(NodeDatum.class.getName()));

		Object o = ctlCapturedEvent.getProperty(DatumEvents.DATUM_PROPERTY);
		assertThat("Event datum is a NodeControlInfo and Datum", o,
				allOf(instanceOf(Datum.class), instanceOf(NodeControlInfo.class)));
		NodeControlInfo info = (NodeControlInfo) o;
		assertThat("Event control ID", info.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Event readonly", info.getReadonly(), equalTo(Boolean.FALSE));
		assertThat("Event type", info.getType(), equalTo(NodeControlPropertyType.Boolean));
		assertThat("Event value", info.getValue(), equalTo(String.valueOf(value)));

		assertThat("Event source ID", ((Datum) o).getSourceId(), equalTo(TEST_CONTROL_ID));
	}

	@Test
	public void handlesSetControlParam() {
		replayAll();
		assertThat("Handles SetControlParameter",
				ctl.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER), equalTo(true));
	}

	@Test
	public void setControlParamOn() {
		// given
		Set<String> modeSet = singleton(TEST_OP_MODE);
		expect(opModesService.enableOperationalModes(modeSet)).andReturn(modeSet);

		// when
		replayAll();
		Instruction instr = InstructionUtils.createSetControlValueLocalInstruction(TEST_CONTROL_ID,
				Boolean.TRUE.toString());
		InstructionStatus status = ctl.processInstruction(instr);

		// then
		assertThat("Completed", status.getInstructionState(), equalTo(InstructionState.Completed));
	}

	@Test
	public void setControlParamOff() {
		// given
		Set<String> modeSet = singleton(TEST_OP_MODE);
		expect(opModesService.disableOperationalModes(modeSet)).andReturn(emptySet());

		// when
		replayAll();
		Instruction instr = InstructionUtils.createSetControlValueLocalInstruction(TEST_CONTROL_ID,
				Boolean.FALSE.toString());
		InstructionStatus status = ctl.processInstruction(instr);

		// then
		assertThat("Completed", status.getInstructionState(), equalTo(InstructionState.Completed));
	}

	@Test
	public void eventOpModeAdded() {
		// given
		Capture<Event> eventCaptor = Capture.newInstance();
		eventAdmin.postEvent(capture(eventCaptor));

		// when
		replayAll();
		Set<String> modeSet = singleton(TEST_OP_MODE);
		Map<String, Object> params = Collections.singletonMap(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES,
				modeSet);
		Event evt = new Event(EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, params);
		ctl.handleEvent(evt);

		// then
		Event ctlCapturedEvent = eventCaptor.getValue();
		verifyControlEvent(ctlCapturedEvent, EVENT_TOPIC_CONTROL_INFO_CHANGED, true);
	}

	@Test
	public void eventOpModeRemoved() {
		// given
		eventOpModeAdded();
		resetAll();

		Capture<Event> eventCaptor = Capture.newInstance();
		eventAdmin.postEvent(capture(eventCaptor));

		// when
		replayAll();
		Map<String, Object> params = Collections.singletonMap(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES,
				emptySet());
		Event evt = new Event(EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, params);
		ctl.handleEvent(evt);

		// then
		Event ctlCapturedEvent = eventCaptor.getValue();
		verifyControlEvent(ctlCapturedEvent, EVENT_TOPIC_CONTROL_INFO_CHANGED, false);
	}
}
