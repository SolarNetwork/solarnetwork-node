/* ==================================================================
 * ModbusPCMControllerTest.java - Mar 23, 2014 1:23:09 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.sma.pcm.test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.sma.pcm.ModbusPCMController;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Unit tests for the {@link ModbusPCMController} class.
 * 
 * @author matt
 * @version 3.0
 */
public class ModbusPCMControllerTest {

	private static final String TEST_CONTROL_ID = "/power/pcm/test";
	private static final int[] DEFAULT_PCM_ADDRESSES = new int[] { 0, 1, 2, 3 };

	private final int UNIT_ID = 1;

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private ModbusPCMController service;

	@Before
	public void setup() {
		service = new ModbusPCMController();
		service.setControlId(TEST_CONTROL_ID);
		service.setUnitId(UNIT_ID);
		service.setD1Address(0);
		service.setD2Address(1);
		service.setD3Address(2);
		service.setD4Address(3);

		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		service.setModbusNetwork(new StaticOptionalService<>(modbus));
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	@Test
	public void handleDemandBalance50Percent() throws IOException {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_DEMAND_BALANCE,
				Instant.now(), Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "50");

		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(Boolean.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		conn.writeDiscreetValues(aryEq(DEFAULT_PCM_ADDRESSES), eq(expectedBitSet));

		replay(modbus, conn);

		InstructionStatus status = service.processInstruction(instr);

		verify(modbus, conn);

		assertThat("Instruction should be processed", status.getInstructionState(),
				is(InstructionState.Completed));
	}

	@Test
	public void exposesPercentControl() {
		Assert.assertEquals("Percent control included in supported control IDs",
				Arrays.asList(TEST_CONTROL_ID,
						TEST_CONTROL_ID + ModbusPCMController.PERCENT_CONTROL_ID_SUFFIX),
				service.getAvailableControlIds());
	}

	@Test
	public void readControlInfo() throws IOException {
		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(BitSet.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		expect(conn.readDiscreetValues(aryEq(DEFAULT_PCM_ADDRESSES), eq(1))).andReturn(expectedBitSet);

		replay(modbus, conn);

		NodeControlInfo info = service.getCurrentControlInfo(TEST_CONTROL_ID);

		verify(modbus, conn);

		assertThat("Read control ID", info.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Read value type", info.getType(), equalTo(NodeControlPropertyType.Integer));
		assertThat("Read value", info.getValue(), equalTo(String.valueOf(8)));
	}

	@Test
	public void readControlInfoAsPercent() throws IOException {
		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(BitSet.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		expect(conn.readDiscreetValues(aryEq(DEFAULT_PCM_ADDRESSES), eq(1))).andReturn(expectedBitSet);

		replay(modbus, conn);

		NodeControlInfo info = service
				.getCurrentControlInfo(TEST_CONTROL_ID + ModbusPCMController.PERCENT_CONTROL_ID_SUFFIX);

		verify(modbus, conn);

		Assert.assertEquals("Read control ID",
				TEST_CONTROL_ID + ModbusPCMController.PERCENT_CONTROL_ID_SUFFIX, info.getControlId());
		Assert.assertEquals("Read value type", NodeControlPropertyType.Integer, info.getType());
		Assert.assertEquals("Read value", String.valueOf(50), info.getValue());
	}

	@Test
	public void readControlInfoWithDefaultAddresses() throws IOException {
		final ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(
				new int[] { 0, 0, 0, 0, 0, 0, 1, 0 }, 0x4000);
		ModbusPCMController service = new ModbusPCMController();
		service.setControlId(TEST_CONTROL_ID);
		service.setUnitId(UNIT_ID);
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
		expect(modbus.performAction(eq(UNIT_ID), anyAction(BitSet.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%

		replay(modbus);

		NodeControlInfo info = service.getCurrentControlInfo(TEST_CONTROL_ID);

		verify(modbus);

		assertThat("Read control ID", info.getControlId(), equalTo(TEST_CONTROL_ID));
		assertThat("Read value type", info.getType(), equalTo(NodeControlPropertyType.Integer));
		assertThat("Read value", info.getValue(), equalTo(String.valueOf(8)));
	}

	@Test
	public void handleDemandBalanceInstructionWithDefaultAddresses() throws IOException {
		ModbusPCMController service = new ModbusPCMController();
		service.setControlId(TEST_CONTROL_ID);
		service.setUnitId(UNIT_ID);
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
		expect(modbus.performAction(eq(UNIT_ID), anyAction(Boolean.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});

		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		conn.writeDiscreetValues(aryEq(new int[] { 0x4000, 0x4002, 0x4004, 0x4006 }),
				eq(expectedBitSet));

		replay(modbus, conn);

		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_DEMAND_BALANCE,
				Instant.now(), null, null);
		instr.addParameter(TEST_CONTROL_ID, "50"); // request 50%

		InstructionStatus result = service.processInstruction(instr);

		verify(modbus, conn);

		assertThat("Result", result.getInstructionState(), is(InstructionState.Completed));
	}
}
