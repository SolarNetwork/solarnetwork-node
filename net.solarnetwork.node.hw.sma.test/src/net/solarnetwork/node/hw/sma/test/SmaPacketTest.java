/* ==================================================================
 * SmaPacketTest.java - Nov 1, 2013 6:28:02 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sma.protocol.SmaChannel;
import net.solarnetwork.node.hw.sma.protocol.SmaChannelParam;
import net.solarnetwork.node.hw.sma.protocol.SmaChannelType;
import net.solarnetwork.node.hw.sma.protocol.SmaChannelTypeGroup;
import net.solarnetwork.node.hw.sma.protocol.SmaCommand;
import net.solarnetwork.node.hw.sma.protocol.SmaControl;
import net.solarnetwork.node.hw.sma.protocol.SmaPacket;
import net.solarnetwork.node.hw.sma.protocol.SmaUserDataField;

/**
 * Test cases for {@link SmaPacket}.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaPacketTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void encodeNetStart() {
		SmaPacket p = SmaPacket.netStartPacket(0);
		byte[] packet = p.getPacket();
		log.debug("Got packet: " + Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("00 00 00 00 80 00 06"), packet);
	}

	@Test
	public void encodeNetStartNonZeroSourceAddress() {
		SmaPacket p = SmaPacket.netStartPacket(8888);
		byte[] packet = p.getPacket();
		log.debug("Got packet: " + Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("B8 22 00 00 80 00 06"), packet);
	}

	@Test
	public void encodeNetGet() {
		SmaPacket p = SmaPacket.netGetPacket(1);
		byte[] packet = p.getPacket();
		log.debug("Got packet: " + Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("01 00 00 00 80 00 01"), packet);
	}

	@Test
	public void decodeNetStartResponse() {
		SmaPacket p = new SmaPacket(
				TestUtils.bytesFromHexString("02 00 01 00 40 00 06 45 24 8F 00 57 52 37 30 30 2D 30 37"));
		p.decodeUserDataFields();
		assertEquals(2, p.getSrcAddress());
		assertEquals(1, p.getDestAddress());
		assertEquals(SmaControl.Response, p.getControl());
		assertEquals(0, p.getPacketCounter());
		assertEquals(SmaCommand.NetStart, p.getCommand());
		assertEquals("Serial number", 9380933L, p.getUserDataField(SmaUserDataField.DeviceSerialNumber));
		assertEquals("Device type", "WR700-07", p.getUserDataField(SmaUserDataField.DeviceType));
	}

	@Test
	public void encodeGetChannelInfo() {
		SmaPacket p = SmaPacket.getChannelInfoPacket(8888, 2);
		byte[] packet = p.getPacket();
		log.debug("Got packet: " + Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("B8 22 02 00 00 00 09"), packet);
	}

	@Test
	public void decodeGetChannelInfoResponse() {
		SmaPacket p = new SmaPacket(
				TestUtils
						.bytesFromHexString("02 00 B8 22 40 3B 09 01 01 04 02 01 02 00 53 4D 41 2D 53 4E 20 20 20 20 20 20 20 20 20 00 20 20 20 20 20 20 20 00 00 00 00 00 00 24 74 49"));
		p.decodeUserDataFields();
		assertEquals(2, p.getSrcAddress());
		assertEquals(8888, p.getDestAddress());
		assertEquals(SmaControl.Response, p.getControl());
		assertEquals(59, p.getPacketCounter());
		assertEquals(SmaCommand.GetChannelInfo, p.getCommand());
		@SuppressWarnings("unchecked")
		List<SmaChannel> channels = (List<SmaChannel>) p.getUserDataField(SmaUserDataField.Channels);
		assertNotNull("SmaChannel list", channels);
		assertEquals("channel count", 1, channels.size());
		SmaChannel c = channels.get(0);
		assertEquals("SMA-SN", c.getName());
		assertEquals(SmaChannelType.Analog, c.getType());
		assertEquals(SmaChannelTypeGroup.Parameter, c.getTypeGroup());
		assertEquals(0.0, ((Float) c.getParameterValue(SmaChannelParam.Gain)).doubleValue(), 1e-4);
		assertEquals(1e6, ((Float) c.getParameterValue(SmaChannelParam.Offset)).doubleValue(), 1e-4);
	}

}
