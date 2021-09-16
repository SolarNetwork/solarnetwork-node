/* ==================================================================
 * CommandMessageTest.java - Jul 7, 2012 8:09:47 PM
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
 */

package net.solarnetwork.node.rfxcom.test;

import static org.junit.Assert.*;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.node.rfxcom.Command;
import net.solarnetwork.node.rfxcom.CommandMessage;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageFactory;
import net.solarnetwork.node.rfxcom.MessageType;
import net.solarnetwork.node.rfxcom.StatusMessage;
import net.solarnetwork.node.rfxcom.TransceiverType;

/**
 * Unit tests for the {@link CommandMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CommandMessageTest {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void encodeResetCommand() {
		CommandMessage msg = new CommandMessage(Command.Reset);
		assertNull("Data", msg.getData());
		final byte[] packet = msg.getMessagePacket();
		log.debug("Got packet: " +Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("0D 00 00 00 00 00 00 00 00 00 00 00 00 00"), packet);
	}
	
	@Test
	public void encodeStatusCommand() {
		CommandMessage msg = new CommandMessage(Command.Status, (short)1);
		assertNull("Data", msg.getData());
		final byte[] packet = msg.getMessagePacket();
		log.debug("Got packet: " +Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("0D 00 00 01 02 00 00 00 00 00 00 00 00 00"), packet);
	}
	
	private void verifyStatusCommandResponse(Message m) {
		assertTrue("Instance of StatusMessage", m instanceof StatusMessage);
		StatusMessage msg = (StatusMessage)m;
		assertEquals(1, msg.getSequenceNumber());
		assertEquals(MessageType.CommandResponse, msg.getType());
		assertEquals((byte)0, msg.getSubType());
		assertEquals(Command.Status, msg.getCommand());
		assertEquals(TransceiverType.Type43392a, msg.getTransceiverType());
		assertEquals(42, msg.getFirmwareVersion());
		assertEquals(1, msg.getHardwareVersion());
		assertFalse("Undecoded mode", msg.isUndecodedMode());
		assertTrue("X10 enabled", msg.isX10Enabled());
		assertTrue("ARC enabled", msg.isARCEnabled());
		assertTrue("AC enabled", msg.isACEnabled());
		assertTrue("HomeEasyEU enabled", msg.isHomeEasyEUEnabled());
		assertFalse("Ikea Koppla enabled", msg.isIkeaKopplaEnabled());
		assertTrue("Oregon Scientific enabled", msg.isOregonEnabled());
		assertTrue("ATI enabled", msg.isATIEnabled());
		assertTrue("Visonic enabled", msg.isVisonicEnabled());
		assertFalse("Mertik enabled", msg.isMertikEnabled());
		assertTrue("AD enabled", msg.isADEnabled());
		assertTrue("Hideki enabled", msg.isHidekiEnabled());
		assertTrue("LaCrosse enabled", msg.isLaCrosseEnabled());
		assertFalse("FS20 enabled", msg.isFS20Enabled());
		assertFalse("ProGuard enabled", msg.isProGuardEnabled());
		//assertTrue("RollerTrol enabled", msg.is)
	}
	
	@Test
	public void decodeStatusCommandResponse() {
		final byte[] packet = TestUtils.bytesFromHexString("0D01000102532A000EEF01000000");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		verifyStatusCommandResponse(new StatusMessage((short)1, data));
		
		// now via MessageFactory
		verifyStatusCommandResponse(new MessageFactory().parseMessage(packet, 0));
	}
	
	
	private void verifyStatusCommandResponseOnlyOregonEnabled(Message m) {
		assertTrue("Instance of StatusMessage", m instanceof StatusMessage);
		StatusMessage msg = (StatusMessage)m;
		assertEquals(24, msg.getSequenceNumber());
		assertEquals(MessageType.CommandResponse, msg.getType());
		assertEquals((byte)0, msg.getSubType());
		assertEquals(Command.Status, msg.getCommand());
		assertEquals(TransceiverType.Type43392a, msg.getTransceiverType());
		assertEquals(42, msg.getFirmwareVersion());
		assertEquals(1, msg.getHardwareVersion());
		assertFalse("Undecoded mode", msg.isUndecodedMode());
		assertFalse("X10 enabled", msg.isX10Enabled());
		assertFalse("ARC enabled", msg.isARCEnabled());
		assertFalse("AC enabled", msg.isACEnabled());
		assertFalse("HomeEasyEU enabled", msg.isHomeEasyEUEnabled());
		assertFalse("Ikea Koppla enabled", msg.isIkeaKopplaEnabled());
		assertTrue("Oregon Scientific enabled", msg.isOregonEnabled());
		assertFalse("ATI enabled", msg.isATIEnabled());
		assertFalse("Visonic enabled", msg.isVisonicEnabled());
		assertFalse("Mertik enabled", msg.isMertikEnabled());
		assertFalse("AD enabled", msg.isADEnabled());
		assertFalse("Hideki enabled", msg.isHidekiEnabled());
		assertFalse("LaCrosse enabled", msg.isLaCrosseEnabled());
		assertFalse("FS20 enabled", msg.isFS20Enabled());
		assertFalse("ProGuard enabled", msg.isProGuardEnabled());
	}
	
	@Test
	public void decodeStatusCommandResponseOnlyOregonEnabled() {
		final byte[] packet = TestUtils.bytesFromHexString("0D01001802532A00002001000000");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		verifyStatusCommandResponseOnlyOregonEnabled(new StatusMessage(packet[3], data));
		
		// now via MessageFactory
		verifyStatusCommandResponseOnlyOregonEnabled(new MessageFactory().parseMessage(packet, 0));
	}
	
	@Test
	public void encodeSaveSettingsCommand() {
		CommandMessage msg = new CommandMessage(Command.SaveSettings, (short)31);
		assertNull("Data", msg.getData());
		final byte[] packet = msg.getMessagePacket();
		log.debug("Got packet: " +Hex.encodeHexString(packet));
		assertNotNull("Packet", packet);
		assertArrayEquals(TestUtils.bytesFromHexString("0D 00 00 1F 06 00 00 00 00 00 00 00 00 00"), packet);
	}
	
	private void verifySaveSettingsResponse(Message m) {
		assertTrue("CommandMessage instance", m instanceof CommandMessage);
		CommandMessage msg = (CommandMessage)m;
		assertEquals(49, msg.getSequenceNumber());
		assertEquals(MessageType.CommandResponse, msg.getType());
		assertEquals((byte)0, msg.getSubType());
		assertEquals(Command.SaveSettings, msg.getCommand());
	}
	
	@Test
	public void decodeSaveSettingsResponse() {
		final byte[] packet = TestUtils.bytesFromHexString("0D01003106532A00002001000000");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		verifySaveSettingsResponse(new CommandMessage(packet[3], data));
		verifySaveSettingsResponse(new MessageFactory().parseMessage(packet, 0));
	}
	
}
