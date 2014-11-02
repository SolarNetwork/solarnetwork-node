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
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.node.weather.nz.metservice.MetserviceWeatherDatumDataSource;
import org.junit.Test;
import org.springframework.util.ResourceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test case for the {@link MetserviceWeatherDatumDataSource} class.
 * 
 * @author matt
 * @version 1.2
 */
public class MetserviceWeatherDatumDataSourceTest extends AbstractNodeTransactionalTest {

	private MetserviceWeatherDatumDataSource createDataSourceInstance() throws Exception {

		URL url = getClass().getResource("localObs_wellington-city.json");
		File f = ResourceUtils.getFile(url);
		String baseDirectory = f.getParent();

		MetserviceWeatherDatumDataSource ds = new MetserviceWeatherDatumDataSource();
		ds.setBaseUrl("file://" + baseDirectory);
		ds.setLocalObs(f.getName());
		ds.setLocalForecast("localForecastwellington-city.json");
		ds.setObjectMapper(new ObjectMapper());
		return ds;
	}

	@Test
	public void parseRiseSet() throws Exception {
		MetserviceWeatherDatumDataSource ds = createDataSourceInstance();
		GeneralAtmosphericDatum datum = (GeneralAtmosphericDatum) ds.readCurrentDatum();
		assertNotNull(datum);

		final SimpleDateFormat tsFormat = new SimpleDateFormat(ds.getTimestampDateFormat());

		assertNotNull(datum.getCreated());
		assertEquals("2:00pm monday 1 sep 2014", tsFormat.format(datum.getCreated()).toLowerCase());

		assertNotNull(datum.getTemperature());
		assertEquals(14.0, datum.getTemperature().doubleValue(), 0.001);

		assertNotNull(datum.getHumidity());
		assertEquals(60.0, datum.getHumidity().doubleValue(), 0.001);

		assertNotNull(datum.getAtmosphericPressure());
		assertEquals(101700, datum.getAtmosphericPressure().intValue());

		assertEquals("Fine", datum.getSkyConditions());
	}

}
