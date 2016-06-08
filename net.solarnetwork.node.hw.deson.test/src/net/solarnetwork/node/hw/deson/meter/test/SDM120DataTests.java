/* ==================================================================
 * SDM120DataTests.java - 24/01/2016 5:10:24 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.deson.meter.test;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.util.Map;
import net.solarnetwork.node.hw.deson.meter.SDM120Data;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link SDM120Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SDM120DataTests {

	private static final int[] TEST_DATA_30001_80 = bytesToModbusWords(new int[] { 0x43, 0x64, 0xB3,
			0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 7 */0x41, 0x00, 0x28, 0xF6, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 13 */0xC4, 0xE5, 0x19, 0x9A, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 19 */0x44, 0xE5, 0x1F, 0x15, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00,
			/* 25 */0x41, 0x90, 0xCC, 0xCD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 31 */
			0xBF, 0x7F, 0xFC, 0xCC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 71 */0x42, 0x47, 0xCC, 0xCD, /* 73 */0x3D, 0xC6, 0xA7, 0xF0,
			/* 75 */0x3D, 0x13, 0x74, 0xBC, /* 77 */0x3C, 0x13, 0x74, 0xBC, /* 79 */0x00, 0x00, 0x00,
			0x00, });

	/**
	 * Convert an array of 8-bit numbers to 16-bit numbers, by combining pairs
	 * of bytes in big-endian order.
	 * 
	 * @param bytes
	 *        The bytes to combine into words.
	 * @return The array of words.
	 */
	public static final int[] bytesToModbusWords(int[] bytes) {
		// convert raw bytes into 16-bit modbus integers
		int[] ints = new int[bytes.length / 2];
		for ( int i = 0, j = 0; i < bytes.length; i += 2, j += 1 ) {
			ints[j] = ((bytes[i] << 8) | bytes[i + 1]);
		}
		return ints;
	}

	private static class TestSDM120Data extends SDM120Data {

		@Override
		public void saveDataArray(final int[] data, int addr) {
			super.saveDataArray(data, addr);
		}

	}

	private ModbusConnection conn;

	@Before
	public void setup() {
		conn = EasyMock.createMock(ModbusConnection.class);
	}

	private SDM120Data getTestDataInstance() {
		TestSDM120Data data = new TestSDM120Data();
		data.saveDataArray(TEST_DATA_30001_80, 0);
		return data;
	}

	@Test
	public void readDeviceInfo() {
		replay(conn);

		TestSDM120Data data = new TestSDM120Data();
		data.readControlData(conn);

		Map<String, Object> info = data.getDeviceInfo();

		verify(conn);

		Assert.assertNotNull(info);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals("SDM-120", info.get(ModbusDeviceSupport.INFO_KEY_DEVICE_MODEL));
	}

	@Test
	public void interpretVoltage() {
		SDM120Data data = getTestDataInstance();
		Assert.assertEquals(228.7, data.getVoltage(SDM120Data.ADDR_DATA_V_NEUTRAL), 0.001);
	}

	@Test
	public void interpretCurrent() {
		SDM120Data data = getTestDataInstance();
		Assert.assertEquals(8.01, data.getCurrent(SDM120Data.ADDR_DATA_I), 0.001);
	}

	@Test
	public void interpretPower() {
		SDM120Data data = getTestDataInstance();
		Assert.assertEquals(-1833, (int) data.getPower(SDM120Data.ADDR_DATA_ACTIVE_POWER));
		Assert.assertEquals(1833, (int) data.getPower(SDM120Data.ADDR_DATA_APPARENT_POWER));
		Assert.assertEquals(18, (int) data.getPower(SDM120Data.ADDR_DATA_REACTIVE_POWER));
	}

	@Test
	public void interpretPowerFactor() {
		SDM120Data data = getTestDataInstance();
		Assert.assertEquals(-0.9999511, data.getPowerFactor(SDM120Data.ADDR_DATA_POWER_FACTOR), 0.001);
	}

	@Test
	public void interpretFrequency() {
		SDM120Data data = getTestDataInstance();
		Assert.assertEquals(49.95, data.getFrequency(SDM120Data.ADDR_DATA_FREQUENCY), 0.001);
	}

	@Test
	public void interpretEnergy() {
		SDM120Data data = getTestDataInstance();
		Assert.assertEquals(97L, (long) data.getEnergy(SDM120Data.ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL));
		Assert.assertEquals(36L, (long) data.getEnergy(SDM120Data.ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL));
		Assert.assertEquals(9L, (long) data.getEnergy(SDM120Data.ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL));
		Assert.assertEquals(0L, (long) data.getEnergy(SDM120Data.ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL));
	}

}
