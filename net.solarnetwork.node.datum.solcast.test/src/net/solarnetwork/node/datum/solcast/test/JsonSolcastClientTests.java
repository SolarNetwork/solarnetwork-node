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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.datum.solcast.JsonSolcastClient;
import net.solarnetwork.node.datum.solcast.SolcastCriteria;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

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
				// @formatter:off
				then(request)
					.as("Method is GET")
					.returns("GET", from(Request::getMethod))
					.as("Path is expected")
					.returns("/data/live/radiation_and_weather", from(r -> r.getHttpURI().getPath()))
					.as("API key included")
					.returns("Bearer " + TEST_API_KEY, from(r -> r.getHeaders().get("Authorization")))
					.extracting(r -> Request.extractQueryParameters(r))
					.as("Latitude param")
					.returns(criteria.getLat().toPlainString(), from(f -> f.getValue("latitude")))
					.as("Longitude param")
					.returns(criteria.getLon().toPlainString(), from(f -> f.getValue("longitude")))
					.as("Period param")
					.returns(criteria.getPeriod().toString(), from(f -> f.getValue("period")))
					.as("PerOutput params param")
					.returns(commaDelimitedStringFromCollection(criteria.getParameters()), from(f -> f.getValue("output_parameters")))
					;
				// @formatter:on
				respondWithJsonResource(request, response, "find-most-recent-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		AtmosphericDatum datum = client.getMostRecentConditions(TEST_SOURCE_ID, criteria);

		// THEN
		// @formatter:off
		then(handler.isHandled())
			.as("Request handled")
			.isTrue()
			;
		then(datum)
			.as("SimpleAtmosphericDatum type returned")
			.isInstanceOf(SimpleAtmosphericDatum.class)
			.as("Created from data")
			.returns(ISO_INSTANT.parse("2022-10-14T01:00:00.000Z", Instant::from), from(AtmosphericDatum::getTimestamp))
			.extracting(d -> d.asSampleOperations())
			.as("Ambient temp mapped to temp")
			.returns(new BigDecimal("10"), from(d -> d.getSampleBigDecimal(Instantaneous, "temp")))
			.as("GHI mapped to irradiance")
			.returns(970, from(d -> d.getSampleInteger(Instantaneous, "irradiance")))
			.as("EBH mapped")
			.returns(869, from(d -> d.getSampleInteger(Instantaneous, "ebh")))
			.as("DNI mapped")
			.returns(976, from(d -> d.getSampleInteger(Instantaneous, "dni")))
			.as("Cloud opacity mapped")
			.returns(0, from(d -> d.getSampleInteger(Instantaneous, "cloud_opacity")))
			;
		// @formatter:on
	}

	@Test
	public void readMostRecentConditions_offsetTimestamps() {
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
				// @formatter:off
				then(request)
					.as("Method is GET")
					.returns("GET", from(Request::getMethod))
					.as("Path is expected")
					.returns("/data/live/radiation_and_weather", from(r -> r.getHttpURI().getPath()))
					.as("API key included")
					.returns("Bearer " + TEST_API_KEY, from(r -> r.getHeaders().get("Authorization")))
					.extracting(r -> Request.extractQueryParameters(r))
					.as("Latitude param")
					.returns(criteria.getLat().toPlainString(), from(f -> f.getValue("latitude")))
					.as("Longitude param")
					.returns(criteria.getLon().toPlainString(), from(f -> f.getValue("longitude")))
					.as("Period param")
					.returns(criteria.getPeriod().toString(), from(f -> f.getValue("period")))
					.as("PerOutput params param")
					.returns(commaDelimitedStringFromCollection(criteria.getParameters()), from(f -> f.getValue("output_parameters")))
					;
				// @formatter:on
				respondWithJsonResource(request, response, "find-most-recent-02.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		AtmosphericDatum datum = client.getMostRecentConditions(TEST_SOURCE_ID, criteria);

		// THEN
		// @formatter:off
		then(handler.isHandled())
			.as("Request handled")
			.isTrue()
			;
		then(datum)
			.as("SimpleAtmosphericDatum type returned")
			.isInstanceOf(SimpleAtmosphericDatum.class)
			.as("Created from data")
			.returns(ISO_INSTANT.parse("2022-10-14T01:00:00+01:30", Instant::from), from(AtmosphericDatum::getTimestamp))
			.extracting(d -> d.asSampleOperations())
			.as("Ambient temp mapped to temp")
			.returns(new BigDecimal("10.1"), from(d -> d.getSampleBigDecimal(Instantaneous, "temp")))
			.as("GHI mapped to irradiance")
			.returns(970, from(d -> d.getSampleInteger(Instantaneous, "irradiance")))
			.as("EBH mapped")
			.returns(869, from(d -> d.getSampleInteger(Instantaneous, "ebh")))
			.as("DNI mapped")
			.returns(976, from(d -> d.getSampleInteger(Instantaneous, "dni")))
			.as("Cloud opacity mapped")
			.returns(0, from(d -> d.getSampleInteger(Instantaneous, "cloud_opacity")))
			;
		// @formatter:on
	}

}
