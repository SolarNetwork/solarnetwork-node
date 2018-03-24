/* ==================================================================
 * ModbusTogglerTests.java - 16/03/2018 2:07:17 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus.toggle.test;

import static net.solarnetwork.node.reactor.Instruction.LOCAL_INSTRUCTION_ID;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.modbus.toggle.ModbusToggler;
import net.solarnetwork.node.io.modbus.AbstractModbusNetwork;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link ModbusToggler} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusTogglerTests {

	private static final int TEST_UNIT_ID = 3;
	private static final int TEST_ADDRESS = 12;
	private static final String TEST_CONTROL_ID = "test.control";
	private static final long TEST_CACHE_LONG_MS = TimeUnit.DAYS.toMillis(1);

	private ModbusConnection conn;
	private ModbusNetwork network;

	private ModbusToggler toggler;

	@Before
	public void setup() {
		conn = EasyMock.createMock(ModbusConnection.class);
		network = EasyMock.createMock(ModbusNetwork.class);

		toggler = new ModbusToggler();
		toggler.setUnitId(TEST_UNIT_ID);
		toggler.setAddress(TEST_ADDRESS);
		toggler.setControlId(TEST_CONTROL_ID);
		toggler.setSampleCacheMs(0);
		toggler.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(network));
	}

	@After
	public void teardown() {
		EasyMock.verify(conn, network);
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	private <T> IExpectationSetters<T> expectModbusAction(Class<T> type) {
		try {
			return expect(network.performAction(anyAction(type), eq(TEST_UNIT_ID)))
					.andDelegateTo(new AbstractModbusNetwork() {

						@Override
						public <A> A performAction(ModbusConnectionAction<A> action, int unitId)
								throws IOException {
							return action.doWithConnection(conn);
						}

						@Override
						public ModbusConnection createConnection(int unitId) {
							return conn;
						}

					});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private void replayAll() {
		EasyMock.replay(conn, network);
	}

	@Test
	public void availableControlIds() {
		// given

		// when
		replayAll();
		List<String> result = toggler.getAvailableControlIds();

		// then
		assertThat(result, contains(TEST_CONTROL_ID));
	}

	@Test
	public void handlesSetControlParameterInstructionTopic() {
		// given

		// when
		replayAll();
		boolean result = toggler.handlesTopic(TOPIC_SET_CONTROL_PARAMETER);

		// then
		assertThat("Handles SetControlParameter", result, equalTo(true));
	}

	@Test
	public void processSetControlParameterOn() {
		// given
		expectModbusAction(Boolean.class);

		BitSet writeBitSet = new BitSet();
		writeBitSet.set(0);
		expect(conn.writeDiscreetValues(aryEq(new Integer[] { TEST_ADDRESS }), eq(writeBitSet)))
				.andReturn(true);

		// when
		replayAll();
		BasicInstruction instr = new BasicInstruction(TOPIC_SET_CONTROL_PARAMETER, new Date(),
				LOCAL_INSTRUCTION_ID, LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "true");
		InstructionStatus.InstructionState result = toggler.processInstruction(instr);

		// then
		assertThat("Handled result", result, equalTo(InstructionStatus.InstructionState.Completed));
	}

	@Test
	public void processSetControlParameterOnHoldingRegister() {
		// given
		toggler.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		expectModbusAction(Boolean.class);

		conn.writeUnsignedShorts(eq(ModbusWriteFunction.WriteHoldingRegister), eq(TEST_ADDRESS),
				aryEq(new int[] { 1 }));

		// when
		replayAll();
		BasicInstruction instr = new BasicInstruction(TOPIC_SET_CONTROL_PARAMETER, new Date(),
				LOCAL_INSTRUCTION_ID, LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "true");
		InstructionStatus.InstructionState result = toggler.processInstruction(instr);

		// then
		assertThat("Handled result", result, equalTo(InstructionStatus.InstructionState.Completed));
	}

	@Test
	public void processSetControlParameterOff() {
		// given
		expectModbusAction(Boolean.class);

		BitSet writeBitSet = new BitSet();
		writeBitSet.set(0, false);
		expect(conn.writeDiscreetValues(aryEq(new Integer[] { TEST_ADDRESS }), eq(writeBitSet)))
				.andReturn(true);

		// when
		replayAll();
		BasicInstruction instr = new BasicInstruction(TOPIC_SET_CONTROL_PARAMETER, new Date(),
				LOCAL_INSTRUCTION_ID, LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "false");
		InstructionStatus.InstructionState result = toggler.processInstruction(instr);

		// then
		assertThat("Handled result", result, equalTo(InstructionStatus.InstructionState.Completed));
	}

	@Test
	public void processSetControlParameterOffHoldingRegister() {
		// given
		toggler.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		expectModbusAction(Boolean.class);

		conn.writeUnsignedShorts(eq(ModbusWriteFunction.WriteHoldingRegister), eq(TEST_ADDRESS),
				aryEq(new int[] { 0 }));

		// when
		replayAll();
		BasicInstruction instr = new BasicInstruction(TOPIC_SET_CONTROL_PARAMETER, new Date(),
				LOCAL_INSTRUCTION_ID, LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "false");
		InstructionStatus.InstructionState result = toggler.processInstruction(instr);

		// then
		assertThat("Handled result", result, equalTo(InstructionStatus.InstructionState.Completed));
	}

	@Test
	public void currentValueOn() {
		// given
		expectModbusAction(Boolean.class);

		BitSet bitSet = new BitSet();
		bitSet.set(0, true);
		expect(conn.readDiscreetValues(aryEq(new Integer[] { TEST_ADDRESS }), eq(1))).andReturn(bitSet);

		// when
		replayAll();
		NodeControlInfo result = toggler.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Current value provided", result, notNullValue());
		assertThat("Control ID", result.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Control prop name", result.getPropertyName(), nullValue());
		assertThat("Control readonly", result.getReadonly(), equalTo(false));
		assertThat("Control type", result.getType(), equalTo(NodeControlPropertyType.Boolean));
		assertThat("Control unit", result.getUnit(), nullValue());
		assertThat("Control value", result.getValue(), equalTo("true"));
	}

	@Test
	public void currentValueOnHoldingRegister() {
		// given
		toggler.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		expectModbusAction(Boolean.class);

		expect(conn.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, TEST_ADDRESS, 1))
				.andReturn(new int[] { 1 });

		// when
		replayAll();
		NodeControlInfo result = toggler.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Current value provided", result, notNullValue());
		assertThat("Control ID", result.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Control prop name", result.getPropertyName(), nullValue());
		assertThat("Control readonly", result.getReadonly(), equalTo(false));
		assertThat("Control type", result.getType(), equalTo(NodeControlPropertyType.Boolean));
		assertThat("Control unit", result.getUnit(), nullValue());
		assertThat("Control value", result.getValue(), equalTo("true"));
	}

	@Test
	public void currentValueOff() {
		// given
		expectModbusAction(Boolean.class);

		BitSet bitSet = new BitSet();
		bitSet.set(0, false);
		expect(conn.readDiscreetValues(aryEq(new Integer[] { TEST_ADDRESS }), eq(1))).andReturn(bitSet);

		// when
		replayAll();
		NodeControlInfo result = toggler.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Current value provided", result, notNullValue());
		assertThat("Control ID", result.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Control prop name", result.getPropertyName(), nullValue());
		assertThat("Control readonly", result.getReadonly(), equalTo(false));
		assertThat("Control type", result.getType(), equalTo(NodeControlPropertyType.Boolean));
		assertThat("Control unit", result.getUnit(), nullValue());
		assertThat("Control value", result.getValue(), equalTo("false"));
	}

	@Test
	public void currentValueOffHoldingRegister() {
		// given
		toggler.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		expectModbusAction(Boolean.class);

		expect(conn.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, TEST_ADDRESS, 1))
				.andReturn(new int[] { 0 });

		// when
		replayAll();
		NodeControlInfo result = toggler.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Current value provided", result, notNullValue());
		assertThat("Control ID", result.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Control prop name", result.getPropertyName(), nullValue());
		assertThat("Control readonly", result.getReadonly(), equalTo(false));
		assertThat("Control type", result.getType(), equalTo(NodeControlPropertyType.Boolean));
		assertThat("Control unit", result.getUnit(), nullValue());
		assertThat("Control value", result.getValue(), equalTo("false"));
	}

	@Test
	public void currentValueOnCached() {
		// given
		toggler.setSampleCacheMs(TEST_CACHE_LONG_MS);
		expectModbusAction(Boolean.class);

		BitSet bitSet = new BitSet();
		bitSet.set(0, true);
		expect(conn.readDiscreetValues(aryEq(new Integer[] { TEST_ADDRESS }), eq(1))).andReturn(bitSet);

		// when
		replayAll();
		NodeControlInfo result = toggler.getCurrentControlInfo(TEST_CONTROL_ID);
		NodeControlInfo cachedResult = toggler.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Current value provided", result, notNullValue());
		assertThat("Cached value provided", cachedResult, notNullValue());
		assertThat("Cached instance", cachedResult, sameInstance(result));
	}

	@Test
	public void currentValueOnCachedExpired() throws InterruptedException {
		// given
		toggler.setSampleCacheMs(300);
		expectModbusAction(Boolean.class).times(2);

		BitSet bitSet = new BitSet();
		bitSet.set(0, true);
		expect(conn.readDiscreetValues(aryEq(new Integer[] { TEST_ADDRESS }), eq(1))).andReturn(bitSet)
				.times(2);

		// when
		replayAll();
		NodeControlInfo result = toggler.getCurrentControlInfo(TEST_CONTROL_ID);
		NodeControlInfo cachedResult = toggler.getCurrentControlInfo(TEST_CONTROL_ID);
		Thread.sleep(300L);
		NodeControlInfo cacheExpiredResult = toggler.getCurrentControlInfo(TEST_CONTROL_ID);

		// then
		assertThat("Current value provided", result, notNullValue());
		assertThat("Cached value provided", cachedResult, notNullValue());
		assertThat("Cache expired value provided", cacheExpiredResult, notNullValue());
		assertThat("Cached instance", cachedResult, sameInstance(result));
		assertThat("Non-cached instance", cacheExpiredResult, not(sameInstance(result)));
	}

	@Test
	public void configureFunctionAsName() {
		// when
		replayAll();
		toggler.setFunctionCode(ModbusWriteFunction.WriteHoldingRegister.name());

		// then
		assertThat(toggler.getFunction(), equalTo(ModbusWriteFunction.WriteHoldingRegister));
	}

}
