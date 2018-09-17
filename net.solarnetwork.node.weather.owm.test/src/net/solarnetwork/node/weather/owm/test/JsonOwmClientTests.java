/* ==================================================================
 * JsonOwmClientTests.java - 17/09/2018 1:42:15 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.owm.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
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
import net.solarnetwork.node.weather.owm.JsonOwmClient;

/**
 * Test cases for the {@link JsonOwmClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JsonOwmClientTests extends AbstractHttpClientTests {

	private static final String TEST_API_KEY = "TEST_API_KEY";

	private JsonOwmClient client;

	@Before
	public void setupClient() {
		client = new JsonOwmClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.setApiKey(TEST_API_KEY);
	}

	@Test
	public void readDay() throws Exception {
		final String owmLocationId = "foobar.loc.id";

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo("/data/2.5/weather"));
				assertThat("API key", request.getParameter("appid"), equalTo(TEST_API_KEY));
				assertThat("Location ID", request.getParameter("id"), equalTo(owmLocationId));
				assertThat("Units", request.getParameter("units"), equalTo("metric"));
				assertThat("Mode", request.getParameter("mode"), equalTo("json"));
				respondWithJsonResource(response, "weather-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		DayDatum datum = client.getCurrentDay(owmLocationId, "Pacific/Auckland");
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("GeneralDayDatum", datum instanceof GeneralDayDatum, equalTo(true));

		assertThat("Created", datum.getCreated(), equalTo(
				new DateTime(2018, 9, 17, 0, 0, DateTimeZone.forID("Pacific/Auckland")).toDate()));

		assertThat("Sunrise", datum.getSunrise(), equalTo(new LocalTime(6, 19)));
		assertThat("Sunset", datum.getSunset(), equalTo(new LocalTime(18, 11)));

		assertThat("No forecast tag", ((GeneralDayDatum) datum).hasTag(DayDatum.TAG_FORECAST),
				equalTo(false));
	}

	@Test
	public void readForecast() throws Exception {
		final String owmLocationId = "foobar.loc.id";

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo("/data/2.5/forecast"));
				assertThat("API key", request.getParameter("appid"), equalTo(TEST_API_KEY));
				assertThat("Location ID", request.getParameter("id"), equalTo(owmLocationId));
				assertThat("Units", request.getParameter("units"), equalTo("metric"));
				assertThat("Mode", request.getParameter("mode"), equalTo("json"));
				respondWithJsonResource(response, "forecast-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		Collection<AtmosphericDatum> results = client.getHourlyForecast(owmLocationId);
		assertTrue("Request handled", handler.isHandled());
		assertNotNull("Result available", results);

		assertThat("Forecast resultcount", results, hasSize(40));

		AtmosphericDatum datum = results.iterator().next();
		assertThat("Created", datum.getCreated(), equalTo(new Date(1537142400000L)));
		assertThat("Temperature", datum.getTemperature(), equalTo(new BigDecimal("15.44")));
		assertThat("Temperature min", datum.getSampleData().get("tempMin"),
				equalTo(new BigDecimal("13.72")));
		assertThat("Temperature min", datum.getSampleData().get("tempMax"),
				equalTo(new BigDecimal("15.44")));
		assertThat("AtmosphericPressure", datum.getAtmosphericPressure(), equalTo(102513));
		assertThat("Humidity", datum.getHumidity(), equalTo(88));
		assertThat("SkyConditions", datum.getSkyConditions(), equalTo("Rain"));
		assertThat("Icon ID", datum.getSampleData().get("iconId"), equalTo("10d"));
		assertThat("WindSpeed", datum.getWindSpeed(), equalTo(new BigDecimal("10.31")));
		assertThat("WindDirection", datum.getWindDirection(), equalTo(341));

		assertThat("Rain", datum.getRain(), equalTo(0));

		assertTrue("GeneralAtmosphericDatum", datum instanceof GeneralAtmosphericDatum);
		assertTrue("Forecast tag",
				((GeneralAtmosphericDatum) datum).hasTag(AtmosphericDatum.TAG_FORECAST));
	}

}
