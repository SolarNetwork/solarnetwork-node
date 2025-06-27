/* ==================================================================
 * ION6200RegisterTests.java - 14/05/2018 6:31:30 PM
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

package net.solarnetwork.node.hw.schneider.test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.schneider.meter.ION6200Register;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link ION6200Register} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ION6200RegisterTests {

	@Test
	public void intRangeSet() {
		// [0-2,12-12,99-110,114-115,119-121,137-144,4000-4000,4011-4014]
		IntRangeSet set = ION6200Register.getRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Register set length", ranges, hasSize(8));
	}

	@Test
	public void configIntRangeSet() {
		IntRangeSet set = ION6200Register.getConfigRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Ranges", ranges,
				contains(rangeOf(0, 2), rangeOf(12, 12), rangeOf(4000, 4000), rangeOf(4011, 4014)));
	}

	@Test
	public void meterIntRangeSet() {
		IntRangeSet set = ION6200Register.getMeterRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Register set length", ranges, hasSize(5));
		List<IntRange> reduced = coveringIntRanges(set, 64);
		assertThat("Reduced ranges", reduced, contains(rangeOf(99, 144), rangeOf(4011, 4014)));
	}

	@Test
	public void reducedIntRangeSet() {
		// [0-12,102-144,4000-4014]
		List<IntRange> ranges = coveringIntRanges(ION6200Register.getRegisterAddressSet(), 64);
		assertThat("Reduced ranges", ranges,
				contains(rangeOf(0, 12), rangeOf(99, 144), rangeOf(4000, 4014)));
	}
}
