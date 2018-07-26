/* ==================================================================
 * Shark100DataTests.java - 26/07/2018 4:15:33 PM
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

package net.solarnetwork.node.hw.eig.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.eig.meter.Shark100Data;
import net.solarnetwork.node.hw.eig.meter.Shark100DataAccessor;
import net.solarnetwork.node.hw.eig.meter.SharkPowerEnergyFormat;
import net.solarnetwork.node.hw.eig.meter.SharkScale;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link Shark100Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class Shark100DataTests {

	// @formatter:off
	private static final int[] TEST_DATA_REG_1 = new int[] {
			0x4531,
			0x3431,
			0x2053,
			0x6861,
			0x726B,
			0x2031,
			0x3030,
			0x2020,
			0x3030,
			0x3532,
			0x3638,
			0x3438,
			0x3333,
			0x2020,
			0x2020,
			0x2020,
			0x0003,
			0x3030,
			0x3437,
			0x0010,
			0x003C,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x4550,
			0x4D36,
			0x3030,
			0x3020,
			0x2020,
			0x2020,
			0x2020,
			0x2020,
	};
	
	private static final int[] TEST_DATA_REG_900 = new int[] {
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
	};
	
	private static final int[] TEST_DATA_REG_1000 = new int[] {
			0x438D,
			0x0AAF,
			0x4393,
			0x52F1,
			0x4392,
			0x6927,
			0x43F9,
			0xC05C,
			0x43FE,
			0x60F5,
			0x43F8,
			0xF119,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x3F80,
			0x0000,
			0x4270,
			0x1584,
			0x0000,
			0x0000,			
	};
	
	private static final int[] TEST_DATA_REG_1100 = new int[] {
			0xFFE9, 0xA638, // Wh rec
			0x0000, 0x02BC, // Wh del
			0xFFE9, 0xA8F5, // Wh net
			0x0016, 0x5C85, // Wh tot
			0x0000, 0xDA5E, // VARh + (rec)
			0xFFFD, 0xE86F, // VARh - (del)
			0xFFFE, 0xC2CD, // VARh net
			0x0002, 0xF1EF, // VARh tot
			0x0016, 0xB49F, // VAh tot
	};
	
	private static final int[] TEST_DATA_REG_2000 = new int[] {
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x8000,
			0x0000,
			0x8000,
			0x0000,
			0x0000,
			0x0000,
			0x3F80,
			0x0000,
			0x3F80,
			0x0000,
	};
	
	private static final int[] TEST_DATA_REG_30000 = new int[] {
			0x0501,
			0x0BB8,
			0x0115,
			0x0115,
			0x0100,
			0x0F81,
			0x3362,
			0x00FF,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0384,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0026,
			0x0011,
			0x0020,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
	};
	
	private static final int[] TEST_DATA_REG_40000 = new int[] {
			0x0000,
			0x0000,
			0x1710,
			0x17B7,
			0x179F,
			0x07FF,
			0x07FF,
			0x07FF,
			0x07FF,
			0x07FF,
			0x07FF,
			0x0BE7,
			0x07FB,
			0x1554,
			0x1591,
			0x1549,
			0x0BB8,
			0x0001,
			0x0005,
			0x0115,
			0x0001,
			0x0115,
			0x0016,
			0x59C8,
			0x0000,
			0x02BC,
			0x0000,
			0xDA5E,
			0x0002,
			0x1791,
			0x0016,
			0xB49F,
	};
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(Shark100Data.class);

	private Shark100Data getTestDataInstance() {
		Shark100Data data = new Shark100Data();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_DATA_REG_1, 0);
				m.saveDataArray(TEST_DATA_REG_900, 899);
				m.saveDataArray(TEST_DATA_REG_1000, 999);
				m.saveDataArray(TEST_DATA_REG_1100, 1099);
				m.saveDataArray(TEST_DATA_REG_2000, 1999);
				m.saveDataArray(TEST_DATA_REG_30000, 29999);
				m.saveDataArray(TEST_DATA_REG_40000, 39999);
				return true;
			}
		});
		return data;
	}

	@Test
	public void dataDebugString() {
		Shark100Data data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfo() {
		Shark100DataAccessor data = getTestDataInstance();
		assertThat("Name", data.getName(), equalTo("E141 Shark 100"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("0052684833"));
		assertThat("Firmware version", data.getFirmwareRevision(), equalTo("0047"));
	}

	@Test
	public void powerEnergyFormat() {
		Shark100DataAccessor data = getTestDataInstance();
		assertThat("Power/energy format", data.getPowerEnergyFormat(),
				equalTo(new SharkPowerEnergyFormat(SharkScale.Kilo, 3, SharkScale.Mega, 2)));
	}

	@Test
	public void interpretBasic() {
		Shark100DataAccessor data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(60.02101f));
		assertThat("Voltage", data.getVoltage(), equalTo(289.85098f));
		assertThat("Current", data.getCurrent(), equalTo(0f));
		assertThat("Power factor", data.getPowerFactor(), equalTo(1.0f));
		assertThat("Energy received", data.getActiveEnergyReceived(), equalTo(-14647760000L));
		assertThat("Energy delivered", data.getActiveEnergyDelivered(), equalTo(7000000L));
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), equalTo(559020000L));
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(),
				equalTo(-1371050000L));
	}

}
