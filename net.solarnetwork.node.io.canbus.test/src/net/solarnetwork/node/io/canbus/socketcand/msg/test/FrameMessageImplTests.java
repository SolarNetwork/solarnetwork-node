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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.util.Arrays;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;

/**
 * Test cases for the {@link FrameMessageImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FrameMessageImplTests {

	@Test
	public void getData_basic() {
		// GIVEN
		FrameMessageImpl m = new FrameMessageImpl(asList("123", "23.424242", "11", "22", "33", "44"));

		// WHEN
		byte[] data = m.getData();

		// THEN
		assertThat("Decoded message data", Arrays.equals(data, new byte[] { 0x11, 0x22, 0x33, 0x44 }),
				equalTo(true));
	}

	@Test
	public void getData_empty() {
		// GIVEN
		FrameMessageImpl m = new FrameMessageImpl(asList("123", "23.424242"));

		// WHEN
		byte[] data = m.getData();

		// THEN
		assertThat("Decoded message data", Arrays.equals(data, new byte[0]), equalTo(true));
	}

	@Test
	public void getData_paddedHex() {
		// GIVEN
		FrameMessageImpl m = new FrameMessageImpl(asList("123", "23.424242", "1", "2", "3"));

		// WHEN
		byte[] data = m.getData();

		// THEN
		assertThat("Decoded message data", Arrays.equals(data, new byte[] { 0x1, 0x2, 0x3 }),
				equalTo(true));
	}

	@Test
	public void constructFromValues() {
		// GIVEN
		FrameMessageImpl m = new FrameMessageImpl(123, false, 23, 424242,
				new byte[] { (byte) 0x1, (byte) 0x2, (byte) 0x3 });

		// WHEN
		byte[] data = m.getData();

		// THEN
		assertThat("Constructed address", m.getAddress(), equalTo(123));
		assertThat("Constructed seconds", m.getSeconds(), equalTo(23));
		assertThat("Constructed microseconds", m.getMicroseconds(), equalTo(424242));
		assertThat("Decoded message data", Arrays.equals(data, new byte[] { 0x1, 0x2, 0x3 }),
				equalTo(true));

	}
}
