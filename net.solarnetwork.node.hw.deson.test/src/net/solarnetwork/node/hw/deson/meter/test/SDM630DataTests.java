/* ==================================================================
 * SDM630DataTests.java - 26/01/2016 9:35:21 am
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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.deson.meter.SDM630Register;
import net.solarnetwork.node.hw.deson.meter.SDMData;
import net.solarnetwork.node.hw.deson.meter.SDMDeviceType;
import net.solarnetwork.node.hw.deson.meter.SDMWiringMode;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link SDM630Data} class.
 * 
 * @author matt
 * @version 2.0
 */
public class SDM630DataTests {

	private static final short[] TEST_DATA_30001_80 = SDM120DataTests.bytesToModbusWords(
	// @formatter:off
					new int[] { 
						/* 000 */	0x43, 0x64, 0xB3, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						/* 005 */	0x00, 0x00, 0x41, 0x00, 0x28, 0xF6, 0x00, 0x00, 0x00, 0x00, 
						/* 010 */	0x00, 0x00, 0x00, 0x00, 0xC4, 0xE5, 0x19, 0x9A, 0x00, 0x00, 
						/* 015 */	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x44, 0xE5, 0x1F, 0x15, 
						/* 020 */	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x41, 0x90, 
						/* 025 */	0xCC, 0xCD, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						/* 030 */	0xBF, 0x7F, 0xFC, 0xCC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						/* 035 */	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						/* 040 */	0x00, 0x00, 0x00, 0x00, 0x43, 0x64, 0xC4, 0x9C, 0x00, 0x00, 
						/* 045 */	0x00, 0x00, 0x41, 0x5A, 0x45, 0xA2, 0x00, 0x00, 0x00, 0x00, 
						/* 050 */	0x00, 0x00, 0x00, 0x00, 0x44, 0x9D, 0x99, 0xEC, 0x00, 0x00, 
						/* 055 */	0x00, 0x00, 0xC4, 0x9F, 0x68, 0x42, 0x00, 0x00, 0x00, 0x00, 
						/* 060 */	0x41, 0x90, 0xE5, 0x60, 0xBF, 0x7F, 0xFC, 0xCC, 0x00, 0x00, 
						/* 065 */	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						/* 070 */	0x42, 0x47, 0xCC, 0xCD, 0x3D, 0xC6, 0xA7, 0xF0, 0x3D, 0x13, 
						/* 075 */	0x74, 0xBC, 0x3C, 0x13, 0x74, 0xBC, 0x00, 0x00, 0x00, 0x00, 
			});
	// @formatter:on

	private SDMData getTestDataInstance() {
		SDMData data = new SDMData();
		data.setDeviceType(SDMDeviceType.SDM630);
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(TEST_DATA_30001_80, 0);
					return true;
				}
			});
			data.performControlUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(new int[] { 0x4040, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
							0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
							0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
							0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 },
							SDM630Register.ConfigWiringMode.getAddress());
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
		assertThat("Info size", info.keySet(), hasSize(3));
		assertThat("Info", info,
				allOf(hasEntry(DataAccessor.INFO_KEY_DEVICE_MODEL, "SDM-630"),
						hasEntry(SDMData.INFO_KEY_DEVICE_WIRING_TYPE, "3 phase, 4 wire"),
						hasEntry(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, "123456")));
	}

	@Test
	public void interpretWiringMode1P2() throws IOException {
		SDMData data = getTestDataInstance();
		data.performControlUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(
						new int[] { 0x3f80, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 },
						SDM630Register.ConfigWiringMode.getAddress());
				return true;
			}
		});
		assertThat("Wiring mode", data.getWiringMode(), equalTo(SDMWiringMode.OnePhaseTwoWire));
	}

	@Test
	public void interpretWiringMode3P3() throws IOException {
		SDMData data = getTestDataInstance();
		data.performControlUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(
						new int[] { 0x4000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 },
						SDM630Register.ConfigWiringMode.getAddress());
				return true;
			}
		});
		assertThat("Wiring mode", data.getWiringMode(), equalTo(SDMWiringMode.ThreePhaseThreeWire));
	}

	@Test
	public void interpretWiringMode3P4() throws IOException {
		SDMData data = getTestDataInstance();
		data.performControlUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(
						new int[] { 0x4040, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x47f1, 0x2000 },
						SDM630Register.ConfigWiringMode.getAddress());
				return true;
			}
		});
		assertThat("Wiring mode", data.getWiringMode(), equalTo(SDMWiringMode.ThreePhaseFourWire));
	}

	@Test
	public void interpretVoltage() {
		SDMData data = getTestDataInstance();
		assertThat("Voltage", data.getVoltage(), equalTo(228.768f));
	}

	@Test
	public void interpretCurrent() {
		SDMData data = getTestDataInstance();
		assertThat("Current", data.getCurrent(), equalTo(13.642f));
	}

	@Test
	public void interpretPower() {
		SDMData data = getTestDataInstance();
		assertThat("Active power", data.getActivePower(), equalTo(1261));
		assertThat("Apparent power", data.getApparentPower(), equalTo(-1275));
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
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(), equalTo(9L));
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), equalTo(0L));
	}

}
