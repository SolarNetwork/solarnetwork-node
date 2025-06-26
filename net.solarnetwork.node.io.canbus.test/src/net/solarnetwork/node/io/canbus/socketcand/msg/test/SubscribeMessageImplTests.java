/* ==================================================================
 * FrameMessageImplTests.java - 20/09/2019 2:24:15 pm
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
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;

/**
 * Test cases for the {@link SubscribeMessageImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SubscribeMessageImplTests {

	@Test
	public void construct_noTime() {
		// GIVEN
		SubscribeMessageImpl m = new SubscribeMessageImpl(asList("0", "0", "123"));

		// THEN
		assertThat("Message time", m.getFractionalSeconds(), equalTo(new BigDecimal("0.000000")));
		assertThat("Message address", m.getAddress(), equalTo(0x123));
	}

	@Test
	public void construct_withTime() {
		// GIVEN
		SubscribeMessageImpl m = new SubscribeMessageImpl(asList("1", "500000", "123"));

		// THEN
		assertThat("Message time", m.getFractionalSeconds(), equalTo(new BigDecimal("1.500000")));
		assertThat("Message address", m.getAddress(), equalTo(0x123));
	}

	@Test(expected = IllegalArgumentException.class)
	public void construct_emptyArgs() {
		// GIVEN
		new SubscribeMessageImpl(emptyList());
	}

	@Test(expected = IllegalArgumentException.class)
	public void construct_tooFewArgs() {
		// GIVEN
		new SubscribeMessageImpl(asList("0", "0"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void construct_secondsNaN() {
		// GIVEN
		new SubscribeMessageImpl(asList("A", "0", "123"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void construct_microsecondsNaN() {
		// GIVEN
		new SubscribeMessageImpl(asList("0", "A", "123"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void construct_addressNaN() {
		// GIVEN
		new SubscribeMessageImpl(asList("0", "1", "Z"));
	}

}
