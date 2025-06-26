/* ==================================================================
 * WattsOnRegisterTests.java - 14/08/2020 10:36:52 AM
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

package net.solarnetwork.node.hw.elkor.upt.test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.elkor.upt.WattsOnRegister;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link WattsOnRegister} class.
 * 
 * @author matt
 * @version 1.0
 */
public class WattsOnRegisterTests {

	@Test
	public void intRangeSet() {
		// [128..131], [149..149], [770..785], [788..887]
		IntRangeSet set = WattsOnRegister.getRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Register set length", ranges, hasSize(4));
	}

	@Test
	public void configIntRangeSet() {
		IntRangeSet set = WattsOnRegister.getConfigRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Ranges", ranges, contains(rangeOf(128, 131), rangeOf(149, 149), rangeOf(830, 831)));
	}

	@Test
	public void meterIntRangeSet() {
		IntRangeSet set = WattsOnRegister.getMeterRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Register set length", ranges, hasSize(4));
		List<IntRange> reduced = coveringIntRanges(set, 64);
		assertThat("Reduced ranges", reduced,
				contains(rangeOf(128, 131), rangeOf(770, 833), rangeOf(834, 887)));
	}

	@Test
	public void reducedIntRangeSet() {
		// [128..149], [770..833], [834..887]
		List<IntRange> ranges = coveringIntRanges(WattsOnRegister.getRegisterAddressSet(), 64);
		assertThat("Reduced ranges", ranges,
				contains(rangeOf(128, 149), rangeOf(770, 833), rangeOf(834, 887)));
	}

}
