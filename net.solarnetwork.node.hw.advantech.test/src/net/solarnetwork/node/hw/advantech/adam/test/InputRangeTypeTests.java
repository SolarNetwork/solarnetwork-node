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
import static org.hamcrest.MatcherAssert.assertThat;
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
	public void plusMinusFifteenMilliVolts() {
		InputRangeType type = InputRangeType.PlusMinusFifteenMilliVolts;
		assertThat("Small value", type.normalizedDataValue(0x0000), equalTo(new BigDecimal("-0.015")));
		assertThat("Middle value", type.normalizedDataValue(0x8000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("0.000")));
		assertThat("Big value", type.normalizedDataValue(0xFFFF), equalTo(new BigDecimal("0.015")));

	}

	@Test
	public void plusMinusOneVolts() {
		InputRangeType type = InputRangeType.PlusMinusOneVolts;
		assertThat("Small value", type.normalizedDataValue((short) 0x0000),
				equalTo(new BigDecimal("-1")));
		assertThat("Middle value", type.normalizedDataValue(0x8000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("0.000")));
		assertThat("Big value", type.normalizedDataValue(0xFFFF), equalTo(new BigDecimal("1")));

	}

	@Test
	public void fourToTwentyMilliAmps() {
		InputRangeType type = InputRangeType.FourToTwentyMilliAmps;
		assertThat("Small value", type.normalizedDataValue((short) 0x0000),
				equalTo(new BigDecimal("0.004")));
		assertThat("Middle value", type.normalizedDataValue(0x8000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("0.012")));
		assertThat("Big value", type.normalizedDataValue(0xFFFF), equalTo(new BigDecimal("0.020")));

	}

	@Test
	public void thermNormalizedValue_J() {
		InputRangeType type = InputRangeType.TypeJThermocouple;
		assertThat("Small value", type.normalizedDataValue(0x0000), equalTo(new BigDecimal("0")));
		assertThat("Middle value", type.normalizedDataValue(0x8000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("380.006")));
		assertThat("Big value", type.normalizedDataValue(0xFFFF), equalTo(new BigDecimal("760")));
	}

	@Test
	public void thermNormalizedValue_R() {
		InputRangeType type = InputRangeType.TypeRThermocouple;
		assertThat("Small value", type.normalizedDataValue(0x0000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("500.000")));
		assertThat("Middle value", type.normalizedDataValue(0x8000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("1125.010")));
		assertThat("Big value", type.normalizedDataValue(0xFFFF), equalTo(new BigDecimal("1750")));
	}

	@Test
	public void thermNormalizedValue_T() {
		InputRangeType type = InputRangeType.TypeTThermocouple;
		assertThat("Small value", type.normalizedDataValue((short) 0x0000),
				equalTo(new BigDecimal("-100")));
		assertThat("Middle value", type.normalizedDataValue(0x8000).setScale(3, RoundingMode.HALF_UP),
				equalTo(new BigDecimal("150.004")));
		assertThat("Big value", type.normalizedDataValue(0xFFFF), equalTo(new BigDecimal("400")));
	}

}
