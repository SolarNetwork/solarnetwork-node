/* ==================================================================
 * WeatherUndergroundWeatherDatumDataSourceTests.java - 10/04/2017 1:06:15 PM
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
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.weather.wu.BasicWeatherUndergoundClient;
import net.solarnetwork.node.weather.wu.WeatherUndergroundClient;
import net.solarnetwork.node.weather.wu.WeatherUndergroundWeatherDatumDataSource;

/**
 * Test cases for the {@link WeatherUndergroundWeatherDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class WeatherUndergroundWeatherDatumDataSourceTests extends AbstractNodeTest {

	private static final long MS_PER_DAY = 24 * 60 * 60 * 1000L;

	private static final String TEST_WU_LOCATION = "/test.location";
	private static final String TEST_SOURCE_ID = "test.source";

	private WeatherUndergroundClient client;

	private WeatherUndergroundWeatherDatumDataSource dataSource;

	@Before
	public void setup() {
		client = EasyMock.createMock(WeatherUndergroundClient.class);

		dataSource = new WeatherUndergroundWeatherDatumDataSource();
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
		WeatherUndergroundWeatherDatumDataSource ds = new WeatherUndergroundWeatherDatumDataSource();
		assertTrue("Client available", ds.getClient() instanceof BasicWeatherUndergoundClient);
	}

	@Test
	public void readCurrentDatum() {
		final GeneralAtmosphericDatum datum = new GeneralAtmosphericDatum();
		expect(client.getCurrentConditions(TEST_WU_LOCATION)).andReturn(datum);

		replayAll();

		AtmosphericDatum result = dataSource.readCurrentDatum();
		assertSame("AtmosphericDatum", datum, result);
	}

	@Test
	public void readMultipleDatum() {
		final GeneralAtmosphericDatum datum = new GeneralAtmosphericDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);

		final GeneralAtmosphericDatum datum2 = new GeneralAtmosphericDatum();
		datum2.setCreated(new Date(System.currentTimeMillis() + MS_PER_DAY));
		datum2.setSourceId(TEST_SOURCE_ID);
		final List<AtmosphericDatum> datumList = Arrays.asList((AtmosphericDatum) datum2);

		// get current conditions first
		expect(client.getCurrentConditions(TEST_WU_LOCATION)).andReturn(datum);

		// followed by hourly forecast
		expect(client.getHourlyForecast(TEST_WU_LOCATION)).andReturn(datumList);

		replayAll();

		Collection<AtmosphericDatum> result = dataSource.readMultipleDatum();
		assertEquals("Forecast list", Arrays.asList(datum, datum2), result);
	}

}
