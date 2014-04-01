/* ==================================================================
 * EM5600SupportTests.java - Mar 28, 2014 2:37:21 PM
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

package net.solarnetwork.node.hw.hc.test;

import net.solarnetwork.node.hw.hc.EM5600Data;
import net.solarnetwork.node.hw.hc.EM5600Support;
import net.solarnetwork.node.hw.hc.UnitFactor;
import net.solarnetwork.node.test.AbstractNodeTest;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link EM5600Support} class.
 * 
 * @author matt
 * @version 1.0
 */
public class EM5600SupportTests extends AbstractNodeTest {

	@Test
	public void testParseDate() {
		final int[] data = new int[] { 0x1200, 0xD03 };
		LocalDateTime d = EM5600Support.parseDate(data);
		Assert.assertNotNull("Date should parse", d);
		Assert.assertEquals("Parsed date", new LocalDateTime(2013, 3, 18, 0, 0, 0), d);
	}

	@Test
	public void testParse5630Sample30A() {
		final int[] data1 = new int[] { 195, 175, 137, 168, 0, 0, 23929, 23929, 0, 15952, 0, 0, 0, 0, 0,
				0, 3, 0, 44, 750, 28061, 1, 1, 23, 752, 1, 0, 20, -747, 0, 0, 0 };
		final int[] data2 = new int[] { 0, 10926, 0, 0, 0, 20, 0, 131, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
		final EM5600Data d = new EM5600Data();
		d.setCurrentVoltagePower(data1);
		d.setEnergy(data2);
		d.setPtRatio(1);
		d.setCtRatio(25);
		d.setEnergyUnit(1);
		d.setUnitFactor(UnitFactor.EM5630_30A);
		float i = d.getCurrent(EM5600Data.ADDR_DATA_I_AVERAGE);
		Assert.assertEquals(0.504, i, 0.001);
		int p = d.getPower(EM5600Data.ADDR_DATA_ACTIVE_POWER_TOTAL);
		Assert.assertEquals(18, p);
		long e = d.getEnergy(EM5600Data.ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT);
		Assert.assertEquals(109260, e);
		float pf = d.getPowerFactor(EM5600Data.ADDR_DATA_POWER_FACTOR_TOTAL);
		Assert.assertEquals(0.075, pf, 0.0001);
	}
}
