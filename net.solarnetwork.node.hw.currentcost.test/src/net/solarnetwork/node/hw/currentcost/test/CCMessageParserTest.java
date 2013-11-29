/* ==================================================================
 * CCMessageParserTest.java - Apr 26, 2013 10:05:40 AM
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

package net.solarnetwork.node.hw.currentcost.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import net.solarnetwork.node.hw.currentcost.CCDatum;
import net.solarnetwork.node.hw.currentcost.CCMessageParser;
import org.joda.time.LocalTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

/**
 * Unit test for the {@link CCMessageParser} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CCMessageParserTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void praseClassicMessageWithHistory() throws IOException {
		byte[] xml = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("message-1.xml"));
		CCMessageParser parser = new CCMessageParser();
		CCDatum datum = parser.parseMessage(xml);
		assertNotNull(datum);
		log.debug("Got CCDatum: " + datum.getStatusMessage());
		assertEquals("channel1Watts value", Integer.valueOf(114), datum.getChannel1Watts());
		assertEquals("channel2Watts value", Integer.valueOf(2), datum.getChannel2Watts());
		assertEquals("channel3Watts value", Integer.valueOf(103), datum.getChannel3Watts());
		assertEquals("daysSinceBegin value", Integer.valueOf(6), datum.getDaysSinceBegin());
		assertEquals("02198", datum.getDeviceAddress());
		assertEquals("CC02", datum.getDeviceName());
		assertEquals("0.07", datum.getDeviceSoftwareVersion());
		assertEquals("1", datum.getDeviceType());
		assertNotNull("temperature", datum.getTemperature());
		assertEquals("temperature value", 22.6, datum.getTemperature().doubleValue(), 0.01);
		assertEquals(new LocalTime(10, 43, 49), datum.getTime());
	}

	@Test
	public void praseCC128Message() throws IOException {
		byte[] xml = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("message-2.xml"));
		CCMessageParser parser = new CCMessageParser();
		CCDatum datum = parser.parseMessage(xml);
		assertNotNull(datum);
		log.debug("Got CCDatum: " + datum.getStatusMessage());
		assertEquals("channel1Watts value", Integer.valueOf(16), datum.getChannel1Watts());
		assertNull("channel2Watts value", datum.getChannel2Watts());
		assertNull("channel3Watts value", datum.getChannel3Watts());
		assertEquals("daysSinceBegin value", Integer.valueOf(4), datum.getDaysSinceBegin());
		assertEquals("00077", datum.getDeviceAddress());
		assertEquals("CC128-v1.51", datum.getDeviceName());
		assertNull("deviceSoftwareVersion", datum.getDeviceSoftwareVersion());
		assertEquals("1", datum.getDeviceType());
		assertNotNull("temperature", datum.getTemperature());
		assertEquals("temperature value", 24.6, datum.getTemperature().doubleValue(), 0.01);
		assertEquals(new LocalTime(14, 14, 27), datum.getTime());
	}

}
