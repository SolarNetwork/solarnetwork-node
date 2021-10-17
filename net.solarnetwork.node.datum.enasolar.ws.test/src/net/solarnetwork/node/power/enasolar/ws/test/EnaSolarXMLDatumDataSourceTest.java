/* ==================================================================
 * EnaSolarXMLDatumDataSourceTest.java - Oct 2, 2011 9:24:34 PM
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

package net.solarnetwork.node.power.enasolar.ws.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.power.enasolar.ws.EnaSolarPowerDatum;
import net.solarnetwork.node.power.enasolar.ws.EnaSolarXMLDatumDataSource;

/**
 * Test case for the {@link EnaSolarXMLDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class EnaSolarXMLDatumDataSourceTest {

	private static final Logger log = LoggerFactory.getLogger(EnaSolarXMLDatumDataSourceTest.class);

	private class TestDataSource extends EnaSolarXMLDatumDataSource {

		@Override
		public void setSample(EnaSolarPowerDatum datum) {
			super.setSample(datum);
		}

	}

	@Test
	public void parseDeviceInfoDatum() {
		EnaSolarXMLDatumDataSource dataSource = new EnaSolarXMLDatumDataSource();
		dataSource.setUrl(getClass().getResource("deviceinfo.xml").toString());
		dataSource.init();
		Map<String, String> deviceInfoMap = new LinkedHashMap<String, String>(10);
		deviceInfoMap.put("outputVoltage", "//data[@key='acOutputVolts']/@value");
		deviceInfoMap.put("outputPower", "//data[@key='acPower']/@value");
		deviceInfoMap.put("decaWattHoursTotal", "//data[@key='decaWattHoursTotal']/@value");
		deviceInfoMap.put("inputVoltage", "//data[@key='pvVolts']/@value");
		deviceInfoMap.put("inputPower", "//data[@key='pvPower']/@value");
		dataSource.setXpathMap(deviceInfoMap);

		AcDcEnergyDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Long.valueOf(57540), datum.getWattHourReading());
		assertEquals(Integer.valueOf(628), datum.getWatts());
		assertNotNull(datum.getVoltage());
		assertEquals(241.1, datum.getVoltage().floatValue(), 0.01);
		assertNotNull(datum.getDcPower());
		assertEquals(Integer.valueOf(681), datum.getDcPower());
		assertNotNull(datum.getDcVoltage());
		assertEquals(304.3F, datum.getDcVoltage().floatValue(), 0.01);
	}

	@Test
	public void parseDeviceInfoDatumAcrossDays() {
		TestDataSource dataSource = new TestDataSource();
		dataSource.setSampleCacheMs(0); // disable cache
		dataSource.setUrl(getClass().getResource("deviceinfo-daily-resetting.xml").toString());
		dataSource.init();
		Map<String, String> deviceInfoMap = new LinkedHashMap<String, String>(10);
		deviceInfoMap.put("outputVoltage", "//data[@key='acOutputVolts']/@value");
		deviceInfoMap.put("outputPower", "//data[@key='acPower']/@value");
		deviceInfoMap.put("kWattHoursToday", "//data[@key='kWattHoursToday']/@value");
		deviceInfoMap.put("inputVoltage", "//data[@key='pvVolts']/@value");
		deviceInfoMap.put("inputPower", "//data[@key='pvPower']/@value");
		dataSource.setXpathMap(deviceInfoMap);

		AcDcEnergyDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Integer.valueOf(628), datum.getWatts());
		assertEquals(Long.valueOf(17940), datum.getWattHourReading());

		// read in zero-watt reading, for threshold
		dataSource.setUrl(getClass().getResource("deviceinfo-dayend.xml").toString());
		for ( int i = 0; i < (int) EnaSolarXMLDatumDataSource.ZERO_WATT_THRESHOLD; i++ ) {
			datum = dataSource.readCurrentDatum();
			assertNotNull("Day end datum " + i, datum);
			assertEquals(Integer.valueOf(0), datum.getWatts());
			assertEquals(Long.valueOf(17940), datum.getWattHourReading());
		}
		for ( int i = 0; i < 5; i++ ) {
			AcDcEnergyDatum invalidDatum = dataSource.readCurrentDatum();
			assertNull(invalidDatum);
		}

		// now trick the data source into being the next day; we are changing the cached sample's creation date
		// to yesterday
		EnaSolarPowerDatum yesterdayDatum = new EnaSolarPowerDatum(datum.getSourceId(),
				datum.getTimestamp().minus(1L, ChronoUnit.DAYS));
		yesterdayDatum = (EnaSolarPowerDatum) yesterdayDatum.copyWithSamples(datum.asSampleOperations());
		dataSource.setSample(yesterdayDatum);

		dataSource.setUrl(getClass().getResource("deviceinfo-daystart.xml").toString());
		for ( int i = 0; i < 5; i++ ) {
			AcDcEnergyDatum nextDayDatum = dataSource.readCurrentDatum();
			assertNull(nextDayDatum);
		}

		dataSource.setUrl(getClass().getResource("deviceinfo-daily-resetting.xml").toString());
		datum = dataSource.readCurrentDatum();
		assertEquals(Integer.valueOf(628), datum.getWatts());
		assertEquals(Long.valueOf(17940), datum.getWattHourReading());
	}

	@Test
	public void parseDeviceInfoDatumWithTemporaryOutage() {
		EnaSolarXMLDatumDataSource dataSource = new EnaSolarXMLDatumDataSource();
		dataSource.setSampleCacheMs(0); // disable cache
		dataSource.setUrl(getClass().getResource("deviceinfo.xml").toString());
		dataSource.init();
		Map<String, String> deviceInfoMap = new LinkedHashMap<String, String>(10);
		deviceInfoMap.put("outputVoltage", "//data[@key='acOutputVolts']/@value");
		deviceInfoMap.put("outputPower", "//data[@key='acPower']/@value");
		deviceInfoMap.put("decaWattHoursTotal", "//data[@key='decaWattHoursTotal']/@value");
		deviceInfoMap.put("inputVoltage", "//data[@key='pvVolts']/@value");
		deviceInfoMap.put("inputPower", "//data[@key='pvPower']/@value");
		dataSource.setXpathMap(deviceInfoMap);

		AcDcEnergyDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Integer.valueOf(628), datum.getWatts());
		assertEquals(Long.valueOf(57540), datum.getWattHourReading());

		// read in a few zero-watt reading, (less than) threshold times, i.e. inverter power went out
		dataSource.setUrl(getClass().getResource("deviceinfo-daystart.xml").toString());
		for ( int i = 0; i < 5; i++ ) {
			AcDcEnergyDatum invalidDatum = dataSource.readCurrentDatum();
			assertNull(invalidDatum);
		}

		// power comes back on, and readings go back to normal
		dataSource.setUrl(getClass().getResource("deviceinfo.xml").toString());
		datum = dataSource.readCurrentDatum();
		assertEquals(Integer.valueOf(628), datum.getWatts());
		assertEquals(Long.valueOf(57540), datum.getWattHourReading());
	}

	@Test
	public void parseMetersDataDatum() {
		EnaSolarXMLDatumDataSource dataSource = new EnaSolarXMLDatumDataSource();
		dataSource.setUrls(new String[] { getClass().getResource("data.xml").toString(),
				getClass().getResource("meters.xml").toString() });
		dataSource.init();
		Map<String, String> deviceInfoMap = new LinkedHashMap<String, String>(10);
		deviceInfoMap.put("outputPower", "//OutputPower");
		deviceInfoMap.put("outputVoltage", "//OutputVoltage");
		deviceInfoMap.put("inputVoltage", "//InputVoltage");
		deviceInfoMap.put("energyLifetime", "//EnergyLifetime");
		dataSource.setXpathMap(deviceInfoMap);

		AcDcEnergyDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Integer.valueOf(214), datum.getWatts());
		assertEquals(Long.valueOf(7629660), datum.getWattHourReading());
		assertNotNull(datum.getVoltage());
		assertEquals(237.2F, datum.getVoltage().floatValue(), 0.01);
		assertNotNull(datum.getDcVoltage());
		assertEquals(419.3F, datum.getDcVoltage().floatValue(), 0.01);
	}

	@Test
	public void noRouteToHostException() {
		EnaSolarXMLDatumDataSource dataSource = new EnaSolarXMLDatumDataSource();
		dataSource.setUrls(
				new String[] { "http://192.168.9.1/data.xml", "http://192.168.9.1/meters.xml" });
		dataSource.init();
		String s = dataSource.getInfoMessage();
		assertNotNull(s);
		assertTrue(s.startsWith("Error communicating with EnaSolar inverter:"));
	}

}
