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

import static net.solarnetwork.util.IntRange.rangeOf;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collection;
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
import net.solarnetwork.domain.tariff.ChronoFieldsTariff;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.io.UrlUtils;
import net.solarnetwork.node.datum.tou.SolarQueryTariffScheduleProvider;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.IntRangeSet;
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
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
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
		Collection<? extends Tariff> rules = result.rules();
		assertThat("Schedule has one rule per datum result (day of week)", rules, hasSize(7));
		int dow = 0;
		for ( Tariff tariff : rules ) {
			dow++;
			assertThat("Tariff is a ChronoFieldsTariff", tariff, instanceOf(ChronoFieldsTariff.class));
			ChronoFieldsTariff cft = (ChronoFieldsTariff) tariff;
			assertThat("No month range for DOW query",
					cft.rangeForChronoField(ChronoField.MONTH_OF_YEAR), is(nullValue()));
			assertThat("No day range for DOW query", cft.rangeForChronoField(ChronoField.DAY_OF_MONTH),
					is(nullValue()));
			assertThat("Singleton weekday range for DOW query",
					cft.rangeForChronoField(ChronoField.DAY_OF_WEEK), is(new IntRangeSet(rangeOf(dow))));
			assertThat("No minute range for DOW query",
					cft.rangeForChronoField(ChronoField.MINUTE_OF_DAY), is(nullValue()));
		}
	}

	@Test
	public void relativeDateRange_truncateDay() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final Set<String> datumProps = new HashSet<>(Arrays.asList("consCurr", "prodTotal"));

		service.setAggregation(Aggregation.DayOfWeek);
		service.setSourceId(sourceId);
		service.setStartDateOffset(Period.ofYears(-1));
		service.setStartDateOffsetTruncateUnit(ChronoUnit.DAYS);
		service.setEndDateOffset(Duration.ZERO);
		service.setEndDateOffsetTruncateUnit(ChronoUnit.DAYS);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		// discover node ID
		expect(identityService.getNodeId()).andReturn(nodeId);

		// resolved date range is 1 year to start of today
		final LocalDateTime expectedStartDate = LocalDateTime.now(clock)
				.plus(service.getStartDateOffset())
				.truncatedTo(service.getStartDateOffsetTruncateUnit());
		final LocalDateTime expectedEndDate = LocalDateTime.now(clock).plus(service.getEndDateOffset())
				.truncatedTo(service.getEndDateOffsetTruncateUnit());
		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(expectedStartDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(expectedEndDate));

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
		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));
	}

	@Test
	public void doy() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		final Set<String> datumProps = new HashSet<>(Arrays.asList("wattHours"));

		service.setNodeId(nodeId);
		service.setAggregation(Aggregation.DayOfYear);
		service.setSourceId(sourceId);
		service.setStartDate(startDate);
		service.setEndDate(endDate);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		final URI queryUri = URI.create(queryUrl.toString());

		final MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, queryUri);
		final MockClientHttpResponse res = new MockClientHttpResponse(
				getClass().getResourceAsStream("test-datum-stream-doy-01.json"), HttpStatus.OK);
		req.setResponse(res);

		// create request
		expect(httpRequestFactory.createRequest(queryUri, HttpMethod.GET)).andReturn(req);

		// invoke customizer
		expect(httpRequestCustomizer.customize(same(req), isNull(), anyObject())).andReturn(req);

		// WHEN
		replayAll();
		TariffSchedule result = service.tariffSchedule();

		// THEN
		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));
		Collection<? extends Tariff> rules = result.rules();
		assertThat("Schedule has one rule per datum result (day of year, leap day included)", rules,
				hasSize(366));

		LocalDate day = LocalDate.of(1996, 1, 1);
		for ( Tariff tariff : rules ) {
			assertThat("Tariff is a ChronoFieldsTariff", tariff, instanceOf(ChronoFieldsTariff.class));
			ChronoFieldsTariff cft = (ChronoFieldsTariff) tariff;
			assertThat("Month range is singleton for DOY query",
					cft.rangeForChronoField(ChronoField.MONTH_OF_YEAR),
					is(new IntRangeSet(rangeOf(day.getMonthValue()))));
			assertThat("Day range is singleton DOY query",
					cft.rangeForChronoField(ChronoField.DAY_OF_MONTH),
					is(new IntRangeSet(rangeOf(day.getDayOfMonth()))));
			assertThat("No weekday range for DOY query",
					cft.rangeForChronoField(ChronoField.DAY_OF_WEEK), is(nullValue()));
			assertThat("No minute range for DOY query",
					cft.rangeForChronoField(ChronoField.MINUTE_OF_DAY), is(nullValue()));
			day = day.plusDays(1);
		}
	}

	@Test
	public void hoy() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		final Set<String> datumProps = new HashSet<>(Arrays.asList("wattHours"));

		service.setNodeId(nodeId);
		service.setAggregation(Aggregation.HourOfYear);
		service.setSourceId(sourceId);
		service.setStartDate(startDate);
		service.setEndDate(endDate);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		final URI queryUri = URI.create(queryUrl.toString());

		final MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, queryUri);
		final MockClientHttpResponse res = new MockClientHttpResponse(
				getClass().getResourceAsStream("test-datum-stream-hoy-01.json"), HttpStatus.OK);
		req.setResponse(res);

		// create request
		expect(httpRequestFactory.createRequest(queryUri, HttpMethod.GET)).andReturn(req);

		// invoke customizer
		expect(httpRequestCustomizer.customize(same(req), isNull(), anyObject())).andReturn(req);

		// WHEN
		replayAll();
		TariffSchedule result = service.tariffSchedule();

		// THEN
		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));
		Collection<? extends Tariff> rules = result.rules();
		assertThat("Schedule has one rule per datum result (hour of year, leap day included)", rules,
				hasSize(366 * 24));

		LocalDateTime hour = LocalDateTime.of(1996, 1, 1, 0, 0);
		for ( Tariff tariff : rules ) {
			assertThat("Tariff is a ChronoFieldsTariff", tariff, instanceOf(ChronoFieldsTariff.class));
			ChronoFieldsTariff cft = (ChronoFieldsTariff) tariff;
			assertThat("Month range is singleton for DOY query",
					cft.rangeForChronoField(ChronoField.MONTH_OF_YEAR),
					is(new IntRangeSet(rangeOf(hour.getMonthValue()))));
			assertThat("Day range is singleton HOY query",
					cft.rangeForChronoField(ChronoField.DAY_OF_MONTH),
					is(new IntRangeSet(rangeOf(hour.getDayOfMonth()))));
			assertThat("No weekday range for HOY query",
					cft.rangeForChronoField(ChronoField.DAY_OF_WEEK), is(nullValue()));
			assertThat("Minute range is hour for HOY query",
					cft.rangeForChronoField(ChronoField.MINUTE_OF_DAY),
					is(new IntRangeSet(rangeOf(hour.getHour() * 60, hour.getHour() * 60 + 60))));
			hour = hour.plusHours(1);
		}
	}

	@Test
	public void hod() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		final Set<String> datumProps = new HashSet<>(Arrays.asList("wattHours"));

		service.setNodeId(nodeId);
		service.setAggregation(Aggregation.HourOfDay);
		service.setSourceId(sourceId);
		service.setStartDate(startDate);
		service.setEndDate(endDate);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		final URI queryUri = URI.create(queryUrl.toString());

		final MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, queryUri);
		final MockClientHttpResponse res = new MockClientHttpResponse(
				getClass().getResourceAsStream("test-datum-stream-hod-01.json"), HttpStatus.OK);
		req.setResponse(res);

		// create request
		expect(httpRequestFactory.createRequest(queryUri, HttpMethod.GET)).andReturn(req);

		// invoke customizer
		expect(httpRequestCustomizer.customize(same(req), isNull(), anyObject())).andReturn(req);

		// WHEN
		replayAll();
		TariffSchedule result = service.tariffSchedule();

		// THEN
		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));
		Collection<? extends Tariff> rules = result.rules();
		assertThat("Schedule has one rule per datum result (hour of day)", rules, hasSize(24));

		LocalTime hour = LocalTime.of(0, 0);
		for ( Tariff tariff : rules ) {
			assertThat("Tariff is a ChronoFieldsTariff", tariff, instanceOf(ChronoFieldsTariff.class));
			ChronoFieldsTariff cft = (ChronoFieldsTariff) tariff;
			assertThat("No month range for HOD query",
					cft.rangeForChronoField(ChronoField.MONTH_OF_YEAR), is(nullValue()));
			assertThat("No day range for HOD query", cft.rangeForChronoField(ChronoField.DAY_OF_MONTH),
					is(nullValue()));
			assertThat("No weekday range for HOD query",
					cft.rangeForChronoField(ChronoField.DAY_OF_WEEK), is(nullValue()));
			assertThat("Minute range is hour for HOD query",
					cft.rangeForChronoField(ChronoField.MINUTE_OF_DAY),
					is(new IntRangeSet(rangeOf(hour.getHour() * 60, hour.getHour() * 60 + 60))));
			hour = hour.plusHours(1);
		}
	}

	@Test
	public void seasonal_hod() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		final Set<String> datumProps = new HashSet<>(Arrays.asList("wattHours"));

		service.setNodeId(nodeId);
		service.setAggregation(Aggregation.SeasonalHourOfDay);
		service.setSourceId(sourceId);
		service.setStartDate(startDate);
		service.setEndDate(endDate);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		final URI queryUri = URI.create(queryUrl.toString());

		final MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, queryUri);
		final MockClientHttpResponse res = new MockClientHttpResponse(
				getClass().getResourceAsStream("test-datum-stream-seasonal-hod-01.json"), HttpStatus.OK);
		req.setResponse(res);

		// create request
		expect(httpRequestFactory.createRequest(queryUri, HttpMethod.GET)).andReturn(req);

		// invoke customizer
		expect(httpRequestCustomizer.customize(same(req), isNull(), anyObject())).andReturn(req);

		// WHEN
		replayAll();
		TariffSchedule result = service.tariffSchedule();

		// THEN
		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));
		Collection<? extends Tariff> rules = result.rules();
		assertThat("Schedule has one rule per datum result (hour of day, per 4 seasons)", rules,
				hasSize(24 * 4));

		LocalDateTime hour = LocalDateTime.of(2000, 12, 1, 0, 0); // seasonal results start in Dec
		for ( Tariff tariff : rules ) {
			assertThat("Tariff is a ChronoFieldsTariff", tariff, instanceOf(ChronoFieldsTariff.class));
			ChronoFieldsTariff cft = (ChronoFieldsTariff) tariff;

			IntRangeSet monthRange = new IntRangeSet(3);
			LocalDate date = hour.toLocalDate();
			for ( int i = 0; i < 3; i++ ) {
				monthRange.add(date.getMonthValue());
				date = date.plusMonths(1);
			}

			assertThat("Month range for seasonal HOD query is 3-month season",
					cft.rangeForChronoField(ChronoField.MONTH_OF_YEAR), is(monthRange));
			assertThat("No day range for seasonal HOD query",
					cft.rangeForChronoField(ChronoField.DAY_OF_MONTH), is(nullValue()));
			assertThat("No weekday range for seasonal HOD query",
					cft.rangeForChronoField(ChronoField.DAY_OF_WEEK), is(nullValue()));
			assertThat("Minute range is hour for seasonal HOD query",
					cft.rangeForChronoField(ChronoField.MINUTE_OF_DAY),
					is(new IntRangeSet(rangeOf(hour.getHour() * 60, hour.getHour() * 60 + 60))));
			hour = hour.plusHours(1);
			if ( hour.getHour() == 0 ) {
				// skip to next season
				hour = hour.plusMonths(3);
			}
		}
	}

	@Test
	public void seasonal_dow() throws IOException {
		// GIVEN

		// configure stream settings
		final Long nodeId = 123L;
		final String sourceId = "test";
		final LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
		final LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		final Set<String> datumProps = new HashSet<>(Arrays.asList("wattHours"));

		service.setNodeId(nodeId);
		service.setAggregation(Aggregation.SeasonalDayOfWeek);
		service.setSourceId(sourceId);
		service.setStartDate(startDate);
		service.setEndDate(endDate);
		service.setDatumStreamPropertyNames(datumProps);

		// discover SolarQuery URL
		final String solarQueryBaseUrl = "http://localhost/solarquery";
		NodeAppConfiguration appConfig = new NodeAppConfiguration(
				Collections.singletonMap("solarquery", solarQueryBaseUrl));
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		final StringBuilder queryUrl = new StringBuilder(solarQueryBaseUrl);
		queryUrl.append(SolarQueryTariffScheduleProvider.STREAM_DATUM_PATH);
		queryUrl.append("?nodeId=").append(nodeId);
		queryUrl.append("&sourceId=").append(sourceId);
		queryUrl.append("&aggregation=").append(service.getAggregation().name());
		UrlUtils.appendURLEncodedValue(queryUrl, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(queryUrl, "localEndDate",
				DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		final URI queryUri = URI.create(queryUrl.toString());

		final MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, queryUri);
		final MockClientHttpResponse res = new MockClientHttpResponse(
				getClass().getResourceAsStream("test-datum-stream-seasonal-dow-01.json"), HttpStatus.OK);
		req.setResponse(res);

		// create request
		expect(httpRequestFactory.createRequest(queryUri, HttpMethod.GET)).andReturn(req);

		// invoke customizer
		expect(httpRequestCustomizer.customize(same(req), isNull(), anyObject())).andReturn(req);

		// WHEN
		replayAll();
		TariffSchedule result = service.tariffSchedule();

		// THEN
		assertThat("TariffSchedule resolved from SolarQuery data", result, is(notNullValue()));
		Collection<? extends Tariff> rules = result.rules();
		assertThat("Schedule has one rule per datum result (weekday, per 4 seasons)", rules,
				hasSize(7 * 4));

		LocalDate day = LocalDate.of(2000, 12, 4); // seasonal results start on first Mon of Dec
		for ( Tariff tariff : rules ) {
			assertThat("Tariff is a ChronoFieldsTariff", tariff, instanceOf(ChronoFieldsTariff.class));
			ChronoFieldsTariff cft = (ChronoFieldsTariff) tariff;

			IntRangeSet monthRange = new IntRangeSet(3);
			LocalDate date = day;
			for ( int i = 0; i < 3; i++ ) {
				monthRange.add(date.getMonthValue());
				date = date.plusMonths(1);
			}

			assertThat("Month range for seasonal DOW query is 3-month season",
					cft.rangeForChronoField(ChronoField.MONTH_OF_YEAR), is(monthRange));
			assertThat("No day range for seasonal DOW query",
					cft.rangeForChronoField(ChronoField.DAY_OF_MONTH), is(nullValue()));
			assertThat("Weekday range for seasonal DOW query is singleton day",
					cft.rangeForChronoField(ChronoField.DAY_OF_WEEK),
					is(new IntRangeSet(rangeOf(day.getDayOfWeek().getValue()))));
			assertThat("No minute range for seasonal DOW query",
					cft.rangeForChronoField(ChronoField.MINUTE_OF_DAY), is(nullValue()));

			day = day.plusDays(1);
			if ( day.getDayOfWeek() == DayOfWeek.MONDAY ) {
				// skip to next season
				day = day.plusMonths(3).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
			}
		}
	}

}
