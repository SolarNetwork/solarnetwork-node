/* ==================================================================
 * SubCombinerRegisterTests.java - 8/11/2019 10:20:12 am
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
import net.solarnetwork.node.hw.satcon.SubCombinerRegister;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link SubCombinerRegister} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SubCombinerRegisterTests {

	@Test
	public void inverterRegisterSet() {
		IntRangeSet set = SubCombinerRegister.getRegisterAddressSet();
		List<IntRange> norm = CollectionUtils.coveringIntRanges(set, 64);
		assertThat("Inverter registers", norm, contains(rangeOf(30010, 30073), rangeOf(30074, 30099),
				rangeOf(36061, 36112), rangeOf(36131, 36172)));
	}

}
