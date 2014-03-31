/* ==================================================================
 * PM3200DataTests.java - Apr 1, 2014 11:08:32 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@Link PM3200Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200DataTests {

	private static final int[] TEST_DATA_2999 = new int[] { 16443, 38526, 65472, 0, 65472, 0, 65472, 0,
			65472, 0, 16443, 38526 };
	private static final int[] TEST_DATA_3019 = new int[] { 65472, 0, 65472, 0, 65472, 0, 65472, 0,
			17260, 31128, 65472, 0, 65472, 0, 65472, 0, 17260, 31128 };
	private static final int[] TEST_DATA_3053 = new int[] { 16138, 19556, 65472, 0, 65472, 0, 16138,
			19556, 48862, 21990, 65472, 0, 65472, 0, 48862, 21990, 16177, 28793, 65472, 0, 65472, 0,
			16177, 28793, 16284, 15418, 65472, 0, 65472, 0, 16284, 15418 };
	private static final int[] TEST_DATA_3107 = new int[] { 48973, 51073, 16968, 12166, 65472, 0, 65472,
			0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 16835,
			55456 };
	private static final int[] TEST_DATA_3203 = new int[] { 0, 0, 7, 31493, 0, 0, 0, 0, 65472, 0, 65472,
			0, 65472, 0, 65472, 0, 0, 0, 0, 6526, 0, 0, 4, 369, 65472, 0, 65472, 0, 65472, 0, 65472, 0,
			0, 0, 8, 55045, 0, 0 };

	private static class TestPM3200Data extends PM3200Data {

		@Override
		public void saveDataArray(final int[] data, int addr) {
			super.saveDataArray(data, addr);
		}

	}

	private PM3200Data getTestDataInstance() {
		TestPM3200Data data = new TestPM3200Data();
		data.saveDataArray(TEST_DATA_2999, 2999);
		data.saveDataArray(TEST_DATA_3019, 3019);
		data.saveDataArray(TEST_DATA_3053, 3053);
		data.saveDataArray(TEST_DATA_3107, 3107);
		data.saveDataArray(TEST_DATA_3203, 3203);
		return data;
	}

	@Test
	public void interpretCurrent() {
		PM3200Data data = getTestDataInstance();
		Assert.assertEquals(2.931, data.getCurrent(PM3200Data.ADDR_DATA_I1), 0.001);
		Assert.assertEquals(2.931, data.getCurrent(PM3200Data.ADDR_DATA_I_AVERAGE), 0.001);
		Assert.assertEquals(236.474, data.getVoltage(PM3200Data.ADDR_DATA_V_L1_NEUTRAL), 0.001);
		Assert.assertEquals(236.474, data.getVoltage(PM3200Data.ADDR_DATA_V_NEUTRAL_AVERAGE), 0.001);
		Assert.assertEquals(541, (int) data.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P1));
		Assert.assertEquals(541, (int) data.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		Assert.assertEquals(694, (int) data.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_P1));
		Assert.assertEquals(694, (int) data.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_TOTAL));
	}

	@Test
	public void interpretVoltage() {
		PM3200Data data = getTestDataInstance();
		Assert.assertEquals(236.474, data.getVoltage(PM3200Data.ADDR_DATA_V_L1_NEUTRAL), 0.001);
		Assert.assertEquals(236.474, data.getVoltage(PM3200Data.ADDR_DATA_V_NEUTRAL_AVERAGE), 0.001);
	}

	@Test
	public void interpretPower() {
		PM3200Data data = getTestDataInstance();
		Assert.assertEquals(541, (int) data.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P1));
		Assert.assertEquals(541, (int) data.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		Assert.assertEquals(694, (int) data.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_P1));
		Assert.assertEquals(694, (int) data.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_TOTAL));
	}

	@Test
	public void interpretPowerFactor() {
		PM3200Data data = getTestDataInstance();
		Assert.assertEquals(1.220588, data.getPowerFactor(PM3200Data.ADDR_DATA_POWER_FACTOR_P1), 0.001);
		Assert.assertEquals(1.220588, data.getPowerFactor(PM3200Data.ADDR_DATA_POWER_FACTOR_TOTAL),
				0.001);
		Assert.assertEquals(-0.80382544,
				data.getPowerFactor(PM3200Data.ADDR_DATA_REACTIVE_FACTOR_TOTAL), 0.001);
		Assert.assertEquals(-0.77941227, data.getEffectiveTotalPowerFactor(), 0.001);
	}
}
