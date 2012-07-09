/* ==================================================================
 * SetModeMessageTest.java - Jul 8, 2012 1:32:26 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.rfxcom.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import net.solarnetwork.node.rfxcom.Command;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageFactory;
import net.solarnetwork.node.rfxcom.MessageType;
import net.solarnetwork.node.rfxcom.SetModeMessage;
import net.solarnetwork.node.rfxcom.StatusMessage;
import net.solarnetwork.node.rfxcom.TransceiverType;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for the {@link SetModeMessage} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class SetModeMessageTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
		
	@Test
	public void testEncodeSetModeCommand1() {
		SetModeMessage msg = new SetModeMessage((short)27, TransceiverType.Type43392a);
		msg.setOregonEnabled(true);
		final byte[] packet = msg.getMessagePacket();
		log.debug("Got packet: " +Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("0D 00 00 1B 03 53 00 00 00 20 00 00 00 00"), packet);
	}
	
	@Test
	public void testEncodeSetModeCommand2() {
		SetModeMessage msg = new SetModeMessage((short)34, TransceiverType.Type43392a);
		msg.setOregonEnabled(true);
		msg.setUndecodedMode(true);
		final byte[] packet = msg.getMessagePacket();
		log.debug("Got packet: " +Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("0D 00 00 22 03 53 00 80 00 20 00 00 00 00"), packet);	
	}
	
	private void verifySaveSettingsResponse(Message m) {
		assertTrue("StatusMessage instance", m instanceof StatusMessage);
		StatusMessage msg = (StatusMessage)m;
		assertEquals(4, msg.getSequenceNumber());
		assertEquals(MessageType.CommandResponse, msg.getType());
		assertEquals((byte)0, msg.getSubType());
		assertEquals(Command.SetMode, msg.getCommand());
	}
	
	@Test
	public void decodeSetModeResponse() {
		final byte[] packet = TestUtils.bytesFromHexString("0D01000403532A00002001000000");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		verifySaveSettingsResponse(new StatusMessage(packet[3], data));
		verifySaveSettingsResponse(new MessageFactory().parseMessage(packet, 0));
	}

}
