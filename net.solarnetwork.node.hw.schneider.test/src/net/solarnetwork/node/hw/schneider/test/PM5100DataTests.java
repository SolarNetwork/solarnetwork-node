/* ==================================================================
 * PM5100DataTests.java - 18/05/2018 7:13:52 AM
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
import net.solarnetwork.node.hw.schneider.meter.PM5100Data;
import net.solarnetwork.node.hw.schneider.meter.PM5100DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.PM5100Model;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Tests for the {@link PM5100Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PM5100DataTests {

	// @formatter:off
	private static final int[] TEST_DATA_REG_28 = {
			0x0000, 0x506F,
			0x7765, 0x7220,
			0x4D65, 0x7465,
			0x7220, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x504D,
			0x3533, 0x3330,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x5363,
			0x686E, 0x6569,
			0x6465, 0x7220,
			0x456C, 0x6563,
			0x7472, 0x6963,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x3BA9,
	};
	
	private static final int[] TEST_DATA_REG_128 = {
			0x0022, 0x055D,
			0xB7C3, 0x0010,
			0x081E, 0x060A,
			0x000D, 0x0000,
	};
	
	private static final int[] TEST_DATA_REG_1636 = {
			0x0000, 0x0002,
			0x0001, 0x0000,
			0x0004, 0x0000,
	};
	
	private static final int[] TEST_DATA_REG_2012 = {
			0x0000, 0x0003,
			0x0004, 0x000B,
	};

	private static final int[] TEST_DATA_REG_3008 = {
			0x0000, 0x42DB,
			0x6A20, 0x3FDA,
			0x90FE, 0x3EAB,
			0x06E0, 0x3FAF,
			0xCF0B, 0x3FDA,
			0x90FE, 0x43F8,
			0x0A73, 0x43F6,
			0xC5B1, 0x43F6,
			0xEA0F, 0x43F7,
			0x3E11, 0x438E,
			0x86AF, 0x438E,
			0xF30F, 0x438E,
			0xCED0, 0x8000,
			0x8000, 0x438E,
			0xC2DA, 0x3EA5,
			0x5473, 0x3E42,
			0xBF84, 0x3E07,
			0xE961, 0x3EA5,
			0x5473, 0x3E28,
			0x9567, 0x3E07,
			0x1209, 0x3D06,
			0x0D76, 0x3E28,
			0x9567, 0x41F5,
			0x8370, 0x41FB,
			0x2E41, 0x41FD,
			0xAEF4, 0x42BB,
			0x9829, 0xBFDE,
			0x601E, 0x4008,
			0x8510, 0xBFED,
			0x2324, 0xBFBA,
			0x7921, 0x0000,
	};

	private static final int[] TEST_DATA_REG_3074 = {
			0x0000, 0x42BB,
			0xF20F, 0x3F80,
			0x3461, 0x3F7F,
			0x6947, 0x3F80,
			0x37C8, 0x3F80,
			0x3D3A, 0x3F80,
			0x0110, 0x3F7F,
			0xDEF3, 0x3F80,
			0x0165, 0x3F80,
			0x0654, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x4270,
			0x070C, 0x0000,
	};
	
	private static final int[] TEST_DATA_REG_3202 = {
			0x0000, 0x0000,
			0x0000, 0x046E,
			0x73EA, 0x0000,
			0x0000, 0x0001,
			0x6481, 0x0000,
			0x0000, 0x046F,
			0xD86B, 0x0000,
			0x0000, 0x046D,
			0x0F69, 0x0000,
			0x0000, 0x0027,
			0x90C1, 0x0000,
			0x0000, 0x000F,
			0xF38A, 0x0000,
			0x0000, 0x0037,
			0x844B, 0x0000,
			0x0000, 0x0017,
			0x9D37, 0x0000,
			0x0000, 0x0481,
			0xCE9A, 0x0000,
			0x0000, 0x0011,
			0xC261, 0x0000,			
	};
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(ION6200DataTests.class);

	private PM5100Data getTestDataInstance() {
		PM5100Data data = new PM5100Data();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_DATA_REG_28, 28);
				m.saveDataArray(TEST_DATA_REG_128, 128);
				m.saveDataArray(TEST_DATA_REG_1636, 1636);
				m.saveDataArray(TEST_DATA_REG_2012, 2012);
				m.saveDataArray(TEST_DATA_REG_3008, 3008);
				m.saveDataArray(TEST_DATA_REG_3074, 3074);
				m.saveDataArray(TEST_DATA_REG_3202, 3202);
				return true;
			}
		});
		return data;
	}

	@Test
	public void dataDebugString() {
		PM5100Data data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfo() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Model", data.getModel(), equalTo(PM5100Model.PM5330));
		assertThat("Firmware version", data.getFirmwareRevision(), equalTo("2.1.4"));
	}

	@Test
	public void interpretBasic() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(60.00688f));
		assertThat("Voltage", data.getVoltage(), equalTo(285.52228f));
		assertThat("Current", data.getCurrent(), equalTo(109.707275f));
		assertThat("Power factor", data.getPowerFactor(), equalTo(1.0018685f));
	}
}
