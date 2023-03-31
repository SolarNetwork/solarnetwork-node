/* ==================================================================
 * CsvDatumDataSourceHttpTests.java - 1/04/2023 9:37:56 am
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

package net.solarnetwork.node.datum.csv.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.csv.CsvDatumDataSource;
import net.solarnetwork.node.datum.csv.CsvPropertyConfig;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Test cases for the {@link CsvDatumDataSource} class using HTTP resources.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvDatumDataSourceHttpTests extends AbstractHttpClientTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private CsvDatumDataSource dataSource;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		dataSource = new CsvDatumDataSource();
		dataSource.setCharset(StandardCharsets.UTF_8);
	}

	@Test
	public void urlDateParameter() {
		// GIVEN
		dataSource.setUrl(getHttpServerBaseUrl() + "/" + "test-01.csv?date={date}");
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		dataSource.setUrlDateFormat("yyyy-MM-dd");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		final String urlQueryDate = DateTimeFormatter.ofPattern(dataSource.getUrlDateFormat())
				.withZone(ZoneId.systemDefault()).format(Instant.now());
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), is(equalTo("GET")));
				assertThat("Request path", request.getPathInfo(), is(equalTo("/test-01.csv")));
				assertThat("Date query parameter", request.getParameter("date"),
						is(equalTo(urlQueryDate)));
				respondWithCsvResource(response, "test-01.csv");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

}
