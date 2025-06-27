/* ==================================================================
 * PM5100RegisterTests.java - 17/05/2018 7:08:23 PM
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
import net.solarnetwork.node.hw.schneider.meter.PM5100Register;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link PM5330Register} class.
 * 
 * @author matt
 * @version 2.0
 */
public class PM5100RegisterTests {

	@Test
	public void intRangeSet() {
		// [29-48,89-89,128-134,1637-1638,1640-1640,2013-2015,2999-3010,3019-3032,3035-3036,3053-3060,3067-3068,3075-3076,3083-3084,3109-3110,3203-3210,3219-3226,3235-3242]
		IntRangeSet set = PM5100Register.getRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Register set length", ranges, hasSize(17));
	}

	@Test
	public void configIntRangeSet() {
		IntRangeSet set = PM5100Register.getConfigRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Ranges", ranges, contains(rangeOf(29, 48), rangeOf(89), rangeOf(128, 134),
				rangeOf(1637, 1638), rangeOf(1640, 1640), rangeOf(2013, 2015)));
	}

	@Test
	public void meterIntRangeSet() {
		IntRangeSet set = PM5100Register.getMeterRegisterAddressSet();
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Register set length", ranges, hasSize(11));
		List<IntRange> reduced = coveringIntRanges(set, 64);
		assertThat("Reduced ranges", reduced,
				contains(rangeOf(2999, 3060), rangeOf(3067, 3110), rangeOf(3203, 3242)));
	}

	@Test
	public void reducedIntRangeSet() {
		// [29-80,128-135,1637-1641,2013-2016,3009-3069,3075-3111,3203-3243]
		List<IntRange> ranges = coveringIntRanges(PM5100Register.getRegisterAddressSet(), 64);
		assertThat("Reduced ranges", ranges,
				contains(rangeOf(29, 89), rangeOf(128, 134), rangeOf(1637, 1640), rangeOf(2013, 2015),
						rangeOf(2999, 3060), rangeOf(3067, 3110), rangeOf(3203, 3242)));
	}
}
