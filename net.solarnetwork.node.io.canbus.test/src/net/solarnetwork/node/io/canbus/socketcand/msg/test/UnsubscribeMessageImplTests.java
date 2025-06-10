/* ==================================================================
 * UnsubscribeMessageImplTests.java - 23/09/2019 5:03:16 pm
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
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.msg.UnsubscribeMessageImpl;

/**
 * Test cases for the {@link UnsubscribeMessageImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UnsubscribeMessageImplTests {

	@Test
	public void construct_list() {
		// GIVEN
		List<String> arguments = asList("123");

		// WHEN
		UnsubscribeMessageImpl m = new UnsubscribeMessageImpl(arguments);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
	}

	@Test
	public void write() throws IOException {
		// GIVEN
		UnsubscribeMessageImpl m = new UnsubscribeMessageImpl(0x123, false);

		// WHEN
		StringWriter out = new StringWriter();
		m.write(out);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Written message", out.toString(), equalTo("< unsubscribe 123 >"));
	}

	@Test
	public void write_extended() throws IOException {
		// GIVEN
		UnsubscribeMessageImpl m = new UnsubscribeMessageImpl(0x123, true);

		// WHEN
		StringWriter out = new StringWriter();
		m.write(out);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Written message in extended mode", out.toString(),
				equalTo("< unsubscribe 00000123 >"));
	}

}
