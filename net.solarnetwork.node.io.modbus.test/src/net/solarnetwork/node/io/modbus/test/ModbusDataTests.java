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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;

/**
 * Test cases for the {@link ModbusData} class.
 * 
 * @author matt
 * @version 1.3
 */
public class ModbusDataTests {

	private static SecureRandom rng() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException(e);
		}
	}

	private static final SecureRandom RNG = rng();

	@Test
	public void construct() {
		ModbusData d = new ModbusData();
		assertThat("Initial timestamp", d.getDataTimestamp(), equalTo(0L));
	}

	@Test
	public void performUpdateWithTimestamp() {
		final long now = System.currentTimeMillis();
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				return true;
			}
		});

		assertThat("Tmestamp updated", d.getDataTimestamp(), greaterThanOrEqualTo(now));
	}

	@Test
	public void performUpdateWithoutTimestamp() {
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				return false;
			}
		});

		assertThat("Tmestamp not updated", d.getDataTimestamp(), equalTo(0L));
	}

	@Test
	public void readInt32() {
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

		assertThat("32-bit integer", d.getInt32(0), equalTo(l));
	}

	@Test
	public void readInt32SavedFromInts() {
		ModbusData d = new ModbusData();
		final long l = 123123123L;
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { (int) ((l >> 16) & 0xFFFF), (int) ((l >> 0) & 0xFFFF) }, 0);
				return false;
			}
		});

		assertThat("32-bit integer", d.getInt32(0), equalTo(l));
	}

	@Test
	public void readInt32SavedFromIntegers() {
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

		assertThat("32-bit integer", d.getInt32(0), equalTo(l));
	}

	@Test
	public void readLargeUnsignedInt32() {
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

		assertThat("Unsigned 32-bit integer", d.getInt32(0), equalTo(l));
	}

	@Test
	public void readLargeUnsignedInt64() {
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
	public void readFloat32() {
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
	public void readFloat32LeastToMostSignificant() {
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
	public void readInt64() {
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
	public void readInt64LeastToMostSignificant() {
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
	public void readFloat64() {
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
	public void readFloat64LeastToMostSignificant() {
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
	public void readBytes() {
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
	public void readBytesLeastToMostSignificant() {
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
		final byte[] b = str.getBytes(ModbusHelper.UTF8_CHARSET);
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
		final byte[] b = str.getBytes(ModbusHelper.UTF8_CHARSET);
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
		final byte[] b = str.getBytes(ModbusHelper.ASCII_CHARSET);
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
		final byte[] b = str.getBytes(ModbusHelper.ASCII_CHARSET);
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
	public void readSignedInt16() {
		final short s = (short) -123;
		ModbusData d = new ModbusData();
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new short[] { s }, 0);
				return false;
			}
		});

		assertThat("Signed 16-bit integer", d.getSignedInt16(0), equalTo(s));
	}

	@Test
	public void debugString() {
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
	public void debugStringLeadingOdd() {
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
}
