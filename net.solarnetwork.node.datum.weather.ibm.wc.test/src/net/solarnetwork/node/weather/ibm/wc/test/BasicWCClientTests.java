
package net.solarnetwork.node.weather.ibm.wc.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.weather.ibm.wc.BasicWCClient;
import net.solarnetwork.node.weather.ibm.wc.DailyDatumPeriod;
import net.solarnetwork.node.weather.ibm.wc.HourlyDatumPeriod;
import net.solarnetwork.node.weather.ibm.wc.MeasurementUnit;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link BasicWCClient} class.
 * 
 * @author matt frost
 * @version 2.0
 */
public class BasicWCClientTests extends AbstractHttpClientTests {

	private BasicWCClient c;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Before
	public void additionalSetup() {
		c = new BasicWCClient();
		c.setBaseUrl(getHttpServerBaseUrl());
		//c.setHourlyForecastUrl("%s_hourly.json");
		//c.setDailyForecastUrl("%s_daily.json");
		c.setObjectMapper(new ObjectMapper());
	}

	@Test
	public void propertyDefaults() {
		assertEquals(MeasurementUnit.Metric, c.getUnits());
	}

	@Test
	public void readHourlyForecast() {

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Path", "/v3/wx/forecast/hourly/1day", request.getRequestURI());
				assertEquals("icaoCode", "NZDA", request.getParameter("icaoCode"));
				assertEquals("language", "en-US", request.getParameter("language"));
				assertEquals("format", "json", request.getParameter("format"));
				assertEquals("units", "m", request.getParameter("units"));
				assertEquals("apiKey", "abc1234", request.getParameter("apiKey"));
				respondWithResource(response, "2day_hourly.json");
				response.flushBuffer();
				return true;
			}
		};
		getHttpServer().addHandler(handler);
		List<AtmosphericDatum> datum = c.readHourlyForecast("NZDA", "abc1234", HourlyDatumPeriod.ONEDAY)
				.stream().collect(Collectors.toList());
		log.debug("Parsed datum: [\n\t{}\n]",
				datum.stream().map(AtmosphericDatum::toString).collect(Collectors.joining("\n\t")));
		AtmosphericDatum first = datum.get(0);
		log.debug("Timestamp = {}", first.getTimestamp());

		assertThat("Timestamp",
				BasicWCClient.OFFSET_DATE_TIME
						.format(first.getTimestamp().atOffset(ZoneOffset.ofHours(-6))),
				is("2018-07-09T18:00:00-0600"));

		assertThat("Temp", first.getTemperature(), is(new BigDecimal(86)));

		assertThat("Vis", first.getVisibility(), is(10));

		assertThat("Wind dir", first.getWindDirection(), is(135));

		assertThat("Wind speed", first.getWindSpeed(), is(new BigDecimal("12")));

		assertThat("Humidity", first.getHumidity(), is(16));

		assertThat("Cloud cover", first.asSampleOperations().getSampleInteger(
				DatumSamplesType.Instantaneous, BasicWCClient.CLOUD_COVER_PROPERTY), is(28));
	}

	@Test
	public void readDailyForecast() {

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Path", "/v3/wx/forecast/daily/10day", request.getRequestURI());
				assertEquals("icaoCode", "NZDA", request.getParameter("icaoCode"));
				assertEquals("language", "en-US", request.getParameter("language"));
				assertEquals("format", "json", request.getParameter("format"));
				assertEquals("units", "m", request.getParameter("units"));
				assertEquals("apiKey", "abc1234", request.getParameter("apiKey"));
				respondWithResource(response, "7day_daily.json");
				response.flushBuffer();
				return true;
			}
		};

		getHttpServer().addHandler(handler);
		List<DayDatum> datum = c.readDailyForecast("NZDA", "abc1234", DailyDatumPeriod.TENDAY).stream()
				.collect(Collectors.toList());
		log.debug("Parsed datum: [\n\t{}\n]",
				datum.stream().map(DayDatum::toString).collect(Collectors.joining("\n\t")));
		DayDatum first = datum.get(1); // actually 2nd to skip null max temp data
		log.debug("Timestamp = {}", first.getTimestamp());

		assertThat("Timestamp",
				BasicWCClient.OFFSET_DATE_TIME
						.format(first.getTimestamp().atOffset(ZoneOffset.ofHours(-6))),
				is("2018-07-06T07:00:00-0600"));

		assertThat("Temp max", first.getTemperatureMaximum().intValue(), is(92));

		assertThat("Temp min", first.getTemperatureMinimum().intValue(), is(64));

		assertThat("Sunrise", DateUtils.format(first.getSunriseTime()), is("05:37"));

		assertThat("Sunset", DateUtils.format(first.getSunsetTime()), is("20:29"));

		assertThat("Moonrise", DateUtils.format(first.getMoonriseTime()), is("00:57"));

		assertThat("Moonset", DateUtils.format(first.getMoonsetTime()), is("13:33"));

	}

}
