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

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.hw.advantech.adam.ADAM411xRegister;
import net.solarnetwork.node.io.modbus.IntRangeSetUtils;

/**
 * Test cases for the {@link ADAM411xRegister} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ADAM411xRegisterTests {

	@Test
	public void channelRegisterSet() {
		IntRangeSet set = ADAM411xRegister.getChannelRegisterAddressSet();
		IntRangeSet combined = IntRangeSetUtils.combineToReduceSize(set, 8);
		assertThat("Range set", combined.ranges(), arrayWithSize(1));
		assertThat("Range min", combined.first(), equalTo(0));
		assertThat("Range last", combined.last(), equalTo(7));
	}

	@Test
	public void configRegisterSet() {
		IntRangeSet set = ADAM411xRegister.getConfigRegisterAddressSet();
		IntRangeSet combined = IntRangeSetUtils.combineToReduceSize(set, 1);
		assertThat("Range set", combined.ranges(), arrayWithSize(3));
		for ( int i = 0; i < combined.ranges().length; i++ ) {
			IntRange r = combined.ranges()[i];
			switch (i) {
				case 0:
					assertThat("Range min", r.first(), equalTo(200));
					assertThat("Range last", r.last(), equalTo(207));
					break;

				case 1:
					assertThat("Range min", r.first(), equalTo(210));
					assertThat("Range last", r.last(), equalTo(213));
					break;

				case 2:
					assertThat("Range min", r.first(), equalTo(220));
					assertThat("Range last", r.last(), equalTo(220));
					break;

				default:
					fail("Unexpected range " + i);
			}
		}
	}

	@Test
	public void allRegisterSet() {
		IntRangeSet set = ADAM411xRegister.getRegisterAddressSet();
		IntRangeSet combined = IntRangeSetUtils.combineToReduceSize(set, 1);
		assertThat("Range set", combined.ranges(), arrayWithSize(4));
		for ( int i = 0; i < combined.ranges().length; i++ ) {
			IntRange r = combined.ranges()[i];
			switch (i) {
				case 0:
					assertThat("Range min", r.first(), equalTo(0));
					assertThat("Range last", r.last(), equalTo(7));
					break;

				case 1:
					assertThat("Range min", r.first(), equalTo(200));
					assertThat("Range last", r.last(), equalTo(207));
					break;

				case 2:
					assertThat("Range min", r.first(), equalTo(210));
					assertThat("Range last", r.last(), equalTo(213));
					break;

				case 3:
					assertThat("Range min", r.first(), equalTo(220));
					assertThat("Range last", r.last(), equalTo(220));
					break;

				default:
					fail("Unexpected range " + i);
			}
		}
	}

}
