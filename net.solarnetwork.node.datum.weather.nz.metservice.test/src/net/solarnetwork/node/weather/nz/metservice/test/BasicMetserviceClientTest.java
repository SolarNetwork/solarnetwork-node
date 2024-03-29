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
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.weather.nz.metservice.BasicMetserviceClient;

/**
 * Test cases for the {@link BasicMetserviceClient} class.
 * 
 * @author matt
 * @version 2.0
 */
public class BasicMetserviceClientTest {

	private static final String RISE_SET_RESOURCE_NAME = "riseSet_wellington-city.json";
	private static final String LOCATION_KEY = "wellington-city";

	private BasicMetserviceClient createClientInstance() throws Exception {
		URL url = getClass().getResource(RISE_SET_RESOURCE_NAME);
		String urlString = url.toString();
		String baseDirectory = urlString.substring(0, urlString.lastIndexOf('/'));
		if ( baseDirectory.startsWith("file:") ) {
			baseDirectory = "file://" + baseDirectory.substring(5);
		}

		BasicMetserviceClient client = new BasicMetserviceClient();
		client.setBaseUrl(baseDirectory);
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
		DayDatum datum = client.readCurrentRiseSet(LOCATION_KEY);
		assertNotNull(datum);

		final DateTimeFormatter dayFormat = client.dayFormatter();

		assertNotNull(datum.getTimestamp());
		assertEquals("1 September 2014", dayFormat.format(datum.getTimestamp()));

		final DateTimeFormatter timeFormat = client.timeFormatter();

		assertNotNull(datum.getSunriseTime());
		assertEquals("6:47am", timeFormat.format(datum.getSunriseTime()).toLowerCase());

		assertNotNull(datum.getSunsetTime());
		assertEquals("5:56pm", timeFormat.format(datum.getSunsetTime()).toLowerCase());

		assertNotNull(datum.getMoonriseTime());
		assertEquals("9:58am", timeFormat.format(datum.getMoonriseTime()).toLowerCase());
	}

	@Test
	public void parseLocalObservations() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		Collection<NodeDatum> results = client.readCurrentLocalObservations(LOCATION_KEY);

		final DateTimeFormatter tsFormat = client.timestampFormatter();

		assertNotNull(results);
		assertEquals(1, results.size());
		Iterator<NodeDatum> itr = results.iterator();

		NodeDatum loc = itr.next();
		assertTrue(loc instanceof AtmosphericDatum);
		AtmosphericDatum weather = (AtmosphericDatum) loc;

		assertNotNull(weather.getTimestamp());
		assertEquals("2:00pm sun, 17 oct", tsFormat.format(weather.getTimestamp()).toLowerCase());

		assertNotNull(weather.getTemperature());
		assertEquals(15.0, weather.getTemperature().doubleValue(), 0.001);

		assertNotNull(weather.getHumidity());
		assertEquals(73.0, weather.getHumidity().doubleValue(), 0.001);

		assertNotNull(weather.getAtmosphericPressure());
		assertEquals(101000, weather.getAtmosphericPressure().intValue());
	}

	@Test
	public void parseLocalObservationsMidnight() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		Collection<NodeDatum> results = client.readCurrentLocalObservations("wellington-city-1");

		final DateTimeFormatter tsFormat = client.timestampFormatter();

		assertNotNull(results);
		assertEquals(1, results.size());
		Iterator<NodeDatum> itr = results.iterator();

		NodeDatum loc = itr.next();
		assertTrue(loc instanceof AtmosphericDatum);
		AtmosphericDatum weather = (AtmosphericDatum) loc;

		assertNotNull(weather.getTimestamp());
		assertEquals("8:00am sat, 29 may", tsFormat.format(weather.getTimestamp()).toLowerCase());

		assertNotNull(weather.getTemperature());
		assertEquals(11.0, weather.getTemperature().doubleValue(), 0.001);

		assertNotNull(weather.getHumidity());
		assertEquals(90.0, weather.getHumidity().doubleValue(), 0.001);

		assertNotNull(weather.getAtmosphericPressure());
		assertEquals(99300, weather.getAtmosphericPressure().intValue());
	}

	@Test
	public void parseLocalObservationsNoon() throws Exception {
		BasicMetserviceClient client = createClientInstance();
		Collection<NodeDatum> results = client.readCurrentLocalObservations("wellington-city-2");

		final DateTimeFormatter tsFormat = client.timestampFormatter();

		assertNotNull(results);
		assertEquals(1, results.size());
		Iterator<NodeDatum> itr = results.iterator();

		NodeDatum loc = itr.next();
		assertTrue(loc instanceof AtmosphericDatum);
		AtmosphericDatum weather = (AtmosphericDatum) loc;

		assertNotNull(weather.getTimestamp());
		assertEquals("8:00am sat, 29 may", tsFormat.format(weather.getTimestamp()).toLowerCase());

		assertNotNull(weather.getTemperature());
		assertEquals(11.0, weather.getTemperature().doubleValue(), 0.001);

		assertNotNull(weather.getHumidity());
		assertEquals(90.0, weather.getHumidity().doubleValue(), 0.001);

		assertNotNull(weather.getAtmosphericPressure());
		assertEquals(99300, weather.getAtmosphericPressure().intValue());
	}

	@Test
	public void parseLocalForecast() throws Exception {
		final BasicMetserviceClient client = createClientInstance();
		final DateTimeFormatter dayFormat = client.dayFormatter();
		final DateTimeFormatter timeFormat = client.timeFormatter();

		Collection<DayDatum> results = client.readLocalForecast(LOCATION_KEY);
		assertNotNull(results);
		assertEquals(10, results.size());

		Iterator<DayDatum> itr = results.iterator();
		DayDatum day = itr.next();

		assertNotNull(day.getTimestamp());
		assertEquals("1 september 2014", dayFormat.format(day.getTimestamp()).toLowerCase());
		assertNotNull(day.getTemperatureMinimum());
		assertEquals(7.0, day.getTemperatureMinimum().doubleValue(), 0.001);
		assertNotNull(day.getTemperatureMaximum());
		assertEquals(15.0, day.getTemperatureMaximum().doubleValue(), 0.001);
		assertNotNull(day.getSunriseTime());
		assertEquals("6:47am", timeFormat.format(day.getSunriseTime()).toLowerCase());
		assertNotNull(day.getSunsetTime());
		assertEquals("5:56pm", timeFormat.format(day.getSunsetTime()).toLowerCase());
		assertNotNull(day.getMoonriseTime());
		assertEquals("9:58am", timeFormat.format(day.getMoonriseTime()).toLowerCase());

		day = itr.next();
		assertEquals("2 september 2014", dayFormat.format(day.getTimestamp()).toLowerCase());
		assertNotNull(day.getTemperatureMinimum());
		assertEquals(6.0, day.getTemperatureMinimum().doubleValue(), 0.001);
		assertNotNull(day.getTemperatureMaximum());
		assertEquals(13.0, day.getTemperatureMaximum().doubleValue(), 0.001);
		assertNotNull(day.getSunriseTime());
		assertEquals("6:45am", timeFormat.format(day.getSunriseTime()).toLowerCase());
		assertNotNull(day.getSunsetTime());
		assertEquals("5:57pm", timeFormat.format(day.getSunsetTime()).toLowerCase());
		assertNotNull(day.getMoonriseTime());
		assertEquals("10:41am", timeFormat.format(day.getMoonriseTime()).toLowerCase());
		assertNotNull(day.getMoonsetTime());
		assertEquals("12:21am", timeFormat.format(day.getMoonsetTime()).toLowerCase());
	}

	@Test
	public void parseHourlyObsAndForecast() throws Exception {
		final BasicMetserviceClient client = createClientInstance();
		final DateTimeFormatter timestampHourFormat = client.timestampHourFormatter();

		Collection<AtmosphericDatum> results = client.readHourlyForecast(LOCATION_KEY);
		assertNotNull(results);
		assertEquals(24, results.size());

		Iterator<AtmosphericDatum> itr = results.iterator();

		AtmosphericDatum hour = itr.next();
		assertNotNull(hour.getTimestamp());
		assertEquals("16:00 sun 17 oct 2021",
				timestampHourFormat.format(hour.getTimestamp()).toLowerCase());
		assertEquals(new BigDecimal("15.0"), hour.getTemperature());
	}

}
