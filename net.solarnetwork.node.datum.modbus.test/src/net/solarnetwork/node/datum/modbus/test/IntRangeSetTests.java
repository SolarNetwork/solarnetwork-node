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

import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertThat;
import org.hamcrest.Matchers;
import org.junit.Test;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;

/**
 * Test cases for how we expect to use {@link IntRangeSet}.
 * 
 * @author matt
 * @version 1.0
 */
public class IntRangeSetTests {

	@Test
	public void nonOrderedInputOrderedAndCombined() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(3109, 3109 + 2);
		set.addAll(3059, 3059 + 2);
		set.addAll(3009, 3009 + 2);
		set.addAll(3035, 3035 + 2);
		set.addAll(3203, 3203 + 4);
		set.addAll(3207, 3207 + 4);
		IntRange[] ranges = set.ranges();
		assertThat(ranges, arrayWithSize(5));
		assertThat("Range 0", ranges[0], Matchers.equalTo(new IntRange(3009, 3011)));
		assertThat("Range 1", ranges[1], Matchers.equalTo(new IntRange(3035, 3037)));
		assertThat("Range 2", ranges[2], Matchers.equalTo(new IntRange(3059, 3061)));
		assertThat("Range 3", ranges[3], Matchers.equalTo(new IntRange(3109, 3111)));
		assertThat("Range 4", ranges[4], Matchers.equalTo(new IntRange(3203, 3211)));
	}

}
