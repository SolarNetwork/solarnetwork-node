/* ==================================================================
 * IntRangeSetTests.java - 8/05/2018 11:17:47 AM
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

package net.solarnetwork.node.datum.modbus.test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for how we expect to use {@link IntRangeSet}.
 * 
 * @author matt
 * @version 2.0
 */
public class IntRangeSetTests {

	@Test
	public void nonOrderedInputOrderedAndCombined() {
		IntRangeSet set = new IntRangeSet();
		set.addRange(3109, 3109 + 2);
		set.addRange(3059, 3059 + 2);
		set.addRange(3009, 3009 + 2);
		set.addRange(3035, 3035 + 2);
		set.addRange(3203, 3203 + 4);
		set.addRange(3207, 3207 + 4);
		List<IntRange> ranges = stream(set.ranges().spliterator(), false).collect(toList());
		assertThat("Ranges", ranges, contains(rangeOf(3009, 3011), rangeOf(3035, 3037),
				rangeOf(3059, 3061), rangeOf(3109, 3111), rangeOf(3203, 3211)));
	}

}
