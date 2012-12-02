/* ==================================================================
 * MessageFactoryTest.java - Dec 3, 2012 9:09:44 AM
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.solarnetwork.node.rfxcom.EnergyMessage;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageFactory;
import org.junit.Test;

/**
 * Test cases for the {@link MessageFactory} class.
 * 
 * <p>
 * These test cases include actual packet data I've captured from logging on a
 * node.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class MessageFactoryTest {

	@Test
	public void createEnergyMessage() {
		final byte[] packet = TestUtils.bytesFromHexString("115A010188F200000000000000000036B079");
		MessageFactory mf = new MessageFactory();
		Message result = mf.parseMessage(packet, 0);
		assertNotNull(result);
		assertEquals(EnergyMessage.class, result.getClass());
		EnergyMessage msg = (EnergyMessage) result;
		assertEquals(1, msg.getSubType());
		assertFalse("Battery low", msg.isBatteryLow());
		assertEquals("Signal level", 7, msg.getSignalLevel());
		assertEquals("ID", "88F2", msg.getAddress());
		assertEquals("Count", 0, msg.getCount());
		assertEquals("Instant", 0, msg.getInstantWatts(), 0.0001);
		assertEquals("Use", 62.5933311276636, msg.getUsageWattHours(), 0.00000001);
	}

	@Test
	public void createBadMessage() {
		final byte[] packet = TestUtils.bytesFromHexString("0c030cceff115a016f6b120000");
		MessageFactory mf = new MessageFactory();
		Message result = mf.parseMessage(packet, 0);
		assertNull(result);
	}

}
