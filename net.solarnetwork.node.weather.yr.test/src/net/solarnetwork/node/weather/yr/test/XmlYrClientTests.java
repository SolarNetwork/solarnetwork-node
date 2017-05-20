/* ==================================================================
 * XmlYrClientTests.java - 19/05/2017 4:11:04 PM
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

package net.solarnetwork.node.weather.yr.test;

import static net.solarnetwork.node.domain.AtmosphericDatum.TAG_FORECAST;
import static net.solarnetwork.node.weather.yr.YrAtmosphericDatum.SYMBOL_VAR_KEY;
import static net.solarnetwork.node.weather.yr.YrAtmosphericDatum.VALID_TO_KEY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.weather.yr.XmlYrClient;
import net.solarnetwork.node.weather.yr.YrAtmosphericDatum;
import net.solarnetwork.node.weather.yr.YrDayDatum;

/**
 * Test cases for the {@link XmlYrClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class XmlYrClientTests extends AbstractHttpClientTests {

	private static final String NZ_TZ_ID = "Pacific/Auckland";

	private static final String TEST_YR_LOC_IDENTIFIER = "/test/identifier";

	private XmlYrClient client;

	@Before
	public void setupClient() {
		client = new XmlYrClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.init();
	}

	@Test
	public void getHourlyForecast() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/place/test/identifier/forecast_hour_by_hour.xml",
						request.getPathInfo());
				respondWithXmlResource(response, "forecast_hour_by_hour-01.xml");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		List<AtmosphericDatum> results = client.getHourlyForecast(TEST_YR_LOC_IDENTIFIER);
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("Result count", results, hasSize(48));

		YrAtmosphericDatum data = (YrAtmosphericDatum) results.get(0);
		assertThat("Location", data.getLocation(), notNullValue());
		assertThat("Location identifier", data.getLocation().getIdentifier(),
				equalTo(TEST_YR_LOC_IDENTIFIER));
		assertThat("Location tz", data.getLocation().getTimeZoneId(), equalTo(NZ_TZ_ID));
		assertThat("Creation date", data.getCreated(),
				equalTo(new DateTime(2017, 05, 10, 11, 00, 00, DateTimeZone.forID(NZ_TZ_ID)).toDate()));
		assertThat("Location ID", data.getLocationId(), nullValue());
		assertThat("Source ID", data.getSourceId(), nullValue());
		assertThat("Forecast tag", data.hasTag(TAG_FORECAST), equalTo(true));
		assertThat("Atmospheric pressure", data.getAtmosphericPressure(), equalTo(102660));
		assertThat("Rain", data.getRain(), equalTo(1));
		assertThat("Sky conditions", data.getSkyConditions(), equalTo("Partly cloudy"));
		assertThat("Symbol vr", data.getStatusSampleString(SYMBOL_VAR_KEY), equalTo("03d"));
		assertThat("Temperature", data.getTemperature(), equalTo(new BigDecimal("8")));
		assertThat("Wind direction", data.getWindDirection(), equalTo(151));
		assertThat("Wind speed", data.getWindSpeed(), equalTo(new BigDecimal("0.6")));
		assertThat("Valid to", data.getStatusSampleString(VALID_TO_KEY),
				equalTo("2017-05-10T00:00:00.000Z"));

		// verify rounding wind direction
		data = (YrAtmosphericDatum) results.get(4);
		assertThat("Wind direction", data.getWindDirection(), equalTo(139));
	}

	@Test
	public void getTenDayForecast() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/place/test/identifier/forecast.xml",
						request.getPathInfo());
				respondWithXmlResource(response, "forecast-01.xml");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		List<DayDatum> results = client.getTenDayForecast(TEST_YR_LOC_IDENTIFIER);
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("Result count", results, hasSize(9));

		YrDayDatum data = (YrDayDatum) results.get(0);
		assertThat("Location", data.getLocation(), notNullValue());
		assertThat("Location identifier", data.getLocation().getIdentifier(),
				equalTo(TEST_YR_LOC_IDENTIFIER));
		assertThat("Location tz", data.getLocation().getTimeZoneId(), equalTo(NZ_TZ_ID));
		assertThat("Creation date", data.getCreated(),
				equalTo(new DateTime(2017, 05, 11, 00, 00, 00, DateTimeZone.forID(NZ_TZ_ID)).toDate()));
		assertThat("Location ID", data.getLocationId(), nullValue());
		assertThat("Source ID", data.getSourceId(), nullValue());
		assertThat("Forecast tag", data.hasTag(DayDatum.TAG_FORECAST), equalTo(true));
		assertThat("Rain", data.getRain(), equalTo(15));
		assertThat("Sky conditions", data.getSkyConditions(), equalTo("Heavy rain"));
		assertThat("Symbol vr", data.getStatusSampleString(YrDayDatum.SYMBOL_VAR_KEY), equalTo("10"));
		assertThat("Temperature max", data.getTemperatureMaximum(), equalTo(new BigDecimal("10")));
		assertThat("Temperature min", data.getTemperatureMinimum(), equalTo(new BigDecimal("8")));
		assertThat("Wind direction", data.getWindDirection(), equalTo(225));
		assertThat("Wind speed", data.getWindSpeed(), equalTo(new BigDecimal("1.0")));

		// verify most frequent wins for sky conditions
		data = (YrDayDatum) results.get(3);
		assertThat("Creation date", data.getCreated(),
				equalTo(new DateTime(2017, 05, 14, 00, 00, 00, DateTimeZone.forID(NZ_TZ_ID)).toDate()));
		assertThat("Sky conditions", data.getSkyConditions(), equalTo("Partly cloudy"));
	}

}
