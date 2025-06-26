/* ==================================================================
 * PacketHeaderTests.java - 14/05/2018 10:15:23 AM
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

package net.solarnetwork.node.hw.yaskawa.ecb.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketEnvelope;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketHeader;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketType;

/**
 * Test cases for the {@link PacketHeader} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PacketHeaderTests {

	@Test(expected = IllegalArgumentException.class)
	public void constructNoData() {
		new PacketHeader(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructNoDataAndOffset() {
		new PacketHeader(null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructEmptyData() {
		new PacketHeader(new byte[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructNotEnoughData() {
		new PacketHeader(new byte[3]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructNotEnoughDataWithOffset() {
		new PacketHeader(new byte[10], 7);
	}

	@Test
	public void constructValid() {
		PacketHeader header = new PacketHeader(new byte[] { 0x02, 0x05, 0x01, 0x00, 0x03, 0x04 });
		assertThat("Valid", header.isValid(), equalTo(true));
		assertThat("Envelope", header.getEnvelope(), equalTo(PacketEnvelope.Start));
		assertThat("Type", header.getType(), equalTo(PacketType.MasterLinkRequest));
		assertThat("Address", header.getAddress(), equalTo((short) 1));
		assertThat("Data length", header.getDataLength(), equalTo(0));
	}

	@Test
	public void debugString() {
		PacketHeader header = new PacketHeader(new byte[] { 0x02, 0x05, 0x01, 0x00, 0x03, 0x04 });
		assertThat("Debug string", header.toDebugString(), equalTo("02 05 01 00"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructInvalidUnknownEnvelope() {
		PacketHeader header = new PacketHeader(new byte[] { 0x01, 0x05, 0x01, 0x00, 0x01, 0x01 });
		header.getEnvelope();
	}

	@Test
	public void constructInvalidWrongEnvelope() {
		PacketHeader header = new PacketHeader(new byte[] { 0x03, 0x05, 0x01, 0x00, 0x01, 0x01 });
		assertThat("Envelope", header.getEnvelope(), equalTo(PacketEnvelope.End));
		assertThat("Valid", header.isValid(), equalTo(false));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructInvalidUnknownType() {
		PacketHeader header = new PacketHeader(new byte[] { 0x02, 0x07, 0x01, 0x00, 0x01, 0x01 });
		header.getType();
	}
}
