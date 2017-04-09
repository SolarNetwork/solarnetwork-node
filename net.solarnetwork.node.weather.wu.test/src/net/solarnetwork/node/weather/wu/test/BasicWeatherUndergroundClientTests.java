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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
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

	@Test
	public void readConditions() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/TEST_API_KEY/conditions/q/foobar.json",
						request.getPathInfo());
				respondWithJsonResource(response, "conditions-1.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		AtmosphericDatum datum = client.getCurrentConditions("/q/foobar");
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Result available", datum);

		assertEquals("AtmosphericPressure", Integer.valueOf(102100), datum.getAtmosphericPressure());
		assertEquals("Created", new Date(1491542701000L), datum.getCreated());
		assertEquals("DewPoint", new BigDecimal("8.0"), datum.getDewPoint());
		assertEquals("Humidity", Integer.valueOf(72), datum.getHumidity());
		assertEquals("Rain", Integer.valueOf(3), datum.getRain());
		assertEquals("SkyConditions", "Clear", datum.getSkyConditions());
		assertNull("Snow", datum.getSnow());
		assertEquals("Temperature", new BigDecimal("13.2"), datum.getTemperature());
		assertEquals("Visibility", Integer.valueOf(10000), datum.getVisibility());
		assertEquals("WindDirection", Integer.valueOf(7), datum.getWindDirection());
		assertEquals("WindSpeed", new BigDecimal("4.472"), datum.getWindSpeed());

		assertTrue("GeneralAtmosphericDatum", datum instanceof GeneralAtmosphericDatum);
		assertFalse("No forecast tag",
				((GeneralAtmosphericDatum) datum).hasTag(AtmosphericDatum.TAG_FORECAST));
	}

	@Test
	public void getHourlyForecast() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/TEST_API_KEY/hourly/q/foobar.json",
						request.getPathInfo());
				respondWithJsonResource(response, "hourly-1.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		Collection<AtmosphericDatum> results = client.getHourlyForecast("/q/foobar");
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Results available", results);
		assertEquals("Result count", 36, results.size());

		AtmosphericDatum datum = results.iterator().next();
		assertEquals("AtmosphericPressure", Integer.valueOf(102900), datum.getAtmosphericPressure());
		assertEquals("Created", new Date(1491616800000L), datum.getCreated());
		assertEquals("DewPoint", new BigDecimal("8.0"), datum.getDewPoint());
		assertEquals("Humidity", Integer.valueOf(63), datum.getHumidity());
		assertEquals("Rain", Integer.valueOf(4), datum.getRain());
		assertEquals("SkyConditions", "Overcast", datum.getSkyConditions());
		assertEquals("Snow", Integer.valueOf(5), datum.getSnow());
		assertEquals("Temperature", new BigDecimal("16.0"), datum.getTemperature());
		assertNull("Visibility", datum.getVisibility());
		assertEquals("WindDirection", Integer.valueOf(142), datum.getWindDirection());
		assertEquals("WindSpeed", new BigDecimal("2.778"), datum.getWindSpeed());

		assertTrue("GeneralAtmosphericDatum", datum instanceof GeneralAtmosphericDatum);
		assertTrue("Forecast tag",
				((GeneralAtmosphericDatum) datum).hasTag(AtmosphericDatum.TAG_FORECAST));
	}

	@Test
	public void readDay() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/TEST_API_KEY/astronomy/forecast/q/foobar.json",
						request.getPathInfo());
				respondWithJsonResource(response, "forecast-astronomy-1.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		DayDatum datum = client.getCurrentDay("/q/foobar");
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Result available", datum);

		assertEquals("BriefOverview", "A few clouds. Low 7C.", datum.getBriefOverview());
		assertEquals("Created",
				new DateTime(2017, 4, 8, 0, 0, DateTimeZone.forID("Pacific/Auckland")).toDate(),
				datum.getCreated());
		assertEquals("Moonrise", new LocalTime(16, 40), datum.getMoonrise());
		assertEquals("Moonset", new LocalTime(3, 12), datum.getMoonset());
		assertEquals("Rain", Integer.valueOf(5), datum.getRain());
		assertEquals("SkyConditions", "Clear", datum.getSkyConditions());
		assertEquals("Snow", Integer.valueOf(30), datum.getSnow());
		assertEquals("Sunrise", new LocalTime(6, 43), datum.getSunrise());
		assertEquals("Sunset", new LocalTime(18, 1), datum.getSunset());
		assertEquals("TemperatureMaximum", new BigDecimal("16.0"), datum.getTemperatureMaximum());
		assertEquals("TemperatureMinimum", new BigDecimal("7.0"), datum.getTemperatureMinimum());
		assertEquals("WindDirection", Integer.valueOf(31), datum.getWindDirection());
		assertEquals("WindSpeed", new BigDecimal("1.389"), datum.getWindSpeed());

		assertTrue("GeneralDayDatum", datum instanceof GeneralDayDatum);
		assertFalse("No forecast tag", ((GeneralDayDatum) datum).hasTag(DayDatum.TAG_FORECAST));
	}

	@Test
	public void read3DayForecast() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/TEST_API_KEY/forecast/q/foobar.json",
						request.getPathInfo());
				respondWithJsonResource(response, "forecast-1.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		Collection<DayDatum> results = client.getThreeDayForecast("/q/foobar");
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Result available", results);

		assertEquals("3 days of forecast", 3, results.size());

		Iterator<DayDatum> days = results.iterator();
		DayDatum datum = days.next();

		assertEquals("BriefOverview",
				"Sun and a few passing clouds. High around 15C. Winds SSE at 15 to 25 km/h.",
				datum.getBriefOverview());
		assertEquals("Created",
				new DateTime(2017, 4, 8, 0, 0, DateTimeZone.forID("Pacific/Auckland")).toDate(),
				datum.getCreated());
		assertNull("Moonrise", datum.getMoonrise());
		assertNull("Moonset", datum.getMoonset());
		assertEquals("Rain", Integer.valueOf(2), datum.getRain());
		assertEquals("SkyConditions", "Clear", datum.getSkyConditions());
		assertEquals("Snow", Integer.valueOf(40), datum.getSnow());
		assertNull("Sunrise", datum.getSunrise());
		assertNull("Sunset", datum.getSunset());
		assertEquals("TemperatureMaximum", new BigDecimal("15.0"), datum.getTemperatureMaximum());
		assertEquals("TemperatureMinimum", new BigDecimal("7.0"), datum.getTemperatureMinimum());
		assertEquals("WindDirection", Integer.valueOf(151), datum.getWindDirection());
		assertEquals("WindSpeed", new BigDecimal("4.444"), datum.getWindSpeed());

		assertTrue("GeneralDayDatum", datum instanceof GeneralDayDatum);
		assertTrue("Forecast tag", ((GeneralDayDatum) datum).hasTag(DayDatum.TAG_FORECAST));
	}
}
