/* ==================================================================
 * ModbusDataTests.java - 20/12/2017 10:01:23 AM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link ModbusData} class.
 *
 * @author matt
 * @version 2.1
 */
public class ModbusDataTests {

	private static SecureRandom rng() {
		return new SecureRandom();
	}

	private static final SecureRandom RNG = rng();

	@Test
	public void construct() {
		ModbusData d = new ModbusData();
		assertThat("Initial timestamp", d.getDataTimestamp(), is(nullValue()));
	}

	@Test
	public void performUpdateWithTimestamp() throws IOException {
		final long now = System.currentTimeMillis();
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				return true;
			}
		});

		assertThat("Tmestamp updated", d.getDataTimestamp().toEpochMilli(), greaterThanOrEqualTo(now));
	}

	@Test
	public void performUpdateWithoutTimestamp() throws IOException {
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				return false;
			}
		});

		assertThat("Tmestamp not updated", d.getDataTimestamp(), is(nullValue()));
	}

	@Test
	public void readInt32() throws IOException {
		ModbusData d = new ModbusData();
		final long l = 123123123L;
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(
						new short[] { (short) ((l >> 16) & 0xFFFF), (short) ((l >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("32-bit integer", d.getUnsignedInt32(0), equalTo(l));
	}

	@Test
	public void readInt32SavedFromInts() throws IOException {
		ModbusData d = new ModbusData();
		final long l = 123123123L;
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { (int) ((l >> 16) & 0xFFFF), (int) ((l >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("32-bit integer", d.getUnsignedInt32(0), equalTo(l));
	}

	@Test
	public void readInt32SavedFromIntegers() throws IOException {
		ModbusData d = new ModbusData();
		final long l = 123123123L;
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new Integer[] { Integer.valueOf((int) (l >> 16) & 0xFFFF),
						Integer.valueOf((int) ((l >> 0) & 0xFFFF)) }, 0);
				return false;
			}
		});

		assertThat("32-bit integer", d.getUnsignedInt32(0), equalTo(l));
	}

	@Test
	public void readLargeUnsignedInt32() throws IOException {
		ModbusData d = new ModbusData();
		final long l = (Integer.MAX_VALUE + 1231231231L) & 0xFFFFFFFFL;
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(
						new short[] { (short) ((l >> 16) & 0xFFFF), (short) ((l >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("Unsigned 32-bit integer", d.getUnsignedInt32(0), equalTo(l));
	}

	@Test
	public void readLargeUnsignedInt64() throws IOException {
		ModbusData d = new ModbusData();
		final BigInteger ull = new BigInteger("AC93244488888ABB", 16);
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new short[] { (short) (ull.shiftRight(48).intValue() & 0xFFFF),
						(short) (ull.shiftRight(32).intValue() & 0xFFFF),
						(short) (ull.shiftRight(16).intValue() & 0xFFFF),
						(short) (ull.intValue() & 0xFFFF), }, 0);
				return false;
			}
		});

		assertThat("Unsigned 64-bit integer", d.getUnsignedInt64(0), equalTo(ull));
	}

	@Test
	public void readFloat32() throws IOException {
		ModbusData d = new ModbusData();
		final float f = RNG.nextFloat();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				int intFloat = Float.floatToIntBits(f);
				m.saveDataArray(new short[] { (short) ((intFloat >> 16) & 0xFFFF),
						(short) ((intFloat >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("32-bit floating point", d.getFloat32(0), equalTo(f));
	}

	@Test
	public void readFloat32LeastToMostSignificant() throws IOException {
		ModbusData d = new ModbusData();
		d.setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		final float f = RNG.nextFloat();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				int intFloat = Float.floatToIntBits(f);
				m.saveDataArray(new short[] { (short) ((intFloat >> 0) & 0xFFFF),
						(short) ((intFloat >> 16) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("32-bit floating point", d.getFloat32(0), equalTo(f));
	}

	@Test
	public void readInt64() throws IOException {
		ModbusData d = new ModbusData();
		final long l = RNG.nextLong();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new short[] { (short) ((l >> 48) & 0xFFFF), (short) ((l >> 32) & 0xFFFF),
						(short) ((l >> 16) & 0xFFFF), (short) ((l >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("Updated data", d.getInt64(0), equalTo(l));
	}

	@Test
	public void readInt64LeastToMostSignificant() throws IOException {
		ModbusData d = new ModbusData();
		d.setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		final long l = RNG.nextLong();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new short[] { (short) ((l >> 0) & 0xFFFF), (short) ((l >> 16) & 0xFFFF),
						(short) ((l >> 32) & 0xFFFF), (short) ((l >> 48) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("Updated data", d.getInt64(0), equalTo(l));
	}

	@Test
	public void readFloat64() throws IOException {
		ModbusData d = new ModbusData();
		final double f = RNG.nextDouble();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				long longFloat = Double.doubleToLongBits(f);
				m.saveDataArray(new short[] { (short) ((longFloat >> 48) & 0xFFFF),
						(short) ((longFloat >> 32) & 0xFFFF), (short) ((longFloat >> 16) & 0xFFFF),
						(short) ((longFloat >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("64-bit floating point", d.getFloat64(0), equalTo(f));
	}

	@Test
	public void readFloat64LeastToMostSignificant() throws IOException {
		ModbusData d = new ModbusData();
		d.setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		final double f = RNG.nextDouble();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				long longFloat = Double.doubleToLongBits(f);
				m.saveDataArray(new short[] { (short) ((longFloat >> 0) & 0xFFFF),
						(short) ((longFloat >> 16) & 0xFFFF), (short) ((longFloat >> 32) & 0xFFFF),
						(short) ((longFloat >> 48) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("64-bit floating point", d.getFloat64(0), equalTo(f));
	}

	@Test
	public void readBytes() throws IOException {
		final int len = 2 + 2 * RNG.nextInt(8); // ensures even number > 2
		final byte[] b = new byte[len];
		RNG.nextBytes(b);
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveBytes(b, 0);
				return false;
			}
		});

		assertThat("Byte array", d.getBytes(0, (int) Math.ceil(b.length / 2.0)), equalTo(b));
	}

	@Test
	public void readBytesLeastToMostSignificant() throws IOException {
		final int len = 2 + 2 * RNG.nextInt(8); // ensures even number > 2
		final byte[] b = new byte[len];
		RNG.nextBytes(b);
		ModbusData d = new ModbusData();
		d.setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveBytes(b, 0);
				return false;
			}
		});

		assertThat("Byte array", d.getBytes(0, (int) Math.ceil(b.length / 2.0)), equalTo(b));
	}

	@Test
	public void readUtf8String() throws Exception {
		final String str = "Four score and seven years ago...";
		final byte[] b = str.getBytes(ByteUtils.UTF8);
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveBytes(b, 0);
				return false;
			}
		});

		assertThat("String", d.getUtf8String(0, (int) Math.ceil(b.length / 2.0), true), equalTo(str));
	}

	@Test
	public void readUtf8StringLeastToMostSignificant() throws Exception {
		final String str = "Four score and seven years ago...";
		final byte[] b = str.getBytes(ByteUtils.UTF8);
		ModbusData d = new ModbusData();
		d.setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveBytes(b, 0);
				return false;
			}
		});

		assertThat("String", d.getUtf8String(0, (int) Math.ceil(b.length / 2.0), true), equalTo(str));
	}

	@Test
	public void readAsciiString() throws Exception {
		final String str = "To be or not to be...";
		final byte[] b = str.getBytes(ByteUtils.ASCII);
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveBytes(b, 0);
				return false;
			}
		});

		assertThat("String", d.getUtf8String(0, (int) Math.ceil(b.length / 2.0), true), equalTo(str));
	}

	@Test
	public void readAsciiStringLeastToMostSignificant() throws Exception {
		final String str = "To be or not to be...";
		final byte[] b = str.getBytes(ByteUtils.ASCII);
		ModbusData d = new ModbusData();
		d.setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveBytes(b, 0);
				return false;
			}
		});

		assertThat("String", d.getUtf8String(0, (int) Math.ceil(b.length / 2.0), true), equalTo(str));
	}

	@Test
	public void readSignedInt16() throws IOException {
		final short s = (short) -123;
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new short[] { s }, 0);
				return false;
			}
		});

		assertThat("Signed 16-bit integer", d.getInt16(0), equalTo(s));
	}

	@Test
	public void readSignedInt16_notAvailable() throws IOException {
		ModbusData d = new ModbusData(false);
		d.getInt16(0);
	}

	@Test(expected = NoSuchElementException.class)
	public void readSignedInt16_strict_notAvailable() throws IOException {
		ModbusData d = new ModbusData(true);
		assertThat("Signed 16-bit integer", d.getInt16(0), equalTo((short) 0));
	}

	@Test
	public void debugString() throws IOException {
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { 0xABCD, 0xFEDC, 0x1122, 0x3456 }, 0);
				m.saveDataArray(new int[] { 0x9999 }, 9); // throw in a lone odd word
				m.saveDataArray(new int[] { 0xFF01, 0xFF02, 0xFF03, 0xFF04, 0xFF05 }, 1000); // end in odd word
				return false;
			}
		});

		String str = d.dataDebugString();
		assertThat("Debug string", str, equalTo(
				"ModbusData{\n\t    0: 0xABCD, 0xFEDC\n\t    2: 0x1122, 0x3456\n\t    8:       , 0x9999\n\t 1000: 0xFF01, 0xFF02\n\t 1002: 0xFF03, 0xFF04\n\t 1004: 0xFF05\n}"));
	}

	@Test
	public void debugStringLeadingOdd() throws IOException {
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { 0xABCD, 0xFEDC, 0x1122 }, 1);
				return false;
			}
		});

		String str = d.dataDebugString();
		assertThat("Debug string", str,
				equalTo("ModbusData{\n\t    0:       , 0xABCD\n\t    2: 0xFEDC, 0x1122\n}"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void unsignedDataMap() throws IOException {
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { 0xABCD, 0xFEDC, 0x1122, 0x3456 }, 0);
				m.saveDataArray(new int[] { 0x9999 }, 9); // throw in a lone odd word
				m.saveDataArray(new int[] { 0xFF01, 0xFF02, 0xFF03, 0xFF04, 0xFF05 }, 1000); // end in odd word
				return false;
			}
		});

		Map<Integer, Integer> dataMap = d.getUnsignedDataMap();
		assertThat("Data map size", dataMap.entrySet(), hasSize(10));
		assertThat("Data map contains", dataMap,
				allOf(hasEntry(0, 0xABCD), hasEntry(1, 0xFEDC), hasEntry(2, 0x1122), hasEntry(3, 0x3456),
						hasEntry(9, 0x9999), hasEntry(1000, 0xFF01), hasEntry(1001, 0xFF02),
						hasEntry(1002, 0xFF03), hasEntry(1003, 0xFF04), hasEntry(1004, 0xFF05)));
	}
}
