/* ==================================================================
 * BaseModelAccessorTests.java - 5/10/2026 6:57:32 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.test;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Accumulator;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.math.BigInteger;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.GenericModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelId;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Test cases for the {@link BaseModelAccessor} class.
 *
 * @author matt
 * @version 1.0
 */
public class BaseModelAccessorTests {

	/** Test registers, relative to the model block address. */
	private enum TestRegister implements SunspecModbusReference {

		UInt32Value(0, UInt32, null),

		Enum32Value(2, UInt32, Enumeration),

		Acc32Value(4, UInt32, Accumulator),

		UInt64Value(6, UInt64, null),

		Acc16Value(10, UInt16, Accumulator),

		Acc64Value(11, UInt64, Accumulator),

		;

		private final int address;
		private final ModbusDataType dataType;
		private final @Nullable DataClassification classification;

		private TestRegister(int address, ModbusDataType dataType,
				@Nullable DataClassification classification) {
			this.address = address;
			this.dataType = dataType;
			this.classification = classification;
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
			return dataType.getWordLength();
		}

		@Override
		public @Nullable DataClassification getClassification() {
			return classification;
		}

	}

	private ModelData data;
	private BaseModelAccessor accessor;

	@Before
	public void setup() {
		data = new ModelData(0);
		accessor = new GenericModelAccessor(data, 0, new GenericModelId(64000));
	}

	private void saveRegisters(TestRegister ref, int... words) throws IOException {
		data.performUpdates(m -> {
			m.saveDataArray(words, accessor.getBlockAddress() + ref.getAddress());
			return true;
		});
	}

	@Test
	public void uint32_notImplemented() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.UInt32Value, 0xFFFF, 0xFFFF);

		// WHEN
		Number result = accessor.getValue(TestRegister.UInt32Value);

		// THEN
		assertThat("uint32 0xFFFFFFFF is not implemented", result, is(nullValue()));
	}

	@Test
	public void uint32_maximum() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.UInt32Value, 0xFFFF, 0xFFFE);

		// WHEN
		Number result = accessor.getValue(TestRegister.UInt32Value);

		// THEN
		assertThat("uint32 0xFFFFFFFE is a value", result, is(equalTo((Number) 0xFFFFFFFEL)));
	}

	@Test
	public void enum32_notImplemented() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Enum32Value, 0xFFFF, 0xFFFF);

		// WHEN
		Number result = accessor.getValue(TestRegister.Enum32Value);

		// THEN
		assertThat("enum32 0xFFFFFFFF is not implemented", result, is(nullValue()));
	}

	@Test
	public void acc32_notAccumulated() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Acc32Value, 0x0000, 0x0000);

		// WHEN
		Number result = accessor.getValue(TestRegister.Acc32Value);

		// THEN
		assertThat("acc32 0 is not accumulated", result, is(nullValue()));
	}

	@Test
	public void acc32_maximum() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Acc32Value, 0xFFFF, 0xFFFF);

		// WHEN
		Number result = accessor.getValue(TestRegister.Acc32Value);

		// THEN
		assertThat("acc32 0xFFFFFFFF is a value", result, is(equalTo((Number) 0xFFFFFFFFL)));
	}

	@Test
	public void acc16_maximum() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Acc16Value, 0xFFFF);

		// WHEN
		Number result = accessor.getValue(TestRegister.Acc16Value);

		// THEN
		assertThat("acc16 0xFFFF is a value", result, is(equalTo((Number) 0xFFFF)));
	}

	@Test
	public void acc64_notAccumulated() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Acc64Value, 0x0000, 0x0000, 0x0000, 0x0000);

		// WHEN
		Number result = accessor.getValue(TestRegister.Acc64Value);

		// THEN
		assertThat("acc64 0 is not accumulated", result, is(nullValue()));
	}

	@Test
	public void acc64_lowWordsZero() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Acc64Value, 0x0000, 0x0001, 0x0000, 0x0000);

		// WHEN
		Number result = accessor.getValue(TestRegister.Acc64Value);

		// THEN
		assertThat("acc64 0x100000000 is a value", result,
				is(equalTo((Number) new BigInteger("100000000", 16))));
	}

	@Test
	public void acc64_maximum() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Acc64Value, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF);

		// WHEN
		Number result = accessor.getValue(TestRegister.Acc64Value);

		// THEN
		assertThat("acc64 0xFFFFFFFFFFFFFFFF is a value", result,
				is(equalTo((Number) new BigInteger("FFFFFFFFFFFFFFFF", 16))));
	}

	@Test
	public void uint64_notImplemented() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.UInt64Value, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF);

		// WHEN
		Number result = accessor.getValue(TestRegister.UInt64Value);

		// THEN
		assertThat("uint64 0xFFFFFFFFFFFFFFFF is not implemented", result, is(nullValue()));
	}

	@Test
	public void uint64_maximum() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.UInt64Value, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFE);

		// WHEN
		Number result = accessor.getValue(TestRegister.UInt64Value);

		// THEN
		assertThat("uint64 0xFFFFFFFFFFFFFFFE is a value", result,
				is(equalTo((Number) new BigInteger("FFFFFFFFFFFFFFFE", 16))));
	}

}
