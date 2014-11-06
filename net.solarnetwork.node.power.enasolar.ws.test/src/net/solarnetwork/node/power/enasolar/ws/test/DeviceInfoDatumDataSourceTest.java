/* ==================================================================
 * DeviceInfoDatumDataSourceTest.java - Oct 2, 2011 9:24:34 PM
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
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.power.enasolar.ws.DeviceInfoDatumDataSource;
import net.solarnetwork.node.test.AbstractNodeTest;
import org.junit.Test;

/**
 * Test case for the {@link DeviceInfoDatumDataSource} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DeviceInfoDatumDataSourceTest extends AbstractNodeTest {

	@Test
	public void parseDeviceInfoDatum() {
		DeviceInfoDatumDataSource dataSource = new DeviceInfoDatumDataSource();
		dataSource.setUrl(getClass().getResource("deviceinfo.xml").toString());
		dataSource.init();
		Map<String, String> deviceInfoMap = new LinkedHashMap<String, String>(10);
		deviceInfoMap.put("outputVoltage", "//data[@key='acOutputVolts']/@value");
		deviceInfoMap.put("outputPower", "//data[@key='acPower']/@value");
		deviceInfoMap.put("decaWattHoursTotal", "//data[@key='decaWattHoursTotal']/@value");
		deviceInfoMap.put("inputVoltage", "//data[@key='pvVolts']/@value");
		deviceInfoMap.put("inputPower", "//data[@key='pvPower']/@value");
		dataSource.setXpathMap(deviceInfoMap);

		GeneralNodePVEnergyDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Long.valueOf(57540), datum.getWattHourReading());
		assertEquals(Integer.valueOf(628), datum.getWatts());
		assertNotNull(datum.getVoltage());
		assertEquals(241.1, datum.getVoltage().floatValue(), 0.01);
		assertNotNull(datum.getDCPower());
		assertEquals(Integer.valueOf(681), datum.getDCPower());
		assertNotNull(datum.getDCVoltage());
		assertEquals(304.3F, datum.getDCVoltage().floatValue(), 0.01);
	}

	@Test
	public void parseMetersDataDatum() {
		DeviceInfoDatumDataSource dataSource = new DeviceInfoDatumDataSource();
		dataSource.setUrls(new String[] { getClass().getResource("data.xml").toString(),
				getClass().getResource("meters.xml").toString() });
		dataSource.init();
		Map<String, String> deviceInfoMap = new LinkedHashMap<String, String>(10);
		deviceInfoMap.put("outputPower", "//OutputPower");
		deviceInfoMap.put("outputVoltage", "//OutputVoltage");
		deviceInfoMap.put("inputVoltage", "//InputVoltage");
		deviceInfoMap.put("energyLifetime", "//EnergyLifetime");
		dataSource.setXpathMap(deviceInfoMap);

		GeneralNodePVEnergyDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Integer.valueOf(214), datum.getWatts());
		assertEquals(Long.valueOf(7629660), datum.getWattHourReading());
		assertNotNull(datum.getVoltage());
		assertEquals(237.2F, datum.getVoltage().floatValue(), 0.01);
		assertNotNull(datum.getDCVoltage());
		assertEquals(419.3F, datum.getDCVoltage().floatValue(), 0.01);
	}
}
