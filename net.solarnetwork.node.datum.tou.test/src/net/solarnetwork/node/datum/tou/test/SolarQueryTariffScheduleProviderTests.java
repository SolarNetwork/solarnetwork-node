/* ==================================================================
 * SolarQueryTariffScheduleProviderTests.java - 12/08/2024 10:06:02 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.tou.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.io.UrlUtils;
import net.solarnetwork.node.datum.tou.SolarQueryTariffScheduleProvider;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.web.service.HttpRequestCustomizerService;

/**
 * Test cases for the {@link SolarQueryTariffScheduleProvider} class.
 *
 * @author matt
 * @version 1.0
 */
public class SolarQueryTariffScheduleProviderTests {

	private static final String JSON_TYPE = "application/json";

	private Clock clock;
	private SetupService setupService;
	private IdentityService identityService;
	private ClientHttpRequestFactory httpRequestFactory;
	private HttpRequestCustomizerService httpRequestCustomizer;
	private SolarQueryTariffScheduleProvider service;

	@Before
	public void setup() {
		clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
		setupService = EasyMock.createMock(SetupService.class);
		identityService = EasyMock.createMock(IdentityService.class);

		httpRequestFactory = EasyMock.createMock(ClientHttpRequestFactory.class);

		httpRequestCustomizer = EasyMock.createMock(HttpRequestCustomizerService.class);

		service = new SolarQueryTariffScheduleProvider(clock, JsonUtils.newDatumObjectMapper(),
				JSON_TYPE, new StaticOptionalService<>(setupService),
				new StaticOptionalService<>(identityService),
				new StaticOptionalService<>(httpRequestFactory));
		service.setHttpRequestCustomizer(new StaticOptionalService<>(httpRequestCustomizer));
	}

	public void replayAll() {
		EasyMock.replay(setupService, identityService, httpRequestFactory, httpRequestCustomizer);
	}

	@After
	public void teardown() {
		EasyMock.verify(setupService, identityService, httpRequestFactory, httpRequestCustomizer);
	}

	@Test
	public void dow_fixedDateRange() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		final Set<String> datumProps = new HashSet<>(Arrays.asList("consCurr", "prodTotal"));

		service.setAggregation(Aggregation.DayOfWeek);
		service.setSourceId(sourceId);
		service.setStartDate(startDate);
		service.setEndDate(endDate);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		// discover node ID
		expect(identityService.getNodeId()).andReturn(nodeId);

		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(Aggregation.DayOfWeek.name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		final URI queryUri = URI.create(queryUrl.toString());

		final MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, queryUri);
		final MockClientHttpResponse res = new MockClientHttpResponse(
				getClass().getResourceAsStream("test-datum-stream-dow-01.json"), HttpStatus.OK);
		req.setResponse(res);

		// create request
		expect(httpRequestFactory.createRequest(queryUri, HttpMethod.GET)).andReturn(req);

		// invoke customizer
		expect(httpRequestCustomizer.customize(same(req), isNull(), anyObject())).andReturn(req);

		// WHEN
		replayAll();
		TariffSchedule result = service.tariffSchedule();

		// THEN
		assertThat("Accept request header added", req.getHeaders().getFirst(HttpHeaders.ACCEPT),
				is(equalTo(JSON_TYPE)));
		assertThat("Accept-Encoding request header added for gzip compression",
				req.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING), is(equalTo("gzip")));

		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));

	}

}
