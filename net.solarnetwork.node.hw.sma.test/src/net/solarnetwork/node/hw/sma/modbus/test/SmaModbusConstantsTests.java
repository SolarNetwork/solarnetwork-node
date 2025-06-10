/* ==================================================================
 * SmaModbusConstantsTests.java - 15/09/2020 7:50:03 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus.test;

import static net.solarnetwork.node.hw.sma.modbus.SmaModbusConstants.isNaN;
import static net.solarnetwork.node.hw.sma.modbus.test.TestRegisters.Int16Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.TestRegisters.Int32Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.TestRegisters.UInt16Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.TestRegisters.UInt32Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.TestRegisters.UInt64Reg;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.math.BigInteger;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.modbus.SmaModbusConstants;
import net.solarnetwork.node.hw.sma.test.TestUtils;
import net.solarnetwork.node.io.modbus.ModbusData;

/**
 * Test cases for the {@link SmaModbusConstants} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaModbusConstantsTests {

	@Test
	public void nan() {
		ModbusData d = TestUtils.testData(getClass(), "test-data-01.txt");

		Number n = d.getNumber(Int16Reg);
		assertThat("Int16 value", n, equalTo((short) 0x8000));
		assertThat("Int16 NaN", isNaN(n, Int16), equalTo(true));

		n = d.getNumber(UInt16Reg);
		assertThat("UInt16 value", n, equalTo(0xFFFF));
		assertThat("UInt16 NaN", isNaN(n, UInt16), equalTo(true));

		n = d.getNumber(Int32Reg);
		assertThat("Int32 value", n, equalTo(0x80000000));
		assertThat("Int32 NaN", isNaN(n, Int32), equalTo(true));

		n = d.getNumber(UInt32Reg);
		assertThat("UInt32 value", n, equalTo(0xFFFFFFFFl));
		assertThat("UInt32 NaN", isNaN(n, UInt32), equalTo(true));

		n = d.getNumber(UInt64Reg);
		assertThat("UInt64 value", n, equalTo(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
		assertThat("UInt64 NaN", isNaN(n, UInt64), equalTo(true));
	}

	@Test
	public void not_nan() {
		ModbusData d = TestUtils.testData(getClass(), "test-data-02.txt");

		Number n = d.getNumber(Int16Reg);
		assertThat("Int16 value", n, equalTo((short) 0x1234));
		assertThat("Int16 not NaN", isNaN(n, Int16), equalTo(false));

		n = d.getNumber(UInt16Reg);
		assertThat("UInt16 value", n, equalTo(0xABCD));
		assertThat("UInt16 not NaN", isNaN(n, UInt16), equalTo(false));

		n = d.getNumber(Int32Reg);
		assertThat("Int32 value", n, equalTo(0x2345));
		assertThat("Int32 not NaN", isNaN(n, Int32), equalTo(false));

		n = d.getNumber(UInt32Reg);
		assertThat("UInt32 value", n, equalTo(0xEFFFFFFFl));
		assertThat("UInt32 not NaN", isNaN(n, UInt32), equalTo(false));

		n = d.getNumber(UInt64Reg);
		assertThat("UInt64 value", n, equalTo(new BigInteger("FFFFFFFFFFFF1234", 16)));
		assertThat("UInt64 not NaN", isNaN(n, UInt64), equalTo(false));
	}
}
