/* ==================================================================
 * SimpleDemandBalanceStrategyTests.java - Jul 22, 2014 3:59:49 PM
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

package net.solarnetwork.node.control.demandbalancer.test;

import org.junit.Assert;
import org.junit.Test;
import net.solarnetwork.node.control.demandbalancer.SimpleDemandBalanceStrategy;

/**
 * Test cases for the {@link SimpleDemandBalanceStrategy} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDemandBalanceStrategyTests {

	private static final String TEST_CONTROL_ID = "/test";

	@Test
	public void demandLessCapcity() {
		SimpleDemandBalanceStrategy impl = new SimpleDemandBalanceStrategy();
		int result = impl.evaluateBalance(TEST_CONTROL_ID, 10, 100, 100, 100);
		Assert.assertEquals(10, result);
	}

	@Test
	public void demandEqualCapacity() {
		SimpleDemandBalanceStrategy impl = new SimpleDemandBalanceStrategy();
		int result = impl.evaluateBalance(TEST_CONTROL_ID, 100, 100, 100, 100);
		Assert.assertEquals(100, result);
	}

	@Test
	public void demandGreaterCapacity() {
		SimpleDemandBalanceStrategy impl = new SimpleDemandBalanceStrategy();
		int result = impl.evaluateBalance(TEST_CONTROL_ID, 200, 100, 100, 100);
		Assert.assertEquals(100, result);
	}

	@Test
	public void demandUnknownNoChange() {
		SimpleDemandBalanceStrategy impl = new SimpleDemandBalanceStrategy();
		impl.setUnknownDemandLimit(-1);
		int result = impl.evaluateBalance(TEST_CONTROL_ID, -1, 100, 100, 100);
		Assert.assertEquals(-1, result);
	}

	@Test
	public void demandUnknownEnforceLimit() {
		SimpleDemandBalanceStrategy impl = new SimpleDemandBalanceStrategy();
		impl.setUnknownDemandLimit(5);
		int result = impl.evaluateBalance(TEST_CONTROL_ID, -1, 100, 100, 100);
		Assert.assertEquals(5, result);
	}
}
