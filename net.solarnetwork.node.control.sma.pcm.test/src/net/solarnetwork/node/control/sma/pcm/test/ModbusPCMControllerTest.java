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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.sma.pcm.ModbusPCMController;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.util.StaticOptionalService;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link ModbusPCMController} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ModbusPCMControllerTest {

	private final String TEST_CONTROL_ID = "/power/pcm/test";

	private final int UNIT_ID = 1;

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private ModbusPCMController service;

	@Before
	public void setup() {
		service = new ModbusPCMController();
		service.setControlId(TEST_CONTROL_ID);
		service.setUnitId(1);
		service.setD1Address(1);
		service.setD2Address(2);
		service.setD3Address(3);
		service.setD4Address(4);

		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	@Test
	public void handleDemandBalance50Percent() throws IOException {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_DEMAND_BALANCE,
				new Date(), Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(TEST_CONTROL_ID, "50");

		expect(modbus.performAction(anyAction(Boolean.class), EasyMock.eq(UNIT_ID))).andDelegateTo(
				new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(ModbusConnectionAction<T> action, int unitId)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		expect(
				conn.writeDiscreetValues(EasyMock.aryEq(new Integer[] { 1, 2, 3, 4 }),
						EasyMock.eq(expectedBitSet))).andReturn(Boolean.TRUE);

		replay(modbus, conn);

		InstructionStatus.InstructionState state = service.processInstruction(instr);

		verify(modbus, conn);

		Assert.assertEquals("Instruction should be processed",
				InstructionStatus.InstructionState.Completed, state);
	}

	@Test
	public void exposesPercentControl() {
		Assert.assertEquals(
				"Percent control included in supported control IDs",
				Arrays.asList(TEST_CONTROL_ID, TEST_CONTROL_ID
						+ ModbusPCMController.PERCENT_CONTROL_ID_SUFFIX),
				service.getAvailableControlIds());
	}

	@Test
	public void readControlInfo() throws IOException {
		expect(modbus.performAction(anyAction(BitSet.class), EasyMock.eq(UNIT_ID))).andDelegateTo(
				new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(ModbusConnectionAction<T> action, int unitId)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		expect(conn.readDiscreetValues(EasyMock.aryEq(new Integer[] { 1, 2, 3, 4 }), EasyMock.eq(1)))
				.andReturn(expectedBitSet);

		replay(modbus, conn);

		NodeControlInfo info = service.getCurrentControlInfo(TEST_CONTROL_ID);

		verify(modbus, conn);

		Assert.assertEquals("Read control ID", TEST_CONTROL_ID, info.getControlId());
		Assert.assertEquals("Read value type", NodeControlPropertyType.Integer, info.getType());
		Assert.assertEquals("Read value", String.valueOf(8), info.getValue());
	}

	@Test
	public void readControlInfoAsPercent() throws IOException {
		expect(modbus.performAction(anyAction(BitSet.class), EasyMock.eq(UNIT_ID))).andDelegateTo(
				new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(ModbusConnectionAction<T> action, int unitId)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});
		BitSet expectedBitSet = new BitSet();
		expectedBitSet.set(3, true); // binary 8 == 50%
		expect(conn.readDiscreetValues(EasyMock.aryEq(new Integer[] { 1, 2, 3, 4 }), EasyMock.eq(1)))
				.andReturn(expectedBitSet);

		replay(modbus, conn);

		NodeControlInfo info = service.getCurrentControlInfo(TEST_CONTROL_ID
				+ ModbusPCMController.PERCENT_CONTROL_ID_SUFFIX);

		verify(modbus, conn);

		Assert.assertEquals("Read control ID", TEST_CONTROL_ID
				+ ModbusPCMController.PERCENT_CONTROL_ID_SUFFIX, info.getControlId());
		Assert.assertEquals("Read value type", NodeControlPropertyType.Integer, info.getType());
		Assert.assertEquals("Read value", String.valueOf(50), info.getValue());
	}

}
