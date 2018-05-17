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

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.hw.schneider.meter.PM5100Register;
import net.solarnetwork.node.io.modbus.IntRangeSetUtils;

/**
 * Test cases for the {@link PM5330Register} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PM5100RegisterTests {

	@Test
	public void intRangeSet() {
		// [29-48,79-79,128-134,1637-1638,1640-1640,2013-2015,3009-3010,3035-3036,3059-3060,3067-3068,3075-3076,3083-3084,3109-3110,3203-3210,3219-3226,3235-3242]
		IntRangeSet set = PM5100Register.getRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(16));
	}

	@Test
	public void configIntRangeSet() {
		IntRangeSet set = PM5100Register.getConfigRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(6));
		IntRange[] ranges = set.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(29, 48)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(89, 89)));
		assertThat("Range 2", ranges[2], equalTo(new IntRange(128, 134)));
		assertThat("Range 3", ranges[3], equalTo(new IntRange(1637, 1638)));
		assertThat("Range 4", ranges[4], equalTo(new IntRange(1640, 1640)));
		assertThat("Range 5", ranges[5], equalTo(new IntRange(2013, 2015)));
	}

	@Test
	public void meterIntRangeSet() {
		IntRangeSet set = PM5100Register.getMeterRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(10));
		IntRangeSet reduced = IntRangeSetUtils.combineToReduceSize(set, 64);
		IntRange[] ranges = reduced.ranges();
		assertThat("Reduced register set length", ranges, arrayWithSize(3));
		assertThat("Range 0", ranges[0], equalTo(new IntRange(3009, 3068)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(3075, 3110)));
		assertThat("Range 2", ranges[2], equalTo(new IntRange(3203, 3242)));
	}

	@Test
	public void reducedIntRangeSet() {
		// [29-80,128-135,1637-1641,2013-2016,3009-3069,3075-3111,3203-3243]
		IntRangeSet set = IntRangeSetUtils.combineToReduceSize(PM5100Register.getRegisterAddressSet(),
				64);
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(7));
		IntRange[] ranges = set.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(29, 89)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(128, 134)));
		assertThat("Range 3", ranges[2], equalTo(new IntRange(1637, 1640)));
		assertThat("Range 2", ranges[3], equalTo(new IntRange(2013, 2015)));
		assertThat("Range 0", ranges[4], equalTo(new IntRange(3009, 3068)));
		assertThat("Range 1", ranges[5], equalTo(new IntRange(3075, 3110)));
		assertThat("Range 2", ranges[6], equalTo(new IntRange(3203, 3242)));
	}
}
