/* ==================================================================
 * InputRangeTypeTests.java - 21/11/2018 9:05:34 PM
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

package net.solarnetwork.node.hw.advantech.adam.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.Test;
import net.solarnetwork.node.hw.advantech.adam.InputRangeType;

/**
 * Test cases for the {@link InputRangeType} enum.
 * 
 * @author matt
 * @version 1.0
 */
public class InputRangeTypeTests {

	@Test
	public void thermNormalizedValue_J() {
		InputRangeType type = InputRangeType.TypeJThermocouple;
		assertThat("Small value", type.normalizedDataValue(0x0), equalTo(new BigDecimal("0")));
		assertThat("Middle value", type.normalizedDataValue(0x4000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("380.012")));
		assertThat("Big value", type.normalizedDataValue(0x7FFF), equalTo(new BigDecimal("760")));
	}

	@Test
	public void thermNormalizedValue_R() {
		InputRangeType type = InputRangeType.TypeRThermocouple;
		assertThat("Small value", type.normalizedDataValue(0x2492), equalTo(new BigDecimal("500")));
		assertThat("Middle value", type.normalizedDataValue(0x5249).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("1125.027")));
		assertThat("Big value", type.normalizedDataValue(0x7FFF), equalTo(new BigDecimal("1750")));
	}

	@Test
	public void thermNormalizedValue_T() {
		InputRangeType type = InputRangeType.TypeTThermocouple;
		assertThat("Small value", type.normalizedDataValue((short) 0xE000),
				equalTo(new BigDecimal("-100")));
		assertThat("Middle value", type.normalizedDataValue(0x3000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("150.005")));
		assertThat("Big value", type.normalizedDataValue(0x7FFF), equalTo(new BigDecimal("400")));
	}

}
