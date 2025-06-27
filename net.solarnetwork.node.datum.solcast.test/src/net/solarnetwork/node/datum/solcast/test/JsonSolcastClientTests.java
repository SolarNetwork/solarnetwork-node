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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.solcast.JsonSolcastClient;
import net.solarnetwork.node.datum.solcast.SolcastCriteria;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;
import net.solarnetwork.util.StringUtils;

/**
 * Test cases for the {@link JsonSolcastClient} class.
 *
 * @author matt
 * @version 1.0
 */
public class JsonSolcastClientTests extends AbstractHttpServerTests {

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
	public void readMostRecentConditions() {
		// GIVEN
		final SolcastCriteria criteria = new SolcastCriteria();
		criteria.setLat(new BigDecimal("1.2345"));
		criteria.setLon(new BigDecimal("2.3456"));
		criteria.setParameters(new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
		criteria.setPeriod(Duration.ofMinutes(10));

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), is(equalTo("GET")));
				assertThat("Request path", request.getHttpURI().getPath(),
						is(equalTo("/data/live/radiation_and_weather")));
				assertThat("API key", request.getHeaders().get("Authorization"),
						is(equalTo("Bearer " + TEST_API_KEY)));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Latitude param", queryParams.getValue("latitude"),
						is(equalTo(criteria.getLat().toPlainString())));
				assertThat("Longitude param", queryParams.getValue("longitude"),
						is(equalTo(criteria.getLon().toPlainString())));
				assertThat("Period param", queryParams.getValue("period"),
						is(equalTo(criteria.getPeriod().toString())));
				assertThat("Output params param", queryParams.getValue("output_parameters"), is(equalTo(
						StringUtils.commaDelimitedStringFromCollection(criteria.getParameters()))));
				respondWithJsonResource(request, response, "find-most-recent-01.json");
				return true;
			}

		};
		addHandler(handler);

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
