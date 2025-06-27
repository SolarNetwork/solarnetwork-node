/* ==================================================================
 * ModbusControlTests.java - 9/06/2022 3:04:50 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus.test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Instant;
import java.util.BitSet;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.modbus.ModbusControl;
import net.solarnetwork.node.control.modbus.ModbusWritePropertyConfig;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link ModbusControl} class.
 *
 * @author matt
 * @version 1.1
 */
public class ModbusControlTests {

	private static final String TEST_CONTROL_ID = "/test/control/1";

	private final int UNIT_ID = 1;

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private ModbusControl control;

	@Before
	public void setup() {
		control = new ModbusControl();
		control.setUnitId(UNIT_ID);

		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		control.setModbusNetwork(new StaticOptionalService<>(modbus));
	}

	@After
	public void teardown() {
		EasyMock.verify(modbus, conn);
	}

	private void replayAll() {
		EasyMock.replay(modbus, conn);
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	@Test
	public void writeCoil_boolean_true() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Boolean, ModbusDataType.Boolean, 123);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });

		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				Instant.now(), Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "true");

		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(Boolean.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(0, true);
		conn.writeDiscreteValues(eq(ModbusWriteFunction.WriteCoil), eq(config.getAddress()), eq(1),
				eq(expectedBitSet));

		// WHEN
		replayAll();
		InstructionStatus status = control.processInstruction(instr);

		// THEN
		assertThat("Instruction should be processed", status.getInstructionState(),
				is(InstructionState.Completed));
	}

	@Test
	public void writeCoil_boolean_false() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Boolean, ModbusDataType.Boolean, 123);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });

		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				Instant.now(), Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "false");

		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(Boolean.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		conn.writeDiscreteValues(eq(ModbusWriteFunction.WriteCoil), eq(config.getAddress()), eq(1),
				eq(expectedBitSet));

		// WHEN
		replayAll();
		InstructionStatus status = control.processInstruction(instr);

		// THEN
		assertThat("Instruction should be processed", status.getInstructionState(),
				is(InstructionState.Completed));
	}

	@Test
	public void readCoil_boolean_true() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Boolean, ModbusDataType.Boolean, 123);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });

		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet resultBitSet = new BitSet();
		resultBitSet.set(0, true);
		expect(conn.readDiscreteValues(config.getAddress(), 1)).andReturn(resultBitSet);

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(TEST_CONTROL_ID);

		// THEN
		assertThat("Info should be returned", info, is(notNullValue()));
		assertThat("Value should be 'true'", info.getValue(), is(equalTo("true")));
	}

	@Test
	public void readCoil_boolean_false() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Boolean, ModbusDataType.Boolean, 123);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });

		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet resultBitSet = new BitSet();
		expect(conn.readDiscreteValues(config.getAddress(), 1)).andReturn(resultBitSet);

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(TEST_CONTROL_ID);

		// THEN
		assertThat("Info should be returned", info, is(notNullValue()));
		assertThat("Value should be 'false'", info.getValue(), is(equalTo("false")));
	}

	@Test
	public void writeHolding_float_leastToMostWordOrder() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Float, ModbusDataType.Float32, 123);
		config.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });
		control.setWordOrder(ModbusWordOrder.LeastToMostSignificant);

		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				Instant.now(), Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "45000.0");

		expect(modbus.performAction(eq(UNIT_ID), anyAction(Boolean.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		conn.writeWords(eq(config.getFunction()), eq(config.getAddress()),
				aryEq(new short[] { (short) 0xC800, (short) 0x472F }));

		// WHEN
		replayAll();
		InstructionStatus status = control.processInstruction(instr);

		// THEN
		assertThat("Instruction should be processed", status.getInstructionState(),
				is(InstructionState.Completed));
	}

	@Test
	public void writeHolding_float_mostToLeastWordOrder() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Float, ModbusDataType.Float32, 123);
		config.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });
		control.setWordOrder(ModbusWordOrder.MostToLeastSignificant);

		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				Instant.now(), Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "45000.0");

		expect(modbus.performAction(eq(UNIT_ID), anyAction(Boolean.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		conn.writeWords(eq(config.getFunction()), eq(config.getAddress()),
				aryEq(new short[] { (short) 0x472F, (short) 0xC800 }));

		// WHEN
		replayAll();
		InstructionStatus status = control.processInstruction(instr);

		// THEN
		assertThat("Instruction should be processed", status.getInstructionState(),
				is(InstructionState.Completed));
	}

	@Test
	public void readHolding_float_mostToLeastWordOrder() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Float, ModbusDataType.Float32, 123);
		config.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });
		control.setWordOrder(ModbusWordOrder.MostToLeastSignificant);

		expect(modbus.performAction(eq(UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		final short[] data = new short[] { (short) 0x472F, (short) 0xC800 };
		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, config.getAddress(), 2))
				.andReturn(data);

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(TEST_CONTROL_ID);

		// THEN
		assertThat("Info should be returned", info, is(notNullValue()));
		assertThat("Value should be '45000'", info.getValue(), is(equalTo("45000")));
	}

	@Test
	public void readHolding_float_leastToMostWordOrder() throws IOException {
		// GIVEN
		ModbusWritePropertyConfig config = new ModbusWritePropertyConfig(TEST_CONTROL_ID,
				NodeControlPropertyType.Float, ModbusDataType.Float32, 123);
		config.setFunction(ModbusWriteFunction.WriteHoldingRegister);
		control.setPropConfigs(new ModbusWritePropertyConfig[] { config });
		control.setWordOrder(ModbusWordOrder.LeastToMostSignificant);

		expect(modbus.performAction(eq(UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		final short[] data = new short[] { (short) 0xC800, (short) 0x472F };
		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, config.getAddress(), 2))
				.andReturn(data);

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(TEST_CONTROL_ID);

		// THEN
		assertThat("Info should be returned", info, is(notNullValue()));
		assertThat("Value should be '45000'", info.getValue(), is(equalTo("45000")));
	}

}
