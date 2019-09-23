/* ==================================================================
 * FilterMessageImplTests.java - 23/09/2019 1:51:50 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.socketcand.msg.test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.msg.FilterMessageImpl;

/**
 * Test cases for the {@link FilterMessageImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FilterMessageImplTests {

	@Test
	public void construct_list() {
		// GIVEN
		List<String> arguments = asList("1", "500000", "123", "1", "FF");

		// WHEN
		FilterMessageImpl m = new FilterMessageImpl(arguments);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Seconds", m.getSeconds(), equalTo(1));
		assertThat("Microseconds", m.getMicroseconds(), equalTo(500000));
		assertThat("Fractional seconds", m.getFractionalSeconds(), equalTo(new BigDecimal("1.500000")));
		assertThat("Data filter", m.getDataFilter(), equalTo(0xFF00000000000000L));
	}

}
