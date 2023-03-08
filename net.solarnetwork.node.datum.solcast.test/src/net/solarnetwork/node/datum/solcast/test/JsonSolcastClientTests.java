/* ==================================================================
 * JsonSolcastClientTests.java - 14/10/2022 2:32:11 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.solcast.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.solcast.JsonSolcastClient;
import net.solarnetwork.node.datum.solcast.SolcastCriteria;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.util.StringUtils;

/**
 * Test cases for the {@link JsonSolcastClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JsonSolcastClientTests extends AbstractHttpClientTests {

	private static final String TEST_API_KEY = UUID.randomUUID().toString();
	private static final String TEST_SOURCE_ID = UUID.randomUUID().toString();

	private JsonSolcastClient client;

	@Before
	public void setupClient() {
		client = new JsonSolcastClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.setApiKey(TEST_API_KEY);
	}

	@Test
	public void readMostRecentConditions() throws Exception {
		// GIVEN
		final SolcastCriteria criteria = new SolcastCriteria();
		criteria.setLat(new BigDecimal("1.2345"));
		criteria.setLon(new BigDecimal("2.3456"));
		criteria.setParameters(new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
		criteria.setPeriod(Duration.ofMinutes(10));

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), is(equalTo("GET")));
				assertThat("Request path", request.getPathInfo(),
						is(equalTo("/data/live/radiation_and_weather")));
				assertThat("API key", request.getHeader("Authorization"),
						is(equalTo("Bearer " + TEST_API_KEY)));
				assertThat("Latitude param", request.getParameter("latitude"),
						is(equalTo(criteria.getLat().toPlainString())));
				assertThat("Longitude param", request.getParameter("longitude"),
						is(equalTo(criteria.getLon().toPlainString())));
				assertThat("Period param", request.getParameter("period"),
						is(equalTo(criteria.getPeriod().toString())));
				assertThat("Output params param", request.getParameter("output_parameters"), is(equalTo(
						StringUtils.commaDelimitedStringFromCollection(criteria.getParameters()))));
				respondWithJsonResource(response, "find-most-recent-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		AtmosphericDatum datum = client.getMostRecentConditions(TEST_SOURCE_ID, criteria);

		// THEN
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("SimpleAtmosphericDatum", datum, instanceOf(SimpleAtmosphericDatum.class));

		Instant ts = DateTimeFormatter.ISO_INSTANT.parse("2022-10-14T01:00:00.000Z", Instant::from);
		assertThat("Created", datum.getTimestamp(), is(equalTo(ts)));
		assertThat("Period mapped to duration",
				datum.asSampleOperations().getSampleLong(DatumSamplesType.Instantaneous, "duration"),
				is(equalTo(Duration.ofMinutes(30).getSeconds())));
		assertThat("Ambient temp mapped to temp", datum.getTemperature(),
				is(equalTo(new BigDecimal("10"))));
		assertThat("GHI mapped to irradiance", datum.asSampleOperations()
				.getSampleInteger(DatumSamplesType.Instantaneous, "irradiance"), is(equalTo(970)));
		assertThat("EBH",
				datum.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "ebh"),
				is(equalTo(869)));
		assertThat("DNI",
				datum.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "dni"),
				is(equalTo(976)));
		assertThat("Cloud opacity", datum.asSampleOperations()
				.getSampleInteger(DatumSamplesType.Instantaneous, "cloud_opacity"), is(equalTo(0)));
	}

}
