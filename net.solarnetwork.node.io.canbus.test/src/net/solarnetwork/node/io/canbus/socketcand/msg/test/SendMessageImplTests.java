/* ==================================================================
 * SendMessageImplTests.java - 21/09/2019 8:46:21 am
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.msg.SendMessageImpl;

/**
 * Test cases for the {@link SendMessageImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SendMessageImplTests {

	@Test
	public void construct() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x11, 0x22, 0x33, (byte) 0xFF };

		// WHEN
		SendMessageImpl m = new SendMessageImpl(1, data);

		// THEN
		assertThat("Arguments", m.getArguments(), contains("1", "4", "11", "22", "33", "FF"));
		assertThat("Data length", m.getDataLength(), equalTo(data.length));
		assertThat("Data", Arrays.equals(m.getData(), data), equalTo(true));
		assertThat("Address", m.getAddress(), equalTo(1));
		assertThat("Extended address", m.isExtendedAddress(), equalTo(false));
	}

	@Test
	public void construct_extended() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x11, 0x22, 0x33, (byte) 0xFF };

		// WHEN
		SendMessageImpl m = new SendMessageImpl(378489, data);

		// THEN
		assertThat("Arguments", m.getArguments(), contains("0005C679", "4", "11", "22", "33", "FF"));
		assertThat("Data length", m.getDataLength(), equalTo(data.length));
		assertThat("Data", Arrays.equals(m.getData(), data), equalTo(true));
		assertThat("Address", m.getAddress(), equalTo(378489));
		assertThat("Extended address", m.isExtendedAddress(), equalTo(true));
	}

	@Test
	public void construct_forced_extended() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x11, 0x22, 0x33, (byte) 0xFF };

		// WHEN
		SendMessageImpl m = new SendMessageImpl(1, true, data);

		// THEN
		assertThat("Arguments", m.getArguments(), contains("00000001", "4", "11", "22", "33", "FF"));
		assertThat("Data length", m.getDataLength(), equalTo(data.length));
		assertThat("Data", Arrays.equals(m.getData(), data), equalTo(true));
		assertThat("Address", m.getAddress(), equalTo(1));
		assertThat("Extended address", m.isExtendedAddress(), equalTo(true));
	}

	@Test
	public void write() throws IOException {
		// GIVEN
		SendMessageImpl m = new SendMessageImpl(234,
				new byte[] { (byte) 0x11, 0x22, 0x33, (byte) 0xFF });

		// WHEN
		StringWriter out = new StringWriter(16);
		m.write(out);

		// THEN
		assertThat("Message value", out.toString(), equalTo("< send EA 4 11 22 33 FF >"));
	}

	@Test
	public void write_extended() throws IOException {
		// GIVEN
		SendMessageImpl m = new SendMessageImpl(0xA12FF4C,
				new byte[] { (byte) 0x11, 0x22, 0x33, (byte) 0xFF });

		// WHEN
		StringWriter out = new StringWriter(16);
		m.write(out);

		// THEN
		assertThat("Message value", out.toString(), equalTo("< send 0A12FF4C 4 11 22 33 FF >"));
	}

}
