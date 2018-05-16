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
import org.apache.commons.codec.DecoderException;
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

	@Test
	public void constructFromHex() throws DecoderException {
		Packet p = new Packet("0205010001010DAC03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void constructFromHexWithWhitespace() throws DecoderException {
		Packet p = new Packet("02 05 01 00 01 01 0D AC 03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void forComponents() throws DecoderException {
		Packet p = Packet.forCommand(1, 1, 1, (String) null);
		assertThat("Packet valid", p.isValid(), equalTo(true));
		assertThat("Address", p.getHeader().getAddress(), equalTo((short) 1));
		assertThat("Command", p.getHeader().getCommand(), equalTo((byte) 1));
		assertThat("Sub-command", p.getHeader().getSubCommand(), equalTo((byte) 1));
		assertThat("CRC", p.getCrc(), equalTo(0xAC0D));
	}

	@Test
	public void forDataNoBody() {
		Packet p = Packet.forData(new byte[] { 0x02, 0x05, 0x01, 0x00 }, 0,
				new byte[] { 0x01, 0x01, (byte) 0x0D, (byte) 0xAC, 0x03 }, 0);
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void forDataNoBodyWithinOffsets() {
		byte[] bytes = new byte[] { (byte) 0xFF, 0x02, 0x05, 0x01, 0x00, 0x01, 0x01, (byte) 0x0D,
				(byte) 0xAC, 0x03, (byte) 0xFF };
		Packet p = Packet.forData(bytes, 1, bytes, 5);
		assertThat("Packet valid", p.isValid(), equalTo(true));
		assertThat("Address", p.getHeader().getAddress(), equalTo((short) 1));
		assertThat("Command", p.getHeader().getCommand(), equalTo((byte) 1));
		assertThat("Sub-command", p.getHeader().getSubCommand(), equalTo((byte) 1));
		assertThat("CRC", p.getCrc(), equalTo(0xAC0D));
	}

	@Test
	public void forDataWithBody() {
		byte[] bytes = new byte[] { 0x02, 0x05, 0x01, 0x02, 0x01, 0x01, (byte) 0xFF, (byte) 0xFE,
				(byte) 0xBD, (byte) 0x5D, 0x03 };
		Packet p = Packet.forData(bytes, 0, bytes, 4);
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

}
