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
import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.GenericModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelId;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.hw.sunspec.der.DerAcWiringType;
import net.solarnetwork.node.hw.sunspec.der.DerOperationalCharacteristic;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.support.StaticDataMapModbusConnection;
import net.solarnetwork.util.IntShortMap;

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

		ScaleFactorValue(15, Int16, ScaleFactor),

		RwUInt16Value(16, UInt16, null, ReadWrite),

		RwInt16Value(17, Int16, null, ReadWrite),

		RwUInt32Value(18, UInt32, null, ReadWrite),

		RwInt32Value(20, Int32, null, ReadWrite),

		RwBitfield16Value(22, UInt16, Bitfield, ReadWrite),

		Enum16Value(23, UInt16, Enumeration),

		Bitfield16Value(24, UInt16, Bitfield),

		Bitfield32Value(25, UInt32, Bitfield),

		;

		private final int address;
		private final ModbusDataType dataType;
		private final @Nullable DataClassification classification;
		private final PointAccess access;

		private TestRegister(int address, ModbusDataType dataType,
				@Nullable DataClassification classification) {
			this(address, dataType, classification, PointAccess.ReadOnly);
		}

		private TestRegister(int address, ModbusDataType dataType,
				@Nullable DataClassification classification, PointAccess access) {
			this.address = address;
			this.dataType = dataType;
			this.classification = classification;
			this.access = access;
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

		@Override
		public PointAccess getAccess() {
			return access;
		}

	}

	/** A write action. */
	@FunctionalInterface
	private interface WriteAction {

		void write() throws IOException;

	}

	private ModelData data;
	private BaseModelAccessor accessor;
	private IntShortMap deviceData;
	private StaticDataMapModbusConnection conn;

	@Before
	public void setup() {
		data = new ModelData(0);
		accessor = new GenericModelAccessor(data, 0, new GenericModelId(64000));
		deviceData = new IntShortMap();
		conn = new StaticDataMapModbusConnection(deviceData);
	}

	private void saveRegisters(TestRegister ref, int... words) throws IOException {
		data.performUpdates(m -> {
			m.saveDataArray(words, accessor.getBlockAddress() + ref.getAddress());
			return true;
		});
	}

	private int[] deviceRegisters(TestRegister ref) {
		int[] result = new int[ref.getWordLength()];
		for ( int i = 0; i < result.length; i++ ) {
			result[i] = deviceData.getValue(accessor.getBlockAddress() + ref.getAddress() + i) & 0xFFFF;
		}
		return result;
	}

	private void assertWriteRejected(String message, Class<? extends RuntimeException> errorType,
			WriteAction action) throws IOException {
		try {
			action.write();
			fail(message);
		} catch ( RuntimeException e ) {
			assertThat(message, e, is(instanceOf(errorType)));
		}
		assertThat("Nothing written to device", deviceData.size(), is(equalTo(0)));
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

	@Test
	public void write_uint16() throws IOException {
		// WHEN
		accessor.writeValue(conn, TestRegister.RwUInt16Value, 1234);

		// THEN
		assertThat("Device register written", deviceRegisters(TestRegister.RwUInt16Value),
				is(equalTo(new int[] { 1234 })));
		assertThat("Model data updated", accessor.getIntegerValue(TestRegister.RwUInt16Value),
				is(equalTo(1234)));
	}

	@Test
	public void write_int16_negative() throws IOException {
		// WHEN
		accessor.writeValue(conn, TestRegister.RwInt16Value, -5);

		// THEN
		assertThat("Device register written", deviceRegisters(TestRegister.RwInt16Value),
				is(equalTo(new int[] { 0xFFFB })));
		assertThat("Model data updated", accessor.getIntegerValue(TestRegister.RwInt16Value),
				is(equalTo(-5)));
	}

	@Test
	public void write_uint32() throws IOException {
		// WHEN
		accessor.writeValue(conn, TestRegister.RwUInt32Value, 70000);

		// THEN
		assertThat("Device registers written", deviceRegisters(TestRegister.RwUInt32Value),
				is(equalTo(new int[] { 0x0001, 0x1170 })));
		assertThat("Model data updated", accessor.getLongValue(TestRegister.RwUInt32Value),
				is(equalTo(70000L)));
	}

	@Test
	public void write_int32_negative() throws IOException {
		// WHEN
		accessor.writeValue(conn, TestRegister.RwInt32Value, -70000);

		// THEN
		assertThat("Device registers written", deviceRegisters(TestRegister.RwInt32Value),
				is(equalTo(new int[] { 0xFFFE, 0xEE90 })));
		assertThat("Model data updated", accessor.getIntegerValue(TestRegister.RwInt32Value),
				is(equalTo(-70000)));
	}

	@Test
	public void write_rounded() throws IOException {
		// WHEN
		accessor.writeValue(conn, TestRegister.RwUInt16Value, 2.5f);

		// THEN
		assertThat("Value rounded half up", deviceRegisters(TestRegister.RwUInt16Value),
				is(equalTo(new int[] { 3 })));
	}

	@Test
	public void write_offset() throws IOException {
		// WHEN
		accessor.writeValue(conn, TestRegister.RwUInt16Value, accessor.getBlockAddress() + 100, 7);

		// THEN
		assertThat("Device register written at offset",
				deviceData.getValue(
						accessor.getBlockAddress() + 100 + TestRegister.RwUInt16Value.getAddress())
						& 0xFFFF,
				is(equalTo(7)));
	}

	@Test
	public void write_notWritable() throws IOException {
		assertWriteRejected("Read-only point not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.UInt32Value, 1));
	}

	@Test
	public void write_uint16_notImplementedValue() throws IOException {
		assertWriteRejected("uint16 0xFFFF not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwUInt16Value, 0xFFFF));
	}

	@Test
	public void write_uint16_negative() throws IOException {
		assertWriteRejected("uint16 -1 not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwUInt16Value, -1));
	}

	@Test
	public void write_int16_notImplementedValue() throws IOException {
		assertWriteRejected("int16 -32768 not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwInt16Value, -32768));
	}

	@Test
	public void write_int16_tooLarge() throws IOException {
		assertWriteRejected("int16 40000 not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwInt16Value, 40000));
	}

	@Test
	public void write_uint32_notImplementedValue() throws IOException {
		assertWriteRejected("uint32 0xFFFFFFFF not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwUInt32Value, 0xFFFFFFFFL));
	}

	@Test
	public void write_bitfield16_mostSignificantBit() throws IOException {
		assertWriteRejected("bitfield16 0x8000 not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwBitfield16Value, 0x8000));
	}

	@Test
	public void write_notFinite() throws IOException {
		assertWriteRejected("NaN not writable", IllegalArgumentException.class,
				() -> accessor.writeValue(conn, TestRegister.RwUInt16Value, Float.NaN));
	}

	@Test
	public void writeScaled() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.ScaleFactorValue, 0xFFFE); // -2

		// WHEN
		accessor.writeScaledValue(conn, TestRegister.RwUInt32Value, TestRegister.ScaleFactorValue,
				599.95);

		// THEN
		assertThat("Device registers written", deviceRegisters(TestRegister.RwUInt32Value),
				is(equalTo(new int[] { 0x0000, 0xEA5B })));
		assertThat("Model data updated",
				accessor.getScaledValue(TestRegister.RwUInt32Value, TestRegister.ScaleFactorValue),
				is(equalTo(new BigDecimal("599.95"))));
	}

	@Test
	public void writeScaled_rounded() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.ScaleFactorValue, 0xFFFF); // -1

		// WHEN
		accessor.writeScaledValue(conn, TestRegister.RwUInt16Value, TestRegister.ScaleFactorValue,
				new BigDecimal("12.35"));

		// THEN
		assertThat("Value rounded half up", deviceRegisters(TestRegister.RwUInt16Value),
				is(equalTo(new int[] { 124 })));
	}

	@Test
	public void writeScaled_positiveScaleFactor() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.ScaleFactorValue, 2);

		// WHEN
		accessor.writeScaledValue(conn, TestRegister.RwInt16Value, TestRegister.ScaleFactorValue, -1250);

		// THEN
		assertThat("Value divided by 100 and rounded half up",
				deviceRegisters(TestRegister.RwInt16Value), is(equalTo(new int[] { 0xFFF3 })));
	}

	@Test
	public void writeScaled_scaleFactorNotRead() throws IOException {
		assertWriteRejected("Unread scale factor rejected", IllegalStateException.class, () -> accessor
				.writeScaledValue(conn, TestRegister.RwUInt16Value, TestRegister.ScaleFactorValue, 1));
	}

	@Test
	public void writeScaled_scaleFactorNotImplemented() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.ScaleFactorValue, 0x8000);

		// THEN
		assertWriteRejected("Not implemented scale factor rejected", IllegalStateException.class,
				() -> accessor.writeScaledValue(conn, TestRegister.RwUInt16Value,
						TestRegister.ScaleFactorValue, 1));
	}

	@Test
	public void codedValue() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Enum16Value, 1);

		// THEN
		assertThat("Code resolved",
				accessor.getCodedValue(TestRegister.Enum16Value, DerAcWiringType.class),
				is(equalTo(DerAcWiringType.SplitPhase)));
	}

	@Test
	public void codedValue_unknownCode() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Enum16Value, 9);

		// THEN
		assertThat("Unknown code is null",
				accessor.getCodedValue(TestRegister.Enum16Value, DerAcWiringType.class),
				is(nullValue()));
	}

	@Test
	public void codedValue_notImplemented() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Enum16Value, 0xFFFF);

		// THEN
		assertThat("Not implemented is null",
				accessor.getCodedValue(TestRegister.Enum16Value, DerAcWiringType.class),
				is(nullValue()));
	}

	@Test
	public void bitmaskableValues() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Bitfield16Value, 0x0003);

		// THEN
		assertThat("Bits resolved",
				accessor.getBitmaskableValues(TestRegister.Bitfield16Value,
						DerOperationalCharacteristic.class),
				is(equalTo(Set.of(DerOperationalCharacteristic.GridFollowing,
						DerOperationalCharacteristic.GridForming))));
	}

	@Test
	public void bitmaskableValues_unknownBits() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Bitfield16Value, 0x000C);

		// THEN
		assertThat("Unknown bit 3 ignored",
				accessor.getBitmaskableValues(TestRegister.Bitfield16Value,
						DerOperationalCharacteristic.class),
				is(equalTo(Set.of(DerOperationalCharacteristic.PvClipped))));
	}

	@Test
	public void bitmaskableValues_mostSignificantBit16() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Bitfield16Value, 0x8001);

		// THEN
		assertThat("Bitfield16 with MSB set not implemented", accessor
				.getBitmaskableValues(TestRegister.Bitfield16Value, DerOperationalCharacteristic.class),
				is(equalTo(Set.of())));
	}

	@Test
	public void bitmaskableValues_mostSignificantBit32() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Bitfield32Value, 0x8000, 0x0001);

		// THEN
		assertThat("Bitfield32 with MSB set not implemented", accessor
				.getBitmaskableValues(TestRegister.Bitfield32Value, DerOperationalCharacteristic.class),
				is(equalTo(Set.of())));
	}

	@Test
	public void bitmaskableValues_notImplemented() throws IOException {
		// GIVEN
		saveRegisters(TestRegister.Bitfield32Value, 0xFFFF, 0xFFFF);

		// THEN
		assertThat("Bitfield32 not implemented", accessor
				.getBitmaskableValues(TestRegister.Bitfield32Value, DerOperationalCharacteristic.class),
				is(equalTo(Set.of())));
	}

}
