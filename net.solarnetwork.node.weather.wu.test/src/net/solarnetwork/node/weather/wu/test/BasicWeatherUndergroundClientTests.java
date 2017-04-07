/* ==================================================================
 * BasicWeatherUndergroundClientTests.java - 7/04/2017 4:46:32 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.wu.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.weather.wu.BasicWeatherUndergoundClient;
import net.solarnetwork.node.weather.wu.WeatherUndergroundLocation;

/**
 * Test cases for the {@link BasicWeatherUndergoundClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicWeatherUndergroundClientTests extends AbstractHttpClientTests {

	private static final String TEST_API_KEY = "TEST_API_KEY";

	private BasicWeatherUndergoundClient client;

	@Before
	public void setupClient() {
		client = new BasicWeatherUndergoundClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.setBaseAutocompleteUrl(getHttpServerBaseUrl());
		client.setApiKey(TEST_API_KEY);
	}

	@Test
	public void geolookupForIpAddress() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/TEST_API_KEY/geolookup/q/autoip.json",
						request.getPathInfo());
				respondWithJsonResource(response, "geolookup-1.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		Collection<WeatherUndergroundLocation> results = client.findLocationsForIpAddress();
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Results available", results);
		assertEquals("Result count", 1, results.size());

		WeatherUndergroundLocation loc = results.iterator().next();
		assertEquals("Identifier", "/q/zmw:00000.113.93546", loc.getIdentifier());
		assertNull("Name", loc.getName());
		assertEquals("Country", "NZ", loc.getCountry());
		assertEquals("StateOrProvince", "MBH", loc.getStateOrProvince());
		assertEquals("Locality", "Whakatahuri", loc.getLocality());
		assertNull("PostalCode", loc.getPostalCode());
		assertEquals("TimeZoneId", "Pacific/Auckland", loc.getTimeZoneId());
		assertEquals("Latitude", new BigDecimal("-41.000000"), loc.getLatitude());
		assertEquals("Longitude", new BigDecimal("174.000000"), loc.getLongitude());
	}

	@Test
	public void queryForNameAndCountry() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/", request.getPathInfo());
				assertEquals("Request query", "query=test&c=NZ", request.getQueryString());
				respondWithJsonResource(response, "autocomplete-1.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		Collection<WeatherUndergroundLocation> results = client.findLocations("test", "NZ");
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Results available", results);
		assertEquals("Result count", 20, results.size());

		WeatherUndergroundLocation loc = results.iterator().next();
		assertEquals("Identifier", "/q/zmw:00000.2.93436", loc.getIdentifier());
		assertEquals("Name", "Wellington, New Zealand", loc.getName());
		assertEquals("Country", "NZ", loc.getCountry());
		assertNull("StateOrProvince", loc.getStateOrProvince());
		assertNull("Locality", loc.getLocality());
		assertNull("PostalCode", loc.getPostalCode());
		assertEquals("TimeZoneId", "Pacific/Auckland", loc.getTimeZoneId());
		assertEquals("Latitude", new BigDecimal("-41.290001"), loc.getLatitude());
		assertEquals("Longitude", new BigDecimal("174.779999"), loc.getLongitude());
	}

}
