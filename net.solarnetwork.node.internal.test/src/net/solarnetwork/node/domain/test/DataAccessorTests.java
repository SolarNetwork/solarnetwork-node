/* ==================================================================
 * DataAccessorTests.java - 13/10/2021 7:34:21 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.node.domain.DataAccessor;

/**
 * Test cases for the {@link DataAccessor} API.
 * 
 * @author matt
 * @version 1.0
 */
public class DataAccessorTests {

	private static final LocalDateTime TEST_MANUFACTURE_DATE = LocalDateTime.of(2021, 10, 13, 10, 11, 12,
			0);
	private static final String TEST_MANUFACTURER = "ACME";
	private static final String TEST_MODEL = "Super Mega Awesome 3000";
	private static final String TEST_NAME = "Foobar";
	private static final String TEST_SERIAL_NUMBER = "ABC123";
	private static final String TEST_VERSION = "1.2.3";

	@Test
	public void deviceInfoBuilder() {
		// GIVEN
		Map<String, Object> info = new HashMap<>(8);
		info.put(DataAccessor.INFO_KEY_DEVICE_MANUFACTURE_DATE, TEST_MANUFACTURE_DATE);
		info.put(DataAccessor.INFO_KEY_DEVICE_MANUFACTURER, TEST_MANUFACTURER);
		info.put(DataAccessor.INFO_KEY_DEVICE_MODEL, TEST_MODEL);
		info.put(DataAccessor.INFO_KEY_DEVICE_NAME, TEST_NAME);
		info.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, TEST_SERIAL_NUMBER);
		info.put(DataAccessor.INFO_KEY_DEVICE_VERSION, TEST_VERSION);

		// WHEN
		DeviceInfo result = DataAccessor.deviceInfoBuilderForInfo(info).build();

		// THEN
		assertThat("Date extracted", result.getManufactureDate(),
				is(TEST_MANUFACTURE_DATE.toLocalDate()));
		assertThat("Manufacturer copied", result.getManufacturer(), is(TEST_MANUFACTURER));
		assertThat("Name copied", result.getName(), is(TEST_NAME));
		assertThat("Model copied", result.getModelName(), is(TEST_MODEL));
		assertThat("Serial Number", result.getSerialNumber(), is(TEST_SERIAL_NUMBER));
		assertThat("Version copied", result.getVersion(), is(TEST_VERSION));
	}

}
