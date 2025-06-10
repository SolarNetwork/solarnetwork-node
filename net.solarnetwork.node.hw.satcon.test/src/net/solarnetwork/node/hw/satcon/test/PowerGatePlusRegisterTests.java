/* ==================================================================
 * PowerGatePlusRegisterTests.java - 8/11/2019 9:50:42 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.satcon.test;

import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.satcon.PowerGatePlusRegister;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link PowerGatePlusRegister} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerGatePlusRegisterTests {

	@Test
	public void configRegisterSet() {
		IntRangeSet set = PowerGatePlusRegister.getConfigRegisterAddressSet();
		List<IntRange> norm = CollectionUtils.coveringIntRanges(set, 64);
		assertThat("Inverter registers", norm, contains(rangeOf(9, 19), rangeOf(301, 327)));
	}

	@Test
	public void inverterRegisterSet() {
		IntRangeSet set = PowerGatePlusRegister.getInverterRegisterAddressSet();
		List<IntRange> norm = CollectionUtils.coveringIntRanges(set, 64);
		assertThat("Inverter registers", norm,
				contains(rangeOf(10, 49), rangeOf(130, 175), rangeOf(279, 291)));
	}

	@Test
	public void controlRegisterSet() {
		IntRangeSet set = PowerGatePlusRegister.getControlRegisterAddressSet();
		List<IntRange> norm = CollectionUtils.coveringIntRanges(set, 64);
		assertThat("Inverter registers", norm, contains(rangeOf(436, 475)));
	}

	@Test
	public void registerSet() {
		IntRangeSet set = PowerGatePlusRegister.getRegisterAddressSet();
		List<IntRange> norm = CollectionUtils.coveringIntRanges(set, 64);
		assertThat("Inverter registers", norm,
				contains(rangeOf(9, 49), rangeOf(130, 175), rangeOf(279, 327)));
	}

	@Test
	public void registerSetCombined() {
		IntRangeSet set = PowerGatePlusRegister.getRegisterAddressSet();
		set.addAll(PowerGatePlusRegister.getControlRegisterAddressSet());
		List<IntRange> norm = CollectionUtils.coveringIntRanges(set, 64);
		assertThat("Inverter registers", norm,
				contains(rangeOf(9, 49), rangeOf(130, 175), rangeOf(279, 327), rangeOf(436, 475)));
	}

}
