/* ==================================================================
 * MetserviceWeatherDatumDataSourceTest.java - Oct 18, 2011 4:57:23 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.weather.nz.metservice.BasicMetserviceClient;
import net.solarnetwork.node.weather.nz.metservice.MetserviceWeatherDatumDataSource;

/**
 * Test case for the {@link MetserviceWeatherDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MetserviceWeatherDatumDataSourceTest {

	private BasicMetserviceClient createClientInstance() throws Exception {
		URL url = getClass().getResource("localObs_wellington-city.json");
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

	private MetserviceWeatherDatumDataSource createDataSourceInstance() throws Exception {
		MetserviceWeatherDatumDataSource ds = new MetserviceWeatherDatumDataSource();
		ds.setClient(createClientInstance());
		return ds;
	}

	@Test
	public void readCurrentDatum() throws Exception {
		final MetserviceWeatherDatumDataSource ds = createDataSourceInstance();
		final BasicMetserviceClient client = (BasicMetserviceClient) ds.getClient();
		final DateTimeFormatter tsFormat = client.timestampFormatter();

		AtmosphericDatum datum = (AtmosphericDatum) ds.readCurrentDatum();
		assertNotNull(datum);

		assertNotNull(datum.getTimestamp());
		assertEquals("2:00pm monday 1 sep 2014", tsFormat.format(datum.getTimestamp()).toLowerCase());

		assertNotNull(datum.getTemperature());
		assertEquals(14.0, datum.getTemperature().doubleValue(), 0.001);

		assertNotNull(datum.getHumidity());
		assertEquals(60.0, datum.getHumidity().doubleValue(), 0.001);

		assertNotNull(datum.getAtmosphericPressure());
		assertEquals(101700, datum.getAtmosphericPressure().intValue());

		assertEquals("Fine", datum.getSkyConditions());
	}

	@Test
	public void readMultipleDatum() throws Exception {
		final MetserviceWeatherDatumDataSource ds = createDataSourceInstance();
		final BasicMetserviceClient client = (BasicMetserviceClient) ds.getClient();
		final DateTimeFormatter tsFormat = client.timestampFormatter();

		Collection<NodeDatum> result = ds.readMultipleDatum();
		assertNotNull(result);
		assertEquals(25, result.size());

		Iterator<NodeDatum> itr = result.iterator();

		AtmosphericDatum datum = (AtmosphericDatum) itr.next();
		assertNotNull(datum.getTimestamp());
		assertEquals("2:00pm monday 1 sep 2014", tsFormat.format(datum.getTimestamp()).toLowerCase());

		assertNotNull(datum.getTemperature());
		assertEquals(14.0, datum.getTemperature().doubleValue(), 0.001);

		assertNotNull(datum.getHumidity());
		assertEquals(60.0, datum.getHumidity().doubleValue(), 0.001);

		assertNotNull(datum.getAtmosphericPressure());
		assertEquals(101700, datum.getAtmosphericPressure().intValue());

		assertEquals("Fine", datum.getSkyConditions());

		datum = (AtmosphericDatum) itr.next();
		assertNotNull(datum.getTimestamp());
		assertEquals("10:00am sunday 29 may 2016", tsFormat.format(datum.getTimestamp()).toLowerCase());
		assertEquals(new BigDecimal("11.0"), datum.getTemperature());
	}

}
