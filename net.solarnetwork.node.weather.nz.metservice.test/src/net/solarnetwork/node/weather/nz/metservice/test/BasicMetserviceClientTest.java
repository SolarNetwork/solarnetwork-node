/* ==================================================================
 * BasicMetserviceClientTest.java - 29/05/2016 7:35:28 am
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.nz.metservice.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimeZone;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.weather.nz.metservice.BasicMetserviceClient;
import org.junit.Test;
import org.springframework.util.ResourceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test cases for the {@link BasicMetserviceClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicMetserviceClientTest extends AbstractNodeTest {

	private static final String RISE_SET_RESOURCE_NAME = "riseSet_wellington-city.json";
	private static final String LOCATION_KEY = "wellington-city";

	private BasicMetserviceClient createClientInstance() throws Exception {
		URL url = getClass().getResource(RISE_SET_RESOURCE_NAME);
		File f = ResourceUtils.getFile(url);
		String baseDirectory = f.getParent();

		BasicMetserviceClient client = new BasicMetserviceClient();
		client.setBaseUrl("file://" + baseDirectory);
		client.setRiseSetTemplate("riseSet_%s.json");
		client.setLocalObsTemplate("localObs_%s.json");
		client.setLocalForecastTemplate("localForecast%s.json");
		client.setOneMinuteObsTemplate("oneMinuteObs_%s.json");
		client.setHourlyObsAndForecastTemplate("hourlyObsAndForecast_%s.json");
		client.setObjectMapper(new ObjectMapper());
		return client;
	}

	@Test
	public void parseRiseSet() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		GeneralDayDatum datum = client.readCurrentRiseSet(LOCATION_KEY);
		assertNotNull(datum);

		final SimpleDateFormat dayFormat = new SimpleDateFormat(client.getDayDateFormat());

		assertNotNull(datum.getCreated());
		assertEquals("1 September 2014", dayFormat.format(datum.getCreated()));

		final SimpleDateFormat timeFormat = new SimpleDateFormat(client.getTimeDateFormat());

		assertNotNull(datum.getSunrise());
		assertEquals("6:47am", timeFormat.format(datum.getSunrise().toDateTimeToday().toDate())
				.toLowerCase());

		assertNotNull(datum.getSunset());
		assertEquals("5:56pm", timeFormat.format(datum.getSunset().toDateTimeToday().toDate())
				.toLowerCase());

		assertNotNull(datum.getMoonrise());
		assertEquals("9:58am", timeFormat.format(datum.getMoonrise().toDateTimeToday().toDate())
				.toLowerCase());
	}

	@Test
	public void parseLocalObservations() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		Collection<GeneralLocationDatum> results = client.readCurrentLocalObservations(LOCATION_KEY);

		final SimpleDateFormat tsFormat = new SimpleDateFormat(client.getTimestampDateFormat());
		tsFormat.setTimeZone(TimeZone.getTimeZone(client.getTimeZoneId()));

		assertNotNull(results);
		assertEquals(2, results.size());
		Iterator<GeneralLocationDatum> itr = results.iterator();

		GeneralLocationDatum loc = itr.next();
		assertTrue(loc instanceof GeneralAtmosphericDatum);
		GeneralAtmosphericDatum weather = (GeneralAtmosphericDatum) loc;

		assertNotNull(weather.getCreated());
		assertEquals("2:00pm monday 1 sep 2014", tsFormat.format(weather.getCreated()).toLowerCase());

		assertNotNull(weather.getTemperature());
		assertEquals(14.0, weather.getTemperature().doubleValue(), 0.001);

		assertNotNull(weather.getHumidity());
		assertEquals(60.0, weather.getHumidity().doubleValue(), 0.001);

		assertNotNull(weather.getAtmosphericPressure());
		assertEquals(101700, weather.getAtmosphericPressure().intValue());

		loc = itr.next();
		assertTrue(loc instanceof GeneralDayDatum);
		GeneralDayDatum day = (GeneralDayDatum) loc;

		assertNotNull(day.getCreated());
		assertEquals("9:00am monday 1 sep 2014", tsFormat.format(day.getCreated()).toLowerCase());

		assertNotNull(day.getTemperatureMinimum());
		assertEquals(5.0, day.getTemperatureMinimum().doubleValue(), 0.001);

		assertNotNull(day.getTemperatureMaximum());
		assertEquals(15.0, day.getTemperatureMaximum().doubleValue(), 0.001);
	}

	@Test
	public void parseLocalObservationsMidnight() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		Collection<GeneralLocationDatum> results = client
				.readCurrentLocalObservations("wellington-city-1");

		final SimpleDateFormat tsFormat = new SimpleDateFormat(client.getTimestampDateFormat());
		tsFormat.setTimeZone(TimeZone.getTimeZone(client.getTimeZoneId()));

		assertNotNull(results);
		assertEquals(2, results.size());
		Iterator<GeneralLocationDatum> itr = results.iterator();

		GeneralLocationDatum loc = itr.next();
		assertTrue(loc instanceof GeneralAtmosphericDatum);
		GeneralAtmosphericDatum weather = (GeneralAtmosphericDatum) loc;

		assertNotNull(weather.getCreated());
		assertEquals("8:00am sunday 29 may 2016", tsFormat.format(weather.getCreated()).toLowerCase());

		assertNotNull(weather.getTemperature());
		assertEquals(11.0, weather.getTemperature().doubleValue(), 0.001);

		assertNotNull(weather.getHumidity());
		assertEquals(90.0, weather.getHumidity().doubleValue(), 0.001);

		assertNotNull(weather.getAtmosphericPressure());
		assertEquals(99300, weather.getAtmosphericPressure().intValue());

		loc = itr.next();
		assertTrue(loc instanceof GeneralDayDatum);
		GeneralDayDatum day = (GeneralDayDatum) loc;

		assertNotNull(day.getCreated());
		assertEquals("12:00am saturday 28 may 2016", tsFormat.format(day.getCreated()).toLowerCase());

		assertNotNull(day.getTemperatureMinimum());
		assertEquals(11.0, day.getTemperatureMinimum().doubleValue(), 0.001);

		assertNotNull(day.getTemperatureMaximum());
		assertEquals(15.0, day.getTemperatureMaximum().doubleValue(), 0.001);
	}

	@Test
	public void parseLocalObservationsNoon() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		Collection<GeneralLocationDatum> results = client
				.readCurrentLocalObservations("wellington-city-2");

		final SimpleDateFormat tsFormat = new SimpleDateFormat(client.getTimestampDateFormat());
		tsFormat.setTimeZone(TimeZone.getTimeZone(client.getTimeZoneId()));

		assertNotNull(results);
		assertEquals(2, results.size());
		Iterator<GeneralLocationDatum> itr = results.iterator();

		GeneralLocationDatum loc = itr.next();
		assertTrue(loc instanceof GeneralAtmosphericDatum);
		GeneralAtmosphericDatum weather = (GeneralAtmosphericDatum) loc;

		assertNotNull(weather.getCreated());
		assertEquals("8:00am sunday 29 may 2016", tsFormat.format(weather.getCreated()).toLowerCase());

		assertNotNull(weather.getTemperature());
		assertEquals(11.0, weather.getTemperature().doubleValue(), 0.001);

		assertNotNull(weather.getHumidity());
		assertEquals(90.0, weather.getHumidity().doubleValue(), 0.001);

		assertNotNull(weather.getAtmosphericPressure());
		assertEquals(99300, weather.getAtmosphericPressure().intValue());

		loc = itr.next();
		assertTrue(loc instanceof GeneralDayDatum);
		GeneralDayDatum day = (GeneralDayDatum) loc;

		assertNotNull(day.getCreated());
		assertEquals("12:00pm saturday 28 may 2016", tsFormat.format(day.getCreated()).toLowerCase());

		assertNotNull(day.getTemperatureMinimum());
		assertEquals(11.0, day.getTemperatureMinimum().doubleValue(), 0.001);

		assertNotNull(day.getTemperatureMaximum());
		assertEquals(15.0, day.getTemperatureMaximum().doubleValue(), 0.001);
	}

	@Test
	public void parseLocalForecast() throws Exception {
		final BasicMetserviceClient client = createClientInstance();
		final SimpleDateFormat dayFormat = new SimpleDateFormat(client.getDayDateFormat());
		dayFormat.setTimeZone(TimeZone.getTimeZone(client.getTimeZoneId()));
		final SimpleDateFormat timeFormat = new SimpleDateFormat(client.getTimeDateFormat());

		Collection<GeneralDayDatum> results = client.readLocalForecast(LOCATION_KEY);
		assertNotNull(results);
		assertEquals(10, results.size());

		Iterator<GeneralDayDatum> itr = results.iterator();
		GeneralDayDatum day = itr.next();

		assertNotNull(day.getCreated());
		assertEquals("1 september 2014", dayFormat.format(day.getCreated()).toLowerCase());
		assertNotNull(day.getTemperatureMinimum());
		assertEquals(7.0, day.getTemperatureMinimum().doubleValue(), 0.001);
		assertNotNull(day.getTemperatureMaximum());
		assertEquals(15.0, day.getTemperatureMaximum().doubleValue(), 0.001);
		assertNotNull(day.getSunrise());
		assertEquals("6:47am", timeFormat.format(day.getSunrise().toDateTimeToday().toDate())
				.toLowerCase());
		assertNotNull(day.getSunset());
		assertEquals("5:56pm", timeFormat.format(day.getSunset().toDateTimeToday().toDate())
				.toLowerCase());
		assertNotNull(day.getMoonrise());
		assertEquals("9:58am", timeFormat.format(day.getMoonrise().toDateTimeToday().toDate())
				.toLowerCase());

		day = itr.next();
		assertEquals("2 september 2014", dayFormat.format(day.getCreated()).toLowerCase());
		assertNotNull(day.getTemperatureMinimum());
		assertEquals(6.0, day.getTemperatureMinimum().doubleValue(), 0.001);
		assertNotNull(day.getTemperatureMaximum());
		assertEquals(13.0, day.getTemperatureMaximum().doubleValue(), 0.001);
		assertNotNull(day.getSunrise());
		assertEquals("6:45am", timeFormat.format(day.getSunrise().toDateTimeToday().toDate())
				.toLowerCase());
		assertNotNull(day.getSunset());
		assertEquals("5:57pm", timeFormat.format(day.getSunset().toDateTimeToday().toDate())
				.toLowerCase());
		assertNotNull(day.getMoonrise());
		assertEquals("10:41am", timeFormat.format(day.getMoonrise().toDateTimeToday().toDate())
				.toLowerCase());
		assertNotNull(day.getMoonset());
		assertEquals("12:21am", timeFormat.format(day.getMoonset().toDateTimeToday().toDate())
				.toLowerCase());
	}

	@Test
	public void parseHourlyObsAndForecast() throws Exception {
		final BasicMetserviceClient client = createClientInstance();
		final SimpleDateFormat timestampHourFormat = new SimpleDateFormat(
				client.getTimestampHourDateFormat());
		timestampHourFormat.setTimeZone(TimeZone.getTimeZone(client.getTimeZoneId()));

		Collection<GeneralAtmosphericDatum> results = client.readHourlyForecast(LOCATION_KEY);
		assertNotNull(results);
		assertEquals(24, results.size());

		Iterator<GeneralAtmosphericDatum> itr = results.iterator();

		GeneralAtmosphericDatum hour = itr.next();
		assertNotNull(hour.getCreated());
		assertEquals("10:00 sun 29 may 2016", timestampHourFormat.format(hour.getCreated())
				.toLowerCase());
		assertEquals(new BigDecimal("11.0"), hour.getTemperature());
	}

}
