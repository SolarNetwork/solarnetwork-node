/* ==================================================================
 * CanbusDataTests.java - 9/10/2019 10:21:02 am
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

package net.solarnetwork.node.io.canbus.test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Test;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.node.io.canbus.CanbusData;
import net.solarnetwork.node.io.canbus.CanbusData.CanbusDataUpdateAction;
import net.solarnetwork.node.io.canbus.CanbusData.MutableCanbusData;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.support.SimpleCanbusSignalReference;

/**
 * Test cases for the {@link CanbusData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusDataTests {

	@Test
	public void construct() {
		CanbusData d = new CanbusData();
		assertThat("Initial timestamp", d.getDataTimestamp(), is(nullValue()));
	}

	@Test
	public void performUpdateWithTimestamp() {
		final long now = System.currentTimeMillis();
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				return true;
			}
		});

		assertThat("Tmestamp updated", d.getDataTimestamp().toEpochMilli(), greaterThanOrEqualTo(now));
	}

	@Test
	public void performUpdateWithoutTimestamp() {
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				return false;
			}
		});

		assertThat("Tmestamp not updated", d.getDataTimestamp(), is(nullValue()));
	}

	@Test
	public void debugString() {
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(
						new FrameMessageImpl(0x123, false, 45, 676767,
								new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC }),
						new FrameMessageImpl(0x1, false, 45, 676767,
								new byte[] { (byte) 0x00, (byte) 0x01 }),
						new FrameMessageImpl(0x234, false, 56, 787878, new byte[] { (byte) 0xDD,
								(byte) 0xEE, (byte) 0xFF, (byte) 0x01, (byte) 0x00 })));
				return false;
			}
		});

		String str = d.dataDebugString();
		assertThat("Debug string", str, equalTo(
				"CanbusData{\n\t0x00000001: 0001\n\t0x00000123: AABBCC\n\t0x00000234: DDEEFF0100\n}"));
	}

	@Test
	public void getNumber_UInt32FromLower8Bytes() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(
				new SimpleCanbusSignalReference(0x1, BitDataType.UInt32, ByteOrdering.BigEndian, 0, 32));

		// THEN
		assertThat("Lower UInt32 parsed", n, equalTo(840293402L));
	}

	@Test
	public void getNumber_UInt32FromUpper8Bytes() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UInt32,
				ByteOrdering.BigEndian, 32, 32));

		// THEN
		assertThat("Upper UInt32 parsed", n, equalTo(1023894840L));
	}

	@Test
	public void getNumber_1BitFromWithinByte() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UnsignedInteger,
				ByteOrdering.BigEndian, 5, 1));

		// THEN
		assertThat("1-bit integer parsed", n, equalTo((short) 1));
	}

	@Test
	public void getNumber_2BitFromWithinByte() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UnsignedInteger,
				ByteOrdering.BigEndian, 4, 2));

		// THEN
		assertThat("2-bit integer parsed", n, equalTo((short) 3));
	}

	@Test
	public void getNumber_3BitFromWithinByte() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UnsignedInteger,
				ByteOrdering.BigEndian, 2, 3));

		// THEN
		assertThat("3-bit integer parsed", n, equalTo((short) 6));
	}

	@Test
	public void getNumber_12BitFromWithinBytes() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UnsignedInteger,
				ByteOrdering.BigEndian, 4, 12));

		// THEN
		assertThat("12-bit integer parsed", n, equalTo((short) 0x653));
	}

	@Test
	public void getNumber_15BitFromWithinBytes() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UnsignedInteger,
				ByteOrdering.BigEndian, 1, 15));

		// THEN
		assertThat("15-bit integer parsed", n, equalTo((short) 0x329C));
	}

	@Test
	public void getNumber_17BitFromWithinBytes() {
		// GIVEN
		CanbusData d = new CanbusData();
		d.performUpdates(new CanbusDataUpdateAction() {

			@Override
			public boolean updateCanbusData(MutableCanbusData m) {
				m.saveData(asList(new FrameMessageImpl(0x1, false, 2, 3,
						new byte[] { (byte) 0x32, (byte) 0x15, (byte) 0xDC, (byte) 0x1A, (byte) 0x3D,
								(byte) 0x07, (byte) 0x65, (byte) 0x38 })));
				return false;
			}
		});

		// WHEN
		Number n = d.getNumber(new SimpleCanbusSignalReference(0x1, BitDataType.UnsignedInteger,
				ByteOrdering.BigEndian, 2, 17));

		// THEN
		assertThat("17-bit integer parsed", n, equalTo(0x1D94E));
	}
}
