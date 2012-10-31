/* ==================================================================
 * CurrentMessageTest.java - Jul 7, 2012 6:43:48 PM
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

import net.solarnetwork.node.rfxcom.CurrentMessage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit test for the {@link CurrentMessage} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class CurrentMessageTest {
		
	@Test
	public void parseMessage1() {
		final byte[] packet = TestUtils.bytesFromHexString("0D5901001A002A001E0000000059");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		CurrentMessage msg = new CurrentMessage(packet[2], packet[3], data);
		assertFalse(msg.isBatteryLow());
		assertEquals("Sequence", 0, msg.getSequenceNumber());
		assertEquals("Signal level", 5, msg.getSignalLevel());
		assertEquals("ID", "1A00", msg.getAddress());
		assertEquals("Count", 42, msg.getCount());
		assertEquals("Amp 1", 3.0, msg.getAmpReading1(), 0.0001);
		assertEquals("Amp 2", 0.0, msg.getAmpReading2(), 0.0001);
		assertEquals("Amp 3", 0.0, msg.getAmpReading3(), 0.0001);
	}
	
	@Test
	public void parseMessage2() {	
		final byte[] packet = TestUtils.bytesFromHexString("0D5901011A0000001C0000000059");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		CurrentMessage msg = new CurrentMessage(packet[2], packet[3], data);
		assertFalse(msg.isBatteryLow());
		assertEquals("Sequence", 1, msg.getSequenceNumber());
		assertEquals("Signal level", 5, msg.getSignalLevel());
		assertEquals("ID", "1A00", msg.getAddress());
		assertEquals("Count", 0, msg.getCount());
		assertEquals("Amp 1", 2.8, msg.getAmpReading1(), 0.0001);
		assertEquals("Amp 2", 0.0, msg.getAmpReading2(), 0.0001);
		assertEquals("Amp 3", 0.0, msg.getAmpReading3(), 0.0001);
	}
	
	@Test
	public void parseMessage3() {	
		final byte[] packet = TestUtils.bytesFromHexString("0D5901361A000400000000000069");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		CurrentMessage msg = new CurrentMessage(packet[2], packet[3], data);
		assertFalse(msg.isBatteryLow());
		assertEquals("Sequence", 54, msg.getSequenceNumber());
		assertEquals("Signal level", 6, msg.getSignalLevel());
		assertEquals("ID", "1A00", msg.getAddress());
		assertEquals("Count", 4, msg.getCount());
		assertEquals("Amp 1", 0.0, msg.getAmpReading1(), 0.0001);
		assertEquals("Amp 2", 0.0, msg.getAmpReading2(), 0.0001);
		assertEquals("Amp 3", 0.0, msg.getAmpReading3(), 0.0001);
	}	

	@Test
	public void parseMessage4() {	
		final byte[] packet = TestUtils.bytesFromHexString("0D5901011A002A0000001D000059");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		CurrentMessage msg = new CurrentMessage(packet[2], packet[3], data);
		assertFalse(msg.isBatteryLow());
		assertEquals("Sequence", 1, msg.getSequenceNumber());
		assertEquals("Signal level", 5, msg.getSignalLevel());
		assertEquals("ID", "1A00", msg.getAddress());
		assertEquals("Count", 42, msg.getCount());
		assertEquals("Amp 1", 0.0, msg.getAmpReading1(), 0.0001);
		assertEquals("Amp 2", 2.9, msg.getAmpReading2(), 0.0001);
		assertEquals("Amp 3", 0.0, msg.getAmpReading3(), 0.0001);
	}

	@Test
	public void parseMessage5() {	
		final byte[] packet = TestUtils.bytesFromHexString("0D5901041A000000000000001C59");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		CurrentMessage msg = new CurrentMessage(packet[2], packet[3], data);
		assertFalse(msg.isBatteryLow());
		assertEquals("Sequence", 4, msg.getSequenceNumber());
		assertEquals("Signal level", 5, msg.getSignalLevel());
		assertEquals("ID", "1A00", msg.getAddress());
		assertEquals("Count", 0, msg.getCount());
		assertEquals("Amp 1", 0.0, msg.getAmpReading1(), 0.0001);
		assertEquals("Amp 2", 0.0, msg.getAmpReading2(), 0.0001);
		assertEquals("Amp 3", 2.8, msg.getAmpReading3(), 0.0001);
	}
}
