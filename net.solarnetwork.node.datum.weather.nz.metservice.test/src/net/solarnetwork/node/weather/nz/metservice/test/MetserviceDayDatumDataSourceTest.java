/* ==================================================================
 * MetserviceDayDatumDataSourceTest.java - Oct 18, 2011 4:04:48 PM
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
import java.net.URL;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.weather.nz.metservice.BasicMetserviceClient;
import net.solarnetwork.node.weather.nz.metservice.MetserviceDayDatumDataSource;

/**
 * Test case for the {@link MetserviceDayDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MetserviceDayDatumDataSourceTest {

	private static final String RISE_SET_RESOURCE_NAME = "riseSet_wellington-city.json";

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
		client.setObjectMapper(new ObjectMapper());
		return client;
	}

	private MetserviceDayDatumDataSource createDataSourceInstance() throws Exception {
		MetserviceDayDatumDataSource ds = new MetserviceDayDatumDataSource();
		ds.setClient(createClientInstance());
		return ds;
	}

	@Test
	public void readCurrentDatum() throws Exception {
		final MetserviceDayDatumDataSource ds = createDataSourceInstance();
		final BasicMetserviceClient client = (BasicMetserviceClient) ds.getClient();
		final DateTimeFormatter dayFormat = client.dayFormatter();
		final DateTimeFormatter timeFormat = client.timeFormatter();

		DayDatum datum = (DayDatum) ds.readCurrentDatum();
		assertNotNull(datum);

		assertNotNull(datum.getTimestamp());
		assertEquals("1 September 2014", dayFormat.format(datum.getTimestamp()));

		assertNotNull(datum.getSunriseTime());
		assertEquals("6:47am", timeFormat.format(datum.getSunriseTime()).toLowerCase());

		assertNotNull(datum.getSunsetTime());
		assertEquals("5:56pm", timeFormat.format(datum.getSunsetTime()).toLowerCase());
	}

}
