/* ==================================================================
 * SimpleBatteryAPIClientTests.java - 16/02/2016 3:35:20 pm
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

import java.io.IOException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import net.solarnetwork.node.hw.panasonic.battery.BatteryData;
import net.solarnetwork.node.hw.panasonic.battery.BatteryDataDeserializer;
import net.solarnetwork.node.hw.panasonic.battery.SimpleBatteryAPIClient;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test cases for the {@link SimpleBatteryAPIClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleBatteryAPIClientTests {

	private static final String TEST_BASE_URL = "http://localhost/batteryapi";

	private ObjectMapper getObjectMapper() {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setDeserializers(Arrays
				.asList(new JsonDeserializer<?>[] { new BatteryDataDeserializer() }));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getBatteryDataForEmail() {
		final String email = "test@localhost";
		final String fileURL = getClass().getResource("battery-data-01.json").toString();
		SimpleBatteryAPIClient client = new SimpleBatteryAPIClient() {

			@Override
			protected URLConnection getURLConnection(String url, String httpMethod, String accept)
					throws IOException {
				final String expectedURL = TEST_BASE_URL + "/BatteryByEmail?EmailID="
						+ URLEncoder.encode(email, "UTF-8");
				Assert.assertEquals(expectedURL, url);
				return super.getURLConnection(fileURL, httpMethod, accept);
			}

		};
		client.setBaseURL(TEST_BASE_URL);
		client.setObjectMapper(getObjectMapper());

		BatteryData bd = client.getCurrentBatteryDataForEmail(email);
		Assert.assertNotNull(bd);
		Assert.assertEquals("1666729", bd.getDeviceID());
		Assert.assertEquals(new DateTime(2016, 2, 9, 14, 16, 41, DateTimeZone.UTC), bd.getDate());
		Assert.assertEquals("A", bd.getStatus());
		Assert.assertEquals(Integer.valueOf(7000), bd.getAvailableCapacity());
		Assert.assertEquals(Integer.valueOf(8400), bd.getTotalCapacity());
	}

}
