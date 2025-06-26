/* ==================================================================
 * FirmwareVersionTests.java - 28/08/2020 7:16:55 AM
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

package net.solarnetwork.node.hw.gss.co2.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import net.solarnetwork.node.hw.gss.co2.FirmwareVersion;

/**
 * Test cases for the {@link FirmwareVersion} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FirmwareVersionTests {

	@Test
	public void parse_typical() {
		FirmwareVersion v = FirmwareVersion.parseMessage(" Y,Oct 25 2016,13:24:49,AL22\r\n");
		assertThat("FirmwareVersion is instantiated", v, notNullValue());
		assertThat("Firmware date parsed", v.getDate(), equalTo(
				LocalDateTime.parse("2016-10-25T13:24:49", DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
		assertThat("Firmware version parsed", v.getVersion(), equalTo("AL22"));
	}

}
