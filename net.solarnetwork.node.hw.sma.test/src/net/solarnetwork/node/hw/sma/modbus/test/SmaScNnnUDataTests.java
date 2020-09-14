/* ==================================================================
 * SmaScNnnUDataTests.java - 14/09/2020 4:25:45 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.domain.SmaScNnnUDataAccessor;
import net.solarnetwork.node.hw.sma.modbus.SmaScNnnUData;
import net.solarnetwork.node.hw.sma.test.TestUtils;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link SmaScNnnUData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaScNnnUDataTests {

	@Test
	public void readInfo_1() {
		// GIVEN
		SmaScNnnUData d = new SmaScNnnUData(SmaDeviceType.SunnyCentral250US);
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sc250us-01.txt");

		// WHEN
		d.readInformationData(conn);

		// THEN
		SmaScNnnUDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), greaterThan(0L));
		assertThat("Device kind maintained", acc.getDeviceKind(),
				equalTo(SmaDeviceType.SunnyCentral250US));
		assertThat("Serial number", acc.getSerialNumber(), equalTo(0x0ACB5EC7l));
	}

	@Test
	public void readData_1() {
		// GIVEN
		SmaScNnnUData d = new SmaScNnnUData(SmaDeviceType.SunnyCentral250US);
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sc250us-01.txt");

		// WHEN
		d.readDeviceData(conn);

		// THEN
		SmaScNnnUDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), greaterThan(0L));
		assertThat("Device kind maintained", acc.getDeviceKind(),
				equalTo(SmaDeviceType.SunnyCentral250US));

		assertThat("DeviceOperatingState", acc.getDeviceOperatingState(),
				equalTo(DeviceOperatingState.Normal));
		assertThat("Error NotSet mapped to null", acc.getError(), nullValue());
		assertThat("Grid contactor status", acc.getGridContactorStatus(),
				equalTo(SmaCommonStatusCode.Open));
		assertThat("Reconnect time", acc.getGridReconnectTime(), equalTo(0L));
		assertThat("Operating state", acc.getOperatingState(), equalTo(SmaCommonStatusCode.Operation));
		assertThat("Recommended action Invalid mapped to null", acc.getRecommendedAction(), nullValue());
	}
}
