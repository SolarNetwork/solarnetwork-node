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

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.hw.schneider.meter.ION6200Register;
import net.solarnetwork.node.io.modbus.IntRangeSetUtils;

/**
 * Test cases for the {@link ION6200Register} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ION6200RegisterTests {

	@Test
	public void intRangeSet() {
		// [0-3,12-13,102-103,110-111,114-116,119-122,137-145,4000-4001,4011-4015]
		IntRangeSet set = ION6200Register.getRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(9));
	}

	@Test
	public void configIntRangeSet() {
		IntRangeSet set = ION6200Register.getConfigRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(4));
		IntRange[] ranges = set.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 3)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(12, 13)));
		assertThat("Range 2", ranges[2], equalTo(new IntRange(4000, 4001)));
		assertThat("Range 3", ranges[3], equalTo(new IntRange(4011, 4015)));
	}

	@Test
	public void meterIntRangeSet() {
		IntRangeSet set = ION6200Register.getMeterRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(5));
		IntRangeSet reduced = IntRangeSetUtils.combineToReduceSize(set, 64);
		IntRange[] ranges = reduced.ranges();
		assertThat("Register set length", ranges, arrayWithSize(1));
		assertThat("Range 0", ranges[0], equalTo(new IntRange(102, 145)));
	}

	@Test
	public void reducedIntRangeSet() {
		// [0-13,102-145,4000-4015]
		IntRangeSet set = IntRangeSetUtils.combineToReduceSize(ION6200Register.getRegisterAddressSet(),
				64);
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(3));
		IntRange[] ranges = set.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 13)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(102, 145)));
		assertThat("Range 2", ranges[2], equalTo(new IntRange(4000, 4015)));
	}
}
