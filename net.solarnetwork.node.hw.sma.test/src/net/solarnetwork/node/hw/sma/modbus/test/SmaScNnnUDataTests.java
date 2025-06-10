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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.Test;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceCommonDataAccessor;
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
	public void readInfo_1() throws IOException {
		// GIVEN
		SmaScNnnUData d = new SmaScNnnUData(SmaDeviceType.SunnyCentral250US);
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sc250us-01.txt");

		// WHEN
		d.readInformationData(conn);

		// THEN
		SmaScNnnUDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), notNullValue());
		assertThat("Device kind maintained", acc.getDeviceKind(),
				equalTo(SmaDeviceType.SunnyCentral250US));
		assertThat("Serial number", acc.getSerialNumber(), equalTo(0x0ACB5EC7l));
	}

	@Test
	public void readData_1() throws IOException {
		// GIVEN
		SmaScNnnUData d = new SmaScNnnUData(SmaDeviceType.SunnyCentral250US);
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sc250us-01.txt");

		// WHEN
		d.readDeviceData(conn);

		// THEN
		SmaScNnnUDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), notNullValue());
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

	@Test
	public void readData_2() throws IOException {
		// GIVEN
		SmaScNnnUData d = new SmaScNnnUData(SmaDeviceType.SunnyCentral250US);
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sc250us-02.txt");

		// WHEN
		d.readDeviceData(conn);

		// THEN
		SmaDeviceCommonDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), notNullValue());
		assertThat("Device kind maintained", acc.getDeviceKind(),
				equalTo(SmaDeviceType.SunnyCentral250US));

		assertThat("DeviceOperatingState", acc.getDeviceOperatingState(),
				equalTo(DeviceOperatingState.Normal));

		assertThat("Active energy exported", acc.getActiveEnergyExported(), equalTo(2578704900L));

		assertThat("Active power", acc.getActivePower(), equalTo(4600));
		assertThat("Active power max", acc.getActivePowerMaximum(), equalTo(250000));
		assertThat("Active power permanent limit", acc.getActivePowerPermanentLimit(), equalTo(250000));
		assertThat("Active power target", acc.getActivePowerTarget(), nullValue());
		assertThat("Apparent power", acc.getApparentPower(), nullValue());
		assertThat("Cabinet temp", acc.getCabinetTemperature(), equalTo(38.1f));
		assertThat("Current", acc.getCurrent(), equalTo(5.3f));
		assertThat("DC current", acc.getDcCurrent(), equalTo(15.5f));
		assertThat("DC power", acc.getDcPower(), equalTo(6400));
		assertThat("DC voltage", acc.getDcVoltage(), equalTo(415.4f));
		assertThat("External temp", acc.getExternalTemperature(), nullValue());
		assertThat("Feed-in time", acc.getFeedInTime(), equalTo(new BigInteger("120642120")));
		assertThat("Frequency", acc.getFrequency(), equalTo(59.97f));
		assertThat("Heat sink temp", acc.getHeatSinkTemperature(), equalTo(54.6f));
		assertThat("Voltage AB", acc.getLineVoltageLine1Line2(), equalTo(285.5f));
		assertThat("Voltage BC", acc.getLineVoltageLine2Line3(), equalTo(285.8f));
		assertThat("Voltage CA", acc.getLineVoltageLine3Line1(), equalTo(285.0f));
		assertThat("Operating time", acc.getOperatingTime(), equalTo(new BigInteger("259553880")));
		assertThat("Reactive power", acc.getReactivePower(), equalTo(0));
		assertThat("Voltage", acc.getVoltage(), equalTo(285.5f));
	}
}
