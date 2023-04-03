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

import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.csv.CsvDatumDataSource;
import net.solarnetwork.node.datum.csv.CsvPropertyConfig;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.test.Assertion;
import net.solarnetwork.util.ByteList;
import net.solarnetwork.web.service.HttpRequestCustomizerService;
import net.solarnetwork.web.service.support.AbstractHttpRequestCustomizerService;
import net.solarnetwork.web.service.support.BasicAuthHttpRequestCustomizerService;

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

		// THEN
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

	@Test
	public void httpRequestFactory() {
		// GIVEN
		dataSource.setUrl(getHttpServerBaseUrl() + "/" + "test-01.csv?date={date}");
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		dataSource.setUrlDateFormat("yyyy-MM-dd");
		dataSource.setHttpRequestFactory(
				new StaticOptionalService<>(new HttpComponentsClientHttpRequestFactory()));

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

		// THEN
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

	@Test
	public void httpRequestCustomizer() {
		// GIVEN
		dataSource.setUrl(getHttpServerBaseUrl() + "/" + "test-01.csv?date={date}");
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		dataSource.setUrlDateFormat("yyyy-MM-dd");
		dataSource.setHttpRequestFactory(
				new StaticOptionalService<>(new HttpComponentsClientHttpRequestFactory()));

		BasicAuthHttpRequestCustomizerService auth = new BasicAuthHttpRequestCustomizerService();
		auth.setUsername("foo");
		auth.setPassword("bar");
		auth.serviceDidStartup();
		dataSource.setHttpRequestCustomizer(new StaticOptionalService<>(auth));

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
				assertThat("Auth header from customizer", request.getHeader(HttpHeaders.AUTHORIZATION),
						is(equalTo("Basic Zm9vOmJhcg==")));
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

		// THEN
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

	@Test
	public void httpRequestCustomizer_placeholderParameters() {
		// GIVEN
		dataSource.setUrl(getHttpServerBaseUrl() + "/" + "test-01.csv?date={date}");
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId("test/{foo}");
		dataSource.setDateTimeColumn("G");
		dataSource.setUrlDateFormat("yyyy-MM-dd");
		dataSource.setHttpRequestFactory(
				new StaticOptionalService<>(new HttpComponentsClientHttpRequestFactory()));

		BasicAuthHttpRequestCustomizerService auth = new BasicAuthHttpRequestCustomizerService();
		auth.setUsername("foo");
		auth.setPassword("bar");
		auth.serviceDidStartup();
		dataSource.setHttpRequestCustomizer(new StaticOptionalService<>(auth));

		final Map<String, Object> placeholders = new HashMap<>(2);
		placeholders.put("foo", "bar");
		placeholders.put("username", "flim");
		placeholders.put("password", "flam");

		PlaceholderService placeholderService = EasyMock.createMock(PlaceholderService.class);
		dataSource.setPlaceholderService(new StaticOptionalService<>(placeholderService));

		// user placeholders as customizer parameters
		placeholderService.copyPlaceholders(assertWith(new Assertion<Map<String, Object>>() {

			@Override
			public void check(Map<String, Object> argument) throws Throwable {
				argument.putAll(placeholders);
			}
		}));

		// resolve source ID placeholders
		expect(placeholderService.resolvePlaceholders(eq(dataSource.getSourceId()), anyObject()))
				.andReturn("test/bar");

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

				HttpHeaders tmp = new HttpHeaders();
				tmp.setBasicAuth("flim", "flam", StandardCharsets.UTF_8);

				assertThat("Auth header from customizer using placeholder credentials",
						request.getHeader(HttpHeaders.AUTHORIZATION),
						is(equalTo(tmp.getFirst(HttpHeaders.AUTHORIZATION))));
				respondWithCsvResource(response, "test-01.csv");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		replay(placeholderService);
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		// THEN
		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID resolved with placeholder", d.getSourceId(), is(equalTo("test/bar")));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));

		verify(placeholderService);
	}

	@Test
	public void httpRequestCustomizer_wrappedRequest() {
		// GIVEN
		dataSource.setUrl(getHttpServerBaseUrl() + "/test-01.csv?date={date}");
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		dataSource.setUrlDateFormat("yyyy-MM-dd");
		dataSource.setHttpRequestFactory(
				new StaticOptionalService<>(new HttpComponentsClientHttpRequestFactory()));

		HttpRequestCustomizerService cust = new AbstractHttpRequestCustomizerService() {

			@Override
			public void configurationChanged(Map<String, Object> properties) {
				// nothing
			}

			@Override
			public String getSettingUid() {
				return "test";
			}

			@Override
			public List<SettingSpecifier> getSettingSpecifiers() {
				return Collections.emptyList();
			}

			@Override
			public HttpRequest customize(HttpRequest request, ByteList body, Map<String, ?> parameters) {
				return new HttpRequestWrapper(request) {

					@Override
					public URI getURI() {
						// change URI
						// @formatter:off
						return UriComponentsBuilder.fromUri(super.getURI())
								.replacePath("/test-02.csv")
								.queryParam("foo", "bar")
								.build().encode().toUri();
						// @formatter:on
					}

				};
			}
		};
		dataSource.setHttpRequestCustomizer(new StaticOptionalService<>(cust));

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
				assertThat("Request path", request.getPathInfo(), is(equalTo("/test-02.csv")));
				assertThat("Date query parameter", request.getParameter("date"),
						is(equalTo(urlQueryDate)));
				assertThat("Foo query parameter", request.getParameter("foo"), is(equalTo("bar")));
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

		// THEN
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
