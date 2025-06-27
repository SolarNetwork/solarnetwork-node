/* ==================================================================
 * SDMDataTests.java - 24/01/2016 5:10:24 pm
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.deson.meter.SDMData;
import net.solarnetwork.node.hw.deson.meter.SDMWiringMode;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Unit tests for the {@link SDMData} class.
 * 
 * @author matt
 * @version 2.0
 */
public class SDM120DataTests {

	private static final short[] TEST_DATA_30001_80 = bytesToModbusWords(new int[] { 0x43, 0x64, 0xB3,
			0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 7 */0x41, 0x00, 0x28, 0xF6, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 13 */0xC4, 0xE5, 0x19, 0x9A, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 19 */0x44, 0xE5, 0x1F, 0x15, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, /* 25 */0x41, 0x90, 0xCC, 0xCD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, /* 31 */
			0xBF, 0x7F, 0xFC, 0xCC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, /* 71 */0x42, 0x47, 0xCC, 0xCD,
			/* 73 */0x3D, 0xC6, 0xA7, 0xF0, /* 75 */0x3D, 0x13, 0x74, 0xBC,
			/* 77 */0x3C, 0x13, 0x74, 0xBC, /* 79 */0x00, 0x00, 0x00, 0x00, });

	/**
	 * Convert an array of 8-bit numbers to 16-bit numbers, by combining pairs
	 * of bytes in big-endian order.
	 * 
	 * @param bytes
	 *        The bytes to combine into words.
	 * @return The array of words.
	 */
	public static final short[] bytesToModbusWords(int[] bytes) {
		// convert raw bytes into 16-bit modbus integers
		short[] w = new short[bytes.length / 2];
		for ( int i = 0, j = 0; i < bytes.length; i += 2, j += 1 ) {
			w[j] = (short) ((bytes[i] << 8) | bytes[i + 1]);
		}
		return w;
	}

	private SDMData getTestDataInstance() {
		SDMData data = new SDMData();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(TEST_DATA_30001_80, 0);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Test
	public void readDeviceInfo() {
		SDMData data = getTestDataInstance();
		Map<String, Object> info = data.getDeviceInfo();

		assertThat("Info size", info.keySet(), hasSize(2));
		assertThat("Model name", info, hasEntry(DataAccessor.INFO_KEY_DEVICE_MODEL, "SDM-120"));
		assertThat("Wiring type", info, hasEntry(SDMData.INFO_KEY_DEVICE_WIRING_TYPE,
				SDMWiringMode.OnePhaseTwoWire.getDescription()));
	}

	@Test
	public void interpretVoltage() {
		SDMData data = getTestDataInstance();
		Assert.assertEquals(228.7, data.getVoltage(), 0.001);
	}

	@Test
	public void interpretCurrent() {
		SDMData data = getTestDataInstance();
		Assert.assertEquals(8.01, data.getCurrent(), 0.001);
	}

	@Test
	public void interpretPower() {
		SDMData data = getTestDataInstance();
		assertThat("Active power", data.getActivePower(), equalTo(-1833));
		assertThat("Apparent power", data.getApparentPower(), equalTo(1833));
		assertThat("Reactive power", data.getReactivePower(), equalTo(18));
	}

	@Test
	public void interpretPowerFactor() {
		SDMData data = getTestDataInstance();
		assertThat("Power factor", data.getPowerFactor(), equalTo(-0.9999511f));
	}

	@Test
	public void interpretFrequency() {
		SDMData data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(49.95f));
	}

	@Test
	public void interpretEnergy() {
		SDMData data = getTestDataInstance();
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(97L));
		assertThat("Active energy received", data.getActiveEnergyReceived(), equalTo(36L));
		assertThat("Reqctive energy delivered", data.getReactiveEnergyDelivered(), equalTo(9L));
		assertThat("Reqctive energy received", data.getReactiveEnergyReceived(), equalTo(0L));
	}

}
