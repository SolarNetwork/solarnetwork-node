/* ==================================================================
 * PacketTests.java - 15/05/2018 5:20:33 PM
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

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.hw.yaskawa.ecb.Packet;

/**
 * Test cases for the {@link Packet} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PacketTests {

	private static Byte[] byteArray(byte[] array) {
		if ( array == null ) {
			return null;
		}
		Byte[] result = new Byte[array.length];
		for ( int i = 0, len = array.length; i < len; i++ ) {
			result[i] = array[i];
		}
		return result;
	}

	@Test
	public void crc() {
		Packet p = new Packet(
				new byte[] { 0x02, 0x05, 0x01, 0x00, 0x01, 0x01, (byte) 0x0D, (byte) 0xAC, 0x03 });
		assertThat("Calcualted CRC", p.getCalculatedCrc(), equalTo(0xAC0D));
		assertThat("Encoded CRC", p.getCrc(), equalTo(0xAC0D));
		assertThat("Packet appears valid", p.isValid(), equalTo(true));
	}

	@Test
	public void bodyEmpty() {
		Packet p = new Packet(
				new byte[] { 0x02, 0x05, 0x01, 0x00, 0x01, 0x01, (byte) 0x0D, (byte) 0xAC, 0x03 });
		assertThat("Body", byteArray(p.getBody()), arrayWithSize(0));
	}

	@Test
	public void body() {
		Packet p = new Packet(
				new byte[] { 0x02, 0x05, 0x01, 0x02, 0x01, 0x01, (byte) 0x10, (byte) 0x11 });
		assertThat("Body", byteArray(p.getBody()), arrayContaining((byte) 0x10, (byte) 0x11));
	}

	@Test(expected = IllegalArgumentException.class)
	public void bodyMissingData() {
		Packet p = new Packet(new byte[] { 0x02, 0x05, 0x01, 0x02, 0x01, 0x01, (byte) 0x10 });
		p.getBody();
	}

}
