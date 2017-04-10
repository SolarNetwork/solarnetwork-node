/* ==================================================================
 * WeatherUndergroundDayDatumDataSourceTests.java - 10/04/2017 11:48:40 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.wu.test;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.weather.wu.BasicWeatherUndergoundClient;
import net.solarnetwork.node.weather.wu.WeatherUndergroundClient;
import net.solarnetwork.node.weather.wu.WeatherUndergroundDayDatumDataSource;

/**
 * test cases for the {@link WeatherUndergroundDayDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class WeatherUndergroundDayDatumDataSourceTests extends AbstractNodeTest {

	private static final long MS_PER_DAY = 24 * 60 * 60 * 1000L;

	private static final String TEST_WU_LOCATION = "/test.location";
	private static final String TEST_SOURCE_ID = "test.source";

	private WeatherUndergroundClient client;

	private WeatherUndergroundDayDatumDataSource dataSource;

	@Before
	public void setup() {
		client = EasyMock.createMock(WeatherUndergroundClient.class);

		dataSource = new WeatherUndergroundDayDatumDataSource();
		dataSource.setClient(client);
		dataSource.setLocationIdentifier(TEST_WU_LOCATION);
	}

	@After
	public void finish() {
		EasyMock.verify(client);
	}

	private void replayAll() {
		EasyMock.replay(client);
	}

	@Test
	public void defaultValues() {
		replayAll();
		WeatherUndergroundDayDatumDataSource ds = new WeatherUndergroundDayDatumDataSource();
		assertTrue("Client available", ds.getClient() instanceof BasicWeatherUndergoundClient);
	}

	@Test
	public void readCurrentDatum() {
		final GeneralDayDatum datum = new GeneralDayDatum();
		expect(client.getCurrentDay(TEST_WU_LOCATION)).andReturn(datum);

		replayAll();

		DayDatum result = dataSource.readCurrentDatum();
		assertSame("DayDatum", datum, result);
	}

	@Test
	public void readCurrentDatumCached() {
		final GeneralDayDatum datum = new GeneralDayDatum();
		datum.setCreated(new Date());
		expect(client.getCurrentDay(TEST_WU_LOCATION)).andReturn(datum);

		replayAll();

		DayDatum result = dataSource.readCurrentDatum();
		assertSame("DayDatum", datum, result);

		// read a second time, which should not call client API
		result = dataSource.readCurrentDatum();
		assertSame("Cached DayDatum", datum, result);
	}

	@Test
	public void readCurrentDatumCachedInvalid() {
		final GeneralDayDatum datum = new GeneralDayDatum();
		// set the datum to yesterday, so cached value is for yesterday
		datum.setCreated(new Date(System.currentTimeMillis() - MS_PER_DAY));
		expect(client.getCurrentDay(TEST_WU_LOCATION)).andReturn(datum);

		final GeneralDayDatum datum2 = new GeneralDayDatum();
		// set the 2nd datum to today
		datum2.setCreated(new Date());
		expect(client.getCurrentDay(TEST_WU_LOCATION)).andReturn(datum2);

		replayAll();

		DayDatum result = dataSource.readCurrentDatum();
		assertSame("DayDatum", datum, result);

		// read a second time, which should call client API again to refresh stale data
		result = dataSource.readCurrentDatum();
		assertSame("Refreshed DayDatum", datum2, result);

		// read a third time, which should not call client API
		result = dataSource.readCurrentDatum();
		assertSame("Cached DayDatum", datum2, result);
	}

	@Test
	public void readMultipleDatum() {
		final GeneralDayDatum datum = new GeneralDayDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);

		final GeneralDayDatum datum2 = new GeneralDayDatum();
		datum2.setCreated(new Date(System.currentTimeMillis() + MS_PER_DAY));
		datum2.setSourceId(TEST_SOURCE_ID);
		final List<DayDatum> datumList = Arrays.asList((DayDatum) datum2);

		// get today's data first
		expect(client.getCurrentDay(TEST_WU_LOCATION)).andReturn(datum);

		// followed by forecast
		expect(client.getTenDayForecast(TEST_WU_LOCATION)).andReturn(datumList);

		replayAll();

		Collection<DayDatum> result = dataSource.readMultipleDatum();
		assertEquals("Forecast list", Arrays.asList(datum, datum2), result);
	}

}
