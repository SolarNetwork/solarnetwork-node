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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;
import net.solarnetwork.node.weather.owm.JsonOwmClient;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test cases for the {@link JsonOwmClient} class.
 *
 * @author matt
 * @version 3.0
 */
public class JsonOwmClientTests extends AbstractHttpServerTests {

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
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(), equalTo("/data/2.5/weather"));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("API key", queryParams.getValue("appid"), equalTo(TEST_API_KEY));
				assertThat("Location ID", queryParams.getValue("id"), equalTo(owmLocationId));
				assertThat("Units", queryParams.getValue("units"), equalTo("metric"));
				assertThat("Mode", queryParams.getValue("mode"), equalTo("json"));
				respondWithJsonResource(request, response, "weather-01.json");
				return true;
			}

		};
		addHandler(handler);

		DayDatum datum = client.getCurrentDay(owmLocationId, "Pacific/Auckland");
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("SimpleDayDatum", datum, instanceOf(SimpleDayDatum.class));

		Instant ts = ZonedDateTime.of(2018, 9, 17, 0, 0, 0, 0, ZoneId.of("Pacific/Auckland"))
				.toInstant();
		assertThat("Created", datum.getTimestamp(), is(ts));

		assertThat("Sunrise", datum.getSunriseTime(), is(LocalTime.of(6, 19)));
		assertThat("Sunset", datum.getSunsetTime(), is(LocalTime.of(18, 11)));

		assertThat("No forecast tag", datum.asSampleOperations().hasTag(DayDatum.TAG_FORECAST),
				equalTo(false));
	}

	@Test
	public void readForecast() throws Exception {
		final String owmLocationId = "foobar.loc.id";

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/data/2.5/forecast"));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("API key", queryParams.getValue("appid"), equalTo(TEST_API_KEY));
				assertThat("Location ID", queryParams.getValue("id"), equalTo(owmLocationId));
				assertThat("Units", queryParams.getValue("units"), equalTo("metric"));
				assertThat("Mode", queryParams.getValue("mode"), equalTo("json"));
				respondWithJsonResource(request, response, "forecast-01.json");
				return true;
			}

		};
		addHandler(handler);

		Collection<AtmosphericDatum> results = client.getHourlyForecast(owmLocationId);
		assertTrue("Request handled", handler.isHandled());
		assertThat("Forecast resultcount", results, hasSize(40));

		AtmosphericDatum datum = results.iterator().next();
		assertThat("SimpleAtmosphericDatum", datum, instanceOf(SimpleAtmosphericDatum.class));
		assertThat("Created", datum.getTimestamp(), equalTo(Instant.ofEpochMilli(1537142400000L)));
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
		assertThat("Visibility", datum.getVisibility(), nullValue());

		assertThat("Rain", datum.getRain(), equalTo(0));

		assertThat("Forecast tag", datum.asSampleOperations().hasTag(AtmosphericDatum.TAG_FORECAST),
				equalTo(true));
	}

	@Test
	public void readConditions() throws Exception {
		final String owmLocationId = "foobar.loc.id";

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(), equalTo("/data/2.5/weather"));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("API key", queryParams.getValue("appid"), equalTo(TEST_API_KEY));
				assertThat("Location ID", queryParams.getValue("id"), equalTo(owmLocationId));
				assertThat("Units", queryParams.getValue("units"), equalTo("metric"));
				assertThat("Mode", queryParams.getValue("mode"), equalTo("json"));
				respondWithJsonResource(request, response, "weather-01.json");
				return true;
			}

		};
		addHandler(handler);

		AtmosphericDatum datum = client.getCurrentConditions(owmLocationId);
		assertTrue("Request handled", handler.isHandled());

		assertThat("SimpleAtmosphericDatum", datum, instanceOf(SimpleAtmosphericDatum.class));
		assertThat("Created", datum.getTimestamp(), equalTo(Instant.ofEpochMilli(1537138800000L)));
		assertThat("Temperature", datum.getTemperature(), equalTo(new BigDecimal("14.0")));
		assertThat("Temperature min", datum.getSampleData().get("tempMin"), nullValue());
		assertThat("Temperature min", datum.getSampleData().get("tempMax"), nullValue());
		assertThat("AtmosphericPressure", datum.getAtmosphericPressure(), equalTo(101600));
		assertThat("Humidity", datum.getHumidity(), equalTo(87));
		assertThat("SkyConditions", datum.getSkyConditions(), equalTo("Rain"));
		assertThat("Icon ID", datum.getSampleData().get("iconId"), equalTo("10n"));
		assertThat("WindSpeed", datum.getWindSpeed(), equalTo(new BigDecimal("9.8")));
		assertThat("WindDirection", datum.getWindDirection(), equalTo(350));
		assertThat("Wind gust", datum.getSampleData().get("wgust"), equalTo(new BigDecimal("14.9")));
		assertThat("Visibility", datum.getVisibility(), equalTo(10000));

		assertThat("Rain", datum.getRain(), nullValue());

		assertThat("No tags", datum.asSampleOperations().getTags(), nullValue());
	}

}
