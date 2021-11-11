/* ==================================================================
 * EnergyMessageTest.java - Jul 8, 2012 3:25:59 PM
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.solarnetwork.node.rfxcom.EnergyMessage;
import org.junit.Test;

/**
 * Test cases for the {@link EnergyMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class EnergyMessageTest {

	@Test
	public void parseMessage1() {
		final byte[] packet = TestUtils.bytesFromHexString("115A010188F200000000000000000036B079");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		EnergyMessage msg = new EnergyMessage(packet[2], packet[3], data);
		assertEquals(1, msg.getSubType());
		assertFalse("Battery low", msg.isBatteryLow());
		assertEquals("Signal level", 7, msg.getSignalLevel());
		assertEquals("ID", "88F2", msg.getAddress());
		assertEquals("Count", 0, msg.getCount());
		assertEquals("Instant", 0, msg.getInstantWatts(), 0.0001);
		assertEquals("Use", 62.5933311276636, msg.getUsageWattHours(), 0.00000001);
	}

	@Test
	public void parseMessage2() {
		final byte[] packet = TestUtils.bytesFromHexString("115A013588F204000002C4000000003B4050");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		EnergyMessage msg = new EnergyMessage(packet[2], packet[3], data);
		assertEquals(1, msg.getSubType());
		assertTrue("Battery low", msg.isBatteryLow());
		assertEquals("Signal level", 5, msg.getSignalLevel());
		assertEquals("ID", "88F2", msg.getAddress());
		assertEquals("Count", 4, msg.getCount());
		assertEquals("Instant", 708.0, msg.getInstantWatts(), 0.0001);
		assertEquals("Use", 67.8154033246001, msg.getUsageWattHours(), 0.00000001);
	}

	@Test
	public void parseMessage3() {
		final byte[] packet = TestUtils.bytesFromHexString("115A013588F204000002C4000000003B4050");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		EnergyMessage msg = new EnergyMessage(packet[2], packet[3], data);
		assertEquals(1, msg.getSubType());
		assertTrue("Battery low", msg.isBatteryLow());
		assertEquals("Signal level", 5, msg.getSignalLevel());
		assertEquals("ID", "88F2", msg.getAddress());
		assertEquals("Count", 4, msg.getCount());
		assertEquals("Instant", 708.0, msg.getInstantWatts(), 0.0001);
		assertEquals("Use", 67.8154033246001, msg.getUsageWattHours(), 0.00000001);
	}

	@Test
	public void parseMessage4() {
		final byte[] packet = TestUtils.bytesFromHexString("115A010288F200000002A400000000617F49");
		final byte[] data = TestUtils.extractMessageBytes(packet);
		EnergyMessage msg = new EnergyMessage(packet[2], packet[3], data);
		assertEquals(1, msg.getSubType());
		assertEquals("Sequence", 2, msg.getSequenceNumber());
		assertFalse("Battery low", msg.isBatteryLow());
		assertEquals("Signal level", 4, msg.getSignalLevel());
		assertEquals("ID", "88F2", msg.getAddress());
		assertEquals("Count", 0, msg.getCount());
		assertEquals("Instant", 676.0, msg.getInstantWatts(), 0.0001);
		assertEquals("Use", 111.590496543954, msg.getUsageWattHours(), 0.00000001);
	}
}
