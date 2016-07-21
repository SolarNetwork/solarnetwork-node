/* ==================================================================
 * SDM360DataTests.java - 26/01/2016 9:35:21 am
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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.deson.meter.SDM120Data;
import net.solarnetwork.node.hw.deson.meter.SDM360Data;
import net.solarnetwork.node.hw.deson.meter.SDMWiringMode;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;

/**
 * Test cases for the {@link SDM360Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SDM360DataTests {

	private static final int[] TEST_DATA_30001_80 = SDM120DataTests.bytesToModbusWords(new int[] { 0x43,
			0x64, 0xB3, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 7 */0x41, 0x00, 0x28,
			0xF6, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 13 */0xC4, 0xE5, 0x19, 0x9A, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 19 */0x44, 0xE5, 0x1F, 0x15, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 25 */0x41, 0x90, 0xCC, 0xCD, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, /* 31 */
			0xBF, 0x7F, 0xFC, 0xCC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 71 */0x42, 0x47, 0xCC, 0xCD, /* 73 */0x3D, 0xC6, 0xA7, 0xF0,
			/* 75 */0x3D, 0x13, 0x74, 0xBC, /* 77 */0x3C, 0x13, 0x74, 0xBC, /* 79 */0x00, 0x00, 0x00,
			0x00, });

	private static class TestSDM360Data extends SDM360Data {

		@Override
		public void saveDataArray(final int[] data, int addr) {
			super.saveDataArray(data, addr);
		}

		@Override
		public void saveControlArray(int[] data, int addr) {
			super.saveControlArray(data, addr);
		}

	}

	private ModbusConnection conn;

	@Before
	public void setup() {
		conn = EasyMock.createMock(ModbusConnection.class);
	}

	private SDM360Data getTestDataInstance() {
		TestSDM360Data data = new TestSDM360Data();
		data.saveDataArray(TEST_DATA_30001_80, 0);
		return data;
	}

	@Test
	public void readDeviceInfo() {
		expect(conn.readInts(SDM360Data.ADDR_SYSTEM_WIRING_TYPE, 34))
				.andReturn(new int[] { 0x4040, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 });
		replay(conn);

		TestSDM360Data data = new TestSDM360Data();
		data.readControlData(conn);

		Map<String, Object> info = data.getDeviceInfo();

		verify(conn);

		Assert.assertNotNull(info);
		Assert.assertEquals(3, info.size());
		Assert.assertEquals("SDM-360", info.get(ModbusDeviceSupport.INFO_KEY_DEVICE_MODEL));
		Assert.assertEquals("3 phase, 4 wire", info.get(SDM360Data.INFO_KEY_DEVICE_WIRING_TYPE));
		Assert.assertNotNull(info.get(ModbusDeviceSupport.INFO_KEY_DEVICE_SERIAL_NUMBER));
		Assert.assertEquals("123456.0", info.get(ModbusDeviceSupport.INFO_KEY_DEVICE_SERIAL_NUMBER));
	}

	@Test
	public void interpretWiringMode1P2() {
		expect(conn.readInts(SDM360Data.ADDR_SYSTEM_WIRING_TYPE, 34))
				.andReturn(new int[] { 0x3f80, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 });
		replay(conn);

		TestSDM360Data data = new TestSDM360Data();
		data.readControlData(conn);

		verify(conn);

		Assert.assertEquals(SDMWiringMode.OnePhaseTwoWire, data.getWiringMode());
	}

	@Test
	public void interpretWiringMode3P3() {
		expect(conn.readInts(SDM360Data.ADDR_SYSTEM_WIRING_TYPE, 34))
				.andReturn(new int[] { 0x4000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 });
		replay(conn);

		TestSDM360Data data = new TestSDM360Data();
		data.readControlData(conn);

		verify(conn);

		Assert.assertEquals(SDMWiringMode.ThreePhaseThreeWire, data.getWiringMode());
	}

	@Test
	public void interpretWiringMode3P4() {
		expect(conn.readInts(SDM360Data.ADDR_SYSTEM_WIRING_TYPE, 34))
				.andReturn(new int[] { 0x4040, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 });
		replay(conn);

		TestSDM360Data data = new TestSDM360Data();
		data.readControlData(conn);

		verify(conn);

		Assert.assertEquals(SDMWiringMode.ThreePhaseFourWire, data.getWiringMode());
	}

	@Test
	public void interpretVoltage() {
		SDM360Data data = getTestDataInstance();
		Assert.assertEquals(228.7, data.getVoltage(SDM120Data.ADDR_DATA_V_NEUTRAL), 0.001);
	}

	@Test
	public void interpretCurrent() {
		SDM360Data data = getTestDataInstance();
		Assert.assertEquals(8.01, data.getCurrent(SDM120Data.ADDR_DATA_I), 0.001);
	}

	@Test
	public void interpretPower() {
		SDM360Data data = getTestDataInstance();
		Assert.assertEquals(-1833, (int) data.getPower(SDM360Data.ADDR_DATA_ACTIVE_POWER_P1));
		Assert.assertEquals(1833, (int) data.getPower(SDM360Data.ADDR_DATA_APPARENT_POWER_P1));
		Assert.assertEquals(18, (int) data.getPower(SDM360Data.ADDR_DATA_REACTIVE_POWER_P1));
	}

	@Test
	public void interpretPowerFactor() {
		SDM360Data data = getTestDataInstance();
		Assert.assertEquals(-0.9999511, data.getPowerFactor(SDM360Data.ADDR_DATA_POWER_FACTOR_P1),
				0.001);
	}

	@Test
	public void interpretFrequency() {
		SDM360Data data = getTestDataInstance();
		Assert.assertEquals(49.95, data.getFrequency(SDM360Data.ADDR_DATA_FREQUENCY), 0.001);
	}

	@Test
	public void interpretEnergy() {
		SDM360Data data = getTestDataInstance();
		Assert.assertEquals(97L, (long) data.getEnergy(SDM360Data.ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL));
		Assert.assertEquals(36L, (long) data.getEnergy(SDM360Data.ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL));
		Assert.assertEquals(9L,
				(long) data.getEnergy(SDM360Data.ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL));
		Assert.assertEquals(0L,
				(long) data.getEnergy(SDM360Data.ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL));
	}

}
