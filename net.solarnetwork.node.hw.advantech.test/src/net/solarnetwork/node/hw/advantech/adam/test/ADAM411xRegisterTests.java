/* ==================================================================
 * ADAM411xRegisterTests.java - 23/11/2018 6:59:44 AM
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

import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.advantech.adam.ADAM411xRegister;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link ADAM411xRegister} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ADAM411xRegisterTests {

	@Test
	public void channelRegisterSet() {
		IntRangeSet set = ADAM411xRegister.getChannelRegisterAddressSet();
		List<IntRange> combined = CollectionUtils.coveringIntRanges(set, 8);
		assertThat("Range set", combined, contains(rangeOf(0, 7)));
	}

	@Test
	public void configRegisterSet() {
		IntRangeSet set = ADAM411xRegister.getConfigRegisterAddressSet();
		assertThat("Range set", set.ranges(),
				contains(rangeOf(200, 207), rangeOf(210, 213), rangeOf(220)));
	}

	@Test
	public void allRegisterSet() {
		IntRangeSet set = ADAM411xRegister.getRegisterAddressSet();
		assertThat("Range set", set.ranges(),
				contains(rangeOf(0, 7), rangeOf(200, 207), rangeOf(210, 213), rangeOf(220)));
	}

}
