package net.solarnetwork.node.weather.ibm.wc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.weather.ibm.wc.BasicWCClient;
import net.solarnetwork.node.weather.ibm.wc.WCClient;
import net.solarnetwork.node.weather.ibm.wc.WCDayDatumDataSource;
import net.solarnetwork.node.weather.ibm.wc.WCHourlyDatum;
import net.solarnetwork.node.weather.ibm.wc.WCHourlyDatumDataSource;

/**
 * Tests the parsing for daily forecasts using downloaded JSON
 * 
 * @author matt frost
 */
public class WCDayDatumDataSourceTest extends AbstractHttpClientTests{

	private static final String TEST_SOURCE_ID = "Test";

	private WCDayDatumDataSource dailyService;

	private static final String RESOURCE_NAME = "7day_daily.json";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private WCClient createClient() {
		URL url = getClass().getResource(RESOURCE_NAME);
		String urlString = url.toString();
		BasicWCClient client = new BasicWCClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.setDailyForecastUrl("%s_daily.json");

		return client;
	}

	@Before
	public void additionalSetup() {
		dailyService = new WCDayDatumDataSource();
		dailyService.setClient(createClient());
		dailyService.setDatumPeriod("7day");
		dailyService.setApiKey("a");
		dailyService.setObjectMapper(new ObjectMapper());
	}

	@Test
	public void readDatum() {
		
		TestHttpHandler handler = new TestHttpHandler() {
			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				respondWithResource(response, "7day_daily.json");
				response.flushBuffer();
				return true;
			}
		};
		
		getHttpServer().addHandler(handler);
		Collection<GeneralDayDatum> datum = dailyService.readMultipleDatum();
		log.debug(datum.toString());
		GeneralDayDatum first = (GeneralDayDatum) datum.toArray()[1];
		SimpleDateFormat day = new SimpleDateFormat("d MMMM yyyy");
		SimpleDateFormat hour = new SimpleDateFormat("HH:mm EE d MMMM yyyy");
		SimpleDateFormat time = new SimpleDateFormat("HH:mm");
		assertNotNull(first);

		assertNotNull(first.getCreated());

		log.debug(first.getCreated().toString());

		assertEquals(day.format(first.getCreated()), "7 July 2018");

		assertEquals(first.getTemperatureMaximum().intValue(), 92);

		assertEquals(first.getTemperatureMinimum().intValue(), 64);

		assertEquals(time.format(first.getSunrise().toDateTimeToday().toDate()), "23:37");

		assertEquals(time.format(first.getSunset().toDateTimeToday().toDate()), "14:29");

		assertEquals(time.format(first.getMoonrise().toDateTimeToday().toDate()), "18:57");

		assertEquals(time.format(first.getMoonset().toDateTimeToday().toDate()), "07:33");

	}

}
