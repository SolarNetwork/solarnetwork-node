/* ==================================================================
 * IntRangeSetUtilsTests.java - 14/05/2018 6:56:22 PM
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

package net.solarnetwork.node.io.modbus.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.io.modbus.IntRangeSetUtils;

/**
 * Test cases for the {@link IntRangeSetUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IntRangeSetUtilsTests {

	@Test
	public void combineToReduceSizeReduced() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(0, 1);
		set.addAll(3, 5);
		set.addAll(100, 101);
		assertThat("Original range count", set.ranges(), arrayWithSize(3));
		IntRangeSet result = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("New copy returned", result, allOf(notNullValue(), not(sameInstance(set))));
		assertThat("New range count", result.ranges(), arrayWithSize(2));
		IntRange[] ranges = result.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 5)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(100, 101)));
	}

	@Test
	public void combineToReduceSizeNotReduced() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(0, 5);
		set.addAll(100, 101);
		assertThat("Original range count", set.ranges(), arrayWithSize(2));
		IntRangeSet result = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("New copy returned", result, allOf(notNullValue(), not(sameInstance(set))));
		assertThat("New range count", result.ranges(), arrayWithSize(2));
		IntRange[] ranges = result.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 5)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(100, 101)));
	}

	@Test
	public void javaDocExample() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(0, 1);
		set.addAll(3, 5);
		set.addAll(20, 28);
		set.addAll(404, 406);
		set.addAll(412, 418);
		IntRangeSet result = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("New copy returned", result, allOf(notNullValue(), not(sameInstance(set))));
		assertThat("New range count", result.ranges(), arrayWithSize(2));
		IntRange[] ranges = result.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 28)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(404, 418)));
	}

}
