/* ==================================================================
 * BasicEm6ApiClientTests.java - 10/03/2023 1:04:23 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.co2.nz.em6.test;

import static java.lang.String.format;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.datum.co2.nz.em6.BasicEm6ApiClient;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test casees for the {@link BasicEm6ApiClient} class.
 *
 * @author matt
 * @version 2.0
 */
public class BasicEm6ApiClientTests extends AbstractHttpServerTests {

	private BasicEm6ApiClient client;

	@Before
	public void setupClient() {
		client = new BasicEm6ApiClient(URI.create(getHttpServerBaseUrl()), JsonUtils.newObjectMapper());
	}

	@Test
	public void currentCarbonIntensity() {
		// GIVEN
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo(BasicEm6ApiClient.CURRENT_CARBON_INTENSITY_PATH));
				respondWithJsonResource(request, response, "current_carbon_intensity-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		Collection<NodeDatum> results = client.currentCarbonIntensity();
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("Results provided", results, hasSize(3));

		Instant start = DateTimeFormatter.ISO_INSTANT.parse("2023-03-07T16:00:00Z", Instant::from);
		int i = 0;
		for ( NodeDatum datum : results ) {
			assertThat(format("Datum %d created", i), datum.getTimestamp(),
					is(equalTo(start.minus(i * 30L, ChronoUnit.MINUTES))));
			assertThat("Datum is location type", datum.getKind(), is(ObjectDatumKind.Location));
			assertThat("Datum location ID not provided", datum.getObjectId(), is(nullValue()));
			assertThat("Datum source ID not provided", datum.getSourceId(), is(nullValue()));

			log.debug("Got datum: {} ", datum);
			DatumSamplesOperations ops = datum.asSampleOperations();
			if ( i == 0 ) {
				assertThat(format("Datum %d roperties parsed", i),
						ops.getSampleData(Instantaneous).keySet(), hasSize(9));
				assertThat(format("Datum %d co2_g", i), ops.getSampleInteger(Instantaneous, "co2_g"),
						is(equalTo(152350)));
				assertThat(format("Datum %d co2_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "co2_gkwh"),
						is(equalTo(new BigDecimal("85.56"))));
				assertThat(format("Datum %d co2_change_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "co2_change_gkwh"),
						is(equalTo(new BigDecimal("2.86"))));
				assertThat(format("Datum %d renewable", i),
						ops.getSampleBigDecimal(Instantaneous, "renewable"),
						is(equalTo(new BigDecimal("88.35"))));
				assertThat(format("Datum %d max_24hrs_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "max_24hrs_gkwh"),
						is(equalTo(new BigDecimal("113.53"))));
				assertThat(format("Datum %d min_24hrs_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "min_24hrs_gkwh"),
						is(equalTo(new BigDecimal("82.7"))));
				assertThat(format("Datum %d current_month_avg_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "current_month_avg_gkwh"),
						is(equalTo(new BigDecimal("99.07"))));
				assertThat(format("Datum %d current_year_avg_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "current_year_avg_gkwh"),
						is(equalTo(new BigDecimal("73.73"))));
				assertThat(format("Datum %d pct_current_year_gkwh", i),
						ops.getSampleBigDecimal(Instantaneous, "pct_current_year_gkwh"),
						is(equalTo(new BigDecimal("61.67"))));
			}

			i++;
		}
	}

}
