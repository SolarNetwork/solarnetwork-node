/* ==================================================================
 * BatteryDataDeserializerTests.java - 16/02/2016 3:36:30 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.panasonic.battery.test;

import java.io.InputStream;
import java.util.Arrays;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.hw.panasonic.battery.BatteryData;
import net.solarnetwork.node.hw.panasonic.battery.BatteryDataDeserializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * Test cases for the {@link BatteryDataDeserializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BatteryDataDeserializerTests {

	@Test
	public void parseBatteryDataJSON() throws Exception {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setDeserializers(
				Arrays.asList(new JsonDeserializer<?>[] { new BatteryDataDeserializer() }));
		ObjectMapper om = factory.getObject();
		InputStream in = getClass().getResourceAsStream("battery-data-01.json");
		BatteryData bd = om.readValue(in, BatteryData.class);
		Assert.assertNotNull(bd);
		Assert.assertEquals("1666729", bd.getDeviceID());
		Assert.assertEquals(new DateTime(2016, 2, 9, 14, 16, 41, DateTimeZone.UTC), bd.getDate());
		Assert.assertEquals("A", bd.getStatus());
		Assert.assertEquals(Integer.valueOf(7000), bd.getAvailableCapacity());
		Assert.assertEquals(Integer.valueOf(8400), bd.getTotalCapacity());
	}

	@Test
	public void parseInvalidDate() throws Exception {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setDeserializers(
				Arrays.asList(new JsonDeserializer<?>[] { new BatteryDataDeserializer() }));
		ObjectMapper om = factory.getObject();
		InputStream in = getClass().getResourceAsStream("battery-data-02.json");
		BatteryData bd = om.readValue(in, BatteryData.class);
		Assert.assertNotNull(bd);
		Assert.assertEquals("1666729", bd.getDeviceID());
		Assert.assertNotNull(bd.getDate());
		Assert.assertTrue("Date should be set to now",
				(System.currentTimeMillis() - bd.getDate().getMillis()) < 1000);
		Assert.assertEquals("A", bd.getStatus());
		Assert.assertEquals(Integer.valueOf(7000), bd.getAvailableCapacity());
		Assert.assertEquals(Integer.valueOf(8400), bd.getTotalCapacity());
	}
}
