package net.solarnetwork.node.weather.ibm.wc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.math.BigDecimal;
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

public class WCClientTests extends AbstractHttpClientTests{


	private WCDayDatumDataSource dailyService;
	private WCHourlyDatumDataSource hourlyService;
	private static final String RESOURCE_NAME = "7day_daily.json";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private WCClient createClient() {

		BasicWCClient client = new BasicWCClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.setHourlyForecastUrl("%s_hourly.json");
		client.setDailyForecastUrl("%s_daily.json");

		return client;
	}
	

	@Before
	public void additionalSetup() {
		ObjectMapper o = new ObjectMapper();
		WCClient c = createClient();

		dailyService = new WCDayDatumDataSource();
		dailyService.setClient(c);
		dailyService.setDatumPeriod("7day");
		dailyService.setApiKey("a");
		dailyService.setObjectMapper(o);
		
		hourlyService = new WCHourlyDatumDataSource();
		hourlyService.setClient(c);
		hourlyService.setDatumPeriod("2day");
		hourlyService.setObjectMapper(o);
	}

	@Test
	public void getHourlyDatum() {

		TestHttpHandler handler = new TestHttpHandler() {
			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				respondWithResource(response, "2day_hourly.json");
				response.flushBuffer();
				return true;
			}
		};
		getHttpServer().addHandler(handler);

		Collection<WCHourlyDatum> datum = hourlyService.readMultipleDatum();
		log.debug(datum.toString());
		WCHourlyDatum first = (WCHourlyDatum) datum.toArray()[0];
		SimpleDateFormat day = new SimpleDateFormat("d MMMM yyyy");
		SimpleDateFormat hour = new SimpleDateFormat("HH:mm EE d MMMM yyyy");
		assertNotNull(first);

		assertNotNull(first.getCreated());

		// log.debug(hour.format(first.getCreated()), "12:00 Tue 10 July 2018");

		assertEquals(hour.format(first.getCreated()), "12:00 Tue 10 July 2018");

		assertEquals(first.getTemperature(), new BigDecimal(86));

		assertEquals(first.getVisibility().intValue(), 10);

		assertEquals(first.getWindDirection().intValue(), 135);

		assertEquals(first.getWindSpeed().intValue(), 12);

		assertEquals(first.getHumidity().intValue(), 16);

		assertEquals(first.getCloudCover().intValue(), 28);
	}

	@Test
	public void getDailyDatum() {
		
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

