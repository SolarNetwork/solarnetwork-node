/* ==================================================================
 * CozIrUtilsTests.java - 27/08/2020 4:58:48 PM
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.hw.gss.co2.CozIrUtils;

/**
 * Test cases for the {@link CozIrUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrUtilsTests {

	@Test
	public void parseKeyValueIntegerLine_typical() {
		Map<String, Integer> data = CozIrUtils
				.parseKeyValueIntegerLine(" H 00408 T 01218 Z 01294 z 01297", 10);
		assertThat("Parsed 4 keys maintain order", data.keySet(), contains("H", "T", "Z", "z"));
		assertThat("Parsed H value", data, hasEntry("H", 408));
		assertThat("Parsed T value", data, hasEntry("T", 1218));
		assertThat("Parsed Z value", data, hasEntry("Z", 1294));
		assertThat("Parsed z value", data, hasEntry("z", 1297));
	}

	@Test
	public void parseKeyValueIntegerLine_null() {
		Map<String, Integer> data = CozIrUtils.parseKeyValueIntegerLine(null, 10);
		assertThat("Empty map returned on null input", data.keySet(), hasSize(0));
	}

	@Test
	public void parseKeyValueIntegerLine_empty() {
		Map<String, Integer> data = CozIrUtils.parseKeyValueIntegerLine("", 10);
		assertThat("Empty map returned on empty input", data.keySet(), hasSize(0));
	}

	@Test
	public void parseKeyValueIntegerLine_onlyWhitespace() {
		Map<String, Integer> data = CozIrUtils.parseKeyValueIntegerLine("   \t    \n  \r", 10);
		assertThat("Empty map returned on whitespace only input", data.keySet(), hasSize(0));
	}

	@Test
	public void parseKeyValueIntegerLine_onlyKey() {
		Map<String, Integer> data = CozIrUtils.parseKeyValueIntegerLine("H", 10);
		assertThat("Empty map returned on key only input", data.keySet(), hasSize(0));
	}

	@Test
	public void parseKeyValueIntegerLine_danglingKey() {
		Map<String, Integer> data = CozIrUtils.parseKeyValueIntegerLine("H 123 T", 10);
		assertThat("Parsed 1 keys, ignoring dangling key", data.keySet(), contains("H"));
		assertThat("Parsed H value", data, hasEntry("H", 123));
	}

	@Test
	public void parseSerialNumberMessage_typical() {
		String s = CozIrUtils.parseSerialNumberMessage(" B 412755 00000\r\n");
		assertThat("Serial number parsed", s, equalTo("412755.0"));
	}

	@Test
	public void altCompForAltitude() {
		assertThat("Alt comp for 0ft", CozIrUtils.altitudeCompensationValueForAltitudeInFeet(0),
				equalTo(8192));
		assertThat("Alt comp for 500ft", CozIrUtils.altitudeCompensationValueForAltitudeInFeet(500),
				equalTo(8400));
		assertThat("Alt comp for 2000ft", CozIrUtils.altitudeCompensationValueForAltitudeInFeet(2000),
				equalTo(9005));
		assertThat("Alt comp for 5000ft", CozIrUtils.altitudeCompensationValueForAltitudeInFeet(5000),
				equalTo(10142));
		assertThat("Alt comp for 10000ft", CozIrUtils.altitudeCompensationValueForAltitudeInFeet(10000),
				equalTo(11814));
	}

}
