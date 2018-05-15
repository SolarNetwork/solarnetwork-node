/* ==================================================================
 * ION6200DataTests.java - 15/05/2018 12:35:29 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.schneider.meter.ION6200Data;
import net.solarnetwork.node.hw.schneider.meter.ION6200DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.ION6200Register;
import net.solarnetwork.node.hw.schneider.meter.ION6200VoltsMode;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link ION6200Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ION6200DataTests {

	private final int[] TEST_INFO_1_REG_0 = new int[] {
			// @formatter:off
			0x40D8,
			0x04C6,
			0x00D3,
			0x0001,
			0x0009,
			0x0000,
			0x0034,
			0x0001,
			0x7695,
			0x0C96,
			0x7704,
			0x0000,
			0x1838,
			// @formatter:on
	};

	private final int[] TEST_METER_1_REG_102 = new int[] {
			// @formatter:off
			0x0B0F,
			0x133C,
			0x131E,
			0x131C,
			0x1328,
			0x01EA,
			0x0113,
			0x0112,
			0x015A,
			0x015E,
			0x040F,
			0x0390,
			0x176D,
			0xDF2A,
			0xEB03,
			0x11DF,
			0x17E5,
			0x0361,
			0x022D,
			0x0405,
			0x02E6,
			0xFE9D,
			0x01DD,
			0x026F,
			0x0044,
			0xFF7B,
			0x0566,
			0x0308,
			0x030C,
			0x0265,
			0x08FC,
			0x027B,
			0x0381,
			0x08EC,
			0x16C7,
			0x6E3B,
			0x0006,
			0x2318,
			0x0001,
			0x5279,
			0x0001,
			0x3302,
			0x0002,
			// @formatter:on
	};

	private final int[] TEST_CONFIG_1_REG_4000 = new int[] {
			// @formatter:off
			0x0000,
			0x01E0,
			0x01E0,
			0x07D0,
			0x0005,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0004,
			0x0004,
			0x0004,
			0x0005,
			0x1838,
			// @formatter:on
	};

	private static final Logger log = LoggerFactory.getLogger(ION6200DataTests.class);

	private ION6200Data getTestDataInstance() {
		ION6200Data data = new ION6200Data();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_INFO_1_REG_0, 0);
				m.saveDataArray(TEST_METER_1_REG_102, 102);
				m.saveDataArray(TEST_CONFIG_1_REG_4000, 4000);
				return true;
			}
		});
		return data;
	}

	@Test
	public void dataDebugString() {
		ION6200Data data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfoAndConfig() {
		ION6200Data data = getTestDataInstance();
		assertThat("Device type", data.getDeviceType(), equalTo(6200));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo(211));
		assertThat("Serial number", data.getSerialNumber(), equalTo(80101592L));
		assertThat("Volts mode", data.getVoltsMode(), equalTo(ION6200VoltsMode.FourWire));
		assertThat("PVS", data.getInt16(ION6200Register.ConfigProgrammableVoltageScale.getAddress()),
				equalTo(4));
		assertThat("PCS", data.getInt16(ION6200Register.ConfigProgrammableCurrentScale.getAddress()),
				equalTo(4));
		assertThat("PnS",
				data.getInt16(ION6200Register.ConfigProgrammableNeutralCurrentScale.getAddress()),
				equalTo(4));
		assertThat("PPS", data.getInt16(ION6200Register.ConfigProgrammablePowerScale.getAddress()),
				equalTo(5));
	}

	@Test
	public void interpretBasic() {
		ION6200DataAccessor data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(59.97f));
		assertThat("Voltage", data.getVoltage(), equalTo(283.1f));
		assertThat("Current", data.getCurrent(), equalTo(34.6f));
		assertThat("Power factor", data.getPowerFactor(), equalTo(-84.06f));
	}

	@Test
	public void interpretPower() {
		ION6200DataAccessor data = getTestDataInstance();
		assertThat("Active power", data.getActivePower(), equalTo(8650));
		assertThat("Apparent power", data.getApparentPower(), equalTo(10290));
		assertThat("Reactive power", data.getReactivePower(), equalTo(5570));
	}

	@Test
	public void interpretEnergy() {
		ION6200DataAccessor data = getTestDataInstance();
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(421435000L));
		assertThat("Active energy received", data.getActiveEnergyReceived(), equalTo(74520000L));
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(), equalTo(86649000L));
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), equalTo(144130000L));
	}

}
