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
import static net.solarnetwork.node.hw.sma.modbus.test.SmaModbusConstantsTests.TestRegisters.Int16Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.SmaModbusConstantsTests.TestRegisters.Int32Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.SmaModbusConstantsTests.TestRegisters.UInt16Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.SmaModbusConstantsTests.TestRegisters.UInt32Reg;
import static net.solarnetwork.node.hw.sma.modbus.test.SmaModbusConstantsTests.TestRegisters.UInt64Reg;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.math.BigInteger;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.modbus.SmaModbusConstants;
import net.solarnetwork.node.hw.sma.test.TestUtils;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Test cases for the {@link SmaModbusConstants} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaModbusConstantsTests {

	public enum TestRegisters implements ModbusReference {

		Int16Reg(0, Int16),

		UInt16Reg(1, UInt16),

		Int32Reg(2, Int32),

		UInt32Reg(4, UInt32),

		UInt64Reg(6, UInt64),

		;

		private final int address;
		private final ModbusDataType dataType;
		private final int wordLength;

		private TestRegisters(int address, ModbusDataType dataType) {
			this(address, dataType, dataType.getWordLength());
		}

		private TestRegisters(int address, ModbusDataType dataType, int wordLength) {
			this.address = address;
			this.dataType = dataType;
			this.wordLength = wordLength;
		}

		@Override
		public int getAddress() {
			return address;
		}

		@Override
		public ModbusDataType getDataType() {
			return dataType;
		}

		@Override
		public ModbusReadFunction getFunction() {
			return ModbusReadFunction.ReadHoldingRegister;
		}

		@Override
		public int getWordLength() {
			return wordLength;
		}
	}

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
