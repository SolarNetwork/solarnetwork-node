/* ==================================================================
 * SmaSunnySensorboxDataTests.java - 15/09/2020 7:09:24 AM
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
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.domain.SmaSunnySensorboxDataAccessor;
import net.solarnetwork.node.hw.sma.modbus.SmaSunnySensorboxData;
import net.solarnetwork.node.hw.sma.test.TestUtils;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link SmaSunnySensorboxData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaSunnySensorboxDataTests {

	@Test
	public void readInfo_1() throws IOException {
		// GIVEN
		SmaSunnySensorboxData d = new SmaSunnySensorboxData();
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sunnysensorbox-01.txt");

		// WHEN
		d.readInformationData(conn);

		// THEN
		SmaSunnySensorboxDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), notNullValue());
		assertThat("Device kind maintained", acc.getDeviceKind(), equalTo(SmaDeviceType.SunnySensorbox));
		assertThat("Serial number", acc.getSerialNumber(), equalTo(0xA15CL));
		assertThat("Device class", acc.getDeviceClass(), equalTo(0x1F80L));
	}

	@Test
	public void readData_1() throws IOException {
		// GIVEN
		SmaSunnySensorboxData d = new SmaSunnySensorboxData();
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-sunnysensorbox-01.txt");

		// WHEN
		d.readDeviceData(conn);

		// THEN
		SmaSunnySensorboxDataAccessor acc = d;
		assertThat("Data updated", acc.getDataTimestamp(), notNullValue());
		assertThat("Device kind maintained", acc.getDeviceKind(), equalTo(SmaDeviceType.SunnySensorbox));

		assertThat("Device operating state not available", acc.getDeviceOperatingState(), nullValue());
		assertThat("External irradiance", acc.getExternalIrradiance(), equalTo(BigDecimal.ZERO));
		assertThat("Irradiance", acc.getIrradiance(), equalTo(new BigDecimal(0x0102)));
		assertThat("Module temperature", acc.getModuleTemperature(), equalTo(new BigDecimal("31.6")));
		assertThat("Ambient temperature", acc.getTemperature(), equalTo(new BigDecimal("-273.1")));
		assertThat("Wind speed", acc.getWindSpeed(), equalTo(new BigDecimal("0.0")));
	}

}
