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
		// [29-49,79-80,128-135,1637-1641,2013-2016, 3009-3011,3035-3037,3059-3061,3067-3069,3075-3077,3083-3085,3109-3111,3203-3211,3219-3227,3235-3243]
		IntRangeSet set = PM5100Register.getRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(15));
	}

	@Test
	public void configIntRangeSet() {
		IntRangeSet set = PM5100Register.getConfigRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(5));
		IntRange[] ranges = set.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(29, 49)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(79, 80)));
		assertThat("Range 2", ranges[2], equalTo(new IntRange(128, 135)));
		assertThat("Range 3", ranges[3], equalTo(new IntRange(1637, 1641)));
		assertThat("Range 3", ranges[4], equalTo(new IntRange(2013, 2016)));
	}

	@Test
	public void meterIntRangeSet() {
		IntRangeSet set = PM5100Register.getMeterRegisterAddressSet();
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(10));
		IntRangeSet reduced = IntRangeSetUtils.combineToReduceSize(set, 64);
		IntRange[] ranges = reduced.ranges();
		assertThat("Reduced register set length", ranges, arrayWithSize(3));
		assertThat("Range 0", ranges[0], equalTo(new IntRange(3009, 3069)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(3075, 3111)));
		assertThat("Range 2", ranges[2], equalTo(new IntRange(3203, 3243)));
	}

	@Test
	public void reducedIntRangeSet() {
		// [29-80,128-135,1637-1641,2013-2016,3009-3069,3075-3111,3203-3243]
		IntRangeSet set = IntRangeSetUtils.combineToReduceSize(PM5100Register.getRegisterAddressSet(),
				64);
		assertThat("Register set", notNullValue());
		assertThat("Register set length", set.ranges(), arrayWithSize(7));
		IntRange[] ranges = set.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(29, 80)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(128, 135)));
		assertThat("Range 3", ranges[2], equalTo(new IntRange(1637, 1641)));
		assertThat("Range 2", ranges[3], equalTo(new IntRange(2013, 2016)));
		assertThat("Range 0", ranges[4], equalTo(new IntRange(3009, 3069)));
		assertThat("Range 1", ranges[5], equalTo(new IntRange(3075, 3111)));
		assertThat("Range 2", ranges[6], equalTo(new IntRange(3203, 3243)));
	}
}
