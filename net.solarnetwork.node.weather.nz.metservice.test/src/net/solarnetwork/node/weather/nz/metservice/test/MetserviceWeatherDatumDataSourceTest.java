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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.weather.nz.metservice.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.node.weather.WeatherDatum;
import net.solarnetwork.node.weather.nz.metservice.MetserviceWeatherDatumDataSource;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

/**
 * Test case for the {@link MetserviceWeatherDatumDataSource} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class MetserviceWeatherDatumDataSourceTest extends AbstractNodeTransactionalTest {

	private MetserviceWeatherDatumDataSource createDataSourceInstance() throws Exception {

		URL url = getClass().getResource("localObs.txt");
		File f = ResourceUtils.getFile(url);
		String baseDirectory = f.getParent();

		MetserviceWeatherDatumDataSource ds = new MetserviceWeatherDatumDataSource();
		ds.setBaseUrl("file://" + baseDirectory);
		ds.setLocalObs("localObs.txt");
		ds.setOneMinObs("oneMinObs.txt");
		ds.setUv("uv.txt");
		ds.setLocalForecast("localForecast.txt");
		return ds;
	}

	@Test
	public void parseRiseSet() throws Exception {
		MetserviceWeatherDatumDataSource ds = createDataSourceInstance();
		WeatherDatum datum = ds.readCurrentDatum();
		assertNotNull(datum);

		final SimpleDateFormat tsFormat = new SimpleDateFormat(ds.getTimestampDateFormat());

		assertNotNull(datum.getInfoDate());
		assertEquals("5:10pm tuesday 18 oct 2011", tsFormat.format(datum.getInfoDate()).toLowerCase());

		assertNotNull(datum.getTemperatureCelsius());
		assertEquals(13.1, datum.getTemperatureCelsius().doubleValue(), 0.001);

		assertNotNull(datum.getHumidity());
		assertEquals(70.0, datum.getHumidity().doubleValue(), 0.001);

		assertNotNull(datum.getBarometricPressure());
		assertEquals(999.0, datum.getBarometricPressure().doubleValue(), 0.001);

		assertEquals("Few showers first", datum.getSkyConditions());
	}

}
