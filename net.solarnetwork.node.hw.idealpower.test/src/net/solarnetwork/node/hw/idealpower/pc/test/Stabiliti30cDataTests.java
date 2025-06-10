/* ==================================================================
 * Stabiliti30cDataTests.java - 30/08/2019 11:51:47 am
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

package net.solarnetwork.node.hw.idealpower.pc.test;

import static net.solarnetwork.node.io.modbus.ModbusWriteFunction.WriteHoldingRegister;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cAcControlMethod;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cControlAccessor;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cData;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDcControlMethod;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link Stabiliti30cData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class Stabiliti30cDataTests {

	private ModbusConnection conn;

	@Before
	public void setup() {
		conn = EasyMock.createMock(ModbusConnection.class);
	}

	private void replayAll() {
		EasyMock.replay(conn);
	}

	@After
	public void teardown() {
		EasyMock.verify(conn);
	}

	@Test
	public void pvFirmingAndDemandChargeManagementExample() throws IOException {
		// GIVEN
		conn.writeWords(eq(WriteHoldingRegister), eq(129), aryEq(new int[] { 0x0001 }));
		conn.writeWords(eq(WriteHoldingRegister), eq(65), aryEq(new int[] { 0x0402 }));
		conn.writeWords(eq(WriteHoldingRegister), eq(193), aryEq(new int[] { 0x0002 }));
		conn.writeWords(eq(WriteHoldingRegister), eq(68), aryEq(new int[] { 0x0000 }));
		conn.writeWords(eq(WriteHoldingRegister), eq(199), aryEq(new int[] { 0x0000 }));
		conn.writeWords(eq(WriteHoldingRegister), eq(200), aryEq(new int[] { 1440 }));
		conn.writeWords(eq(WriteHoldingRegister), eq(263), aryEq(new int[] { 1 }));

		// WHEN
		replayAll();

		Stabiliti30cData data = new Stabiliti30cData();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				Stabiliti30cControlAccessor acc = data.controlAccessor(conn, m);
				acc.setP2ControlMethod(Stabiliti30cDcControlMethod.Net);
				acc.setP1ControlMethod(Stabiliti30cAcControlMethod.GridPower);
				acc.setP3ControlMethod(Stabiliti30cDcControlMethod.Mppt);
				acc.setP1ActivePowerSetpoint(0);
				acc.setP3MpptStartTimeOffsetSetpoint(0);
				acc.setP3MpptStopTimeOffsetSetpoint(1440);
				acc.setManualModeEnabled(true);
				return false;
			}
		});

		// THEN
		assertThat("P2 control method state updated", data.getP2ControlMethod(),
				equalTo(Stabiliti30cDcControlMethod.Net));
		assertThat("P1 control method state updated", data.getP1ControlMethod(),
				equalTo(Stabiliti30cAcControlMethod.GridPower));
		assertThat("P3 control method state updated", data.getP3ControlMethod(),
				equalTo(Stabiliti30cDcControlMethod.Mppt));
		assertThat("P1 active power setpoint state updated", data.getP1ActivePowerSetpoint(),
				equalTo(0));
		assertThat("P3 MPPT start time offset state updated", data.getP3MpptStartTimeOffsetSetpoint(),
				equalTo(0));
		assertThat("P3 MPPT stop time offset state updated", data.getP3MpptStopTimeOffsetSetpoint(),
				equalTo(1440));
		assertThat("Manual mode enabled", data.isManualModeEnabled(), equalTo(true));
	}

}
