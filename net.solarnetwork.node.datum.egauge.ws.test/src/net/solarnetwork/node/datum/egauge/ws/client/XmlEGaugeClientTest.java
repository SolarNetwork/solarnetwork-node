/* ==================================================================
 * XmlEGaugeClient.java - 9/03/2018 12:45:52 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;

/**
 * Test cases for the XmlEGaugeClient.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class XmlEGaugeClientTest {

	public static final String TEST_FILE_INSTANTANEOUS = "instantaneous.xml";

	private static final String HOST = "testhost";
	private static final String SOURCE = "test-source";

	@Test
	public void instantaneousData() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);

		EGaugePowerDatum datum = client.getCurrent(HOST, SOURCE);
		checkInstantaneousReadings(datum);
	}

	public static void checkInstantaneousReadings(EGaugePowerDatum datum) {
		assertNotNull(datum);
		assertEquals(Integer.valueOf(0), datum.getSolarPlusWatts());
		assertEquals(Long.valueOf(196366), datum.getSolarPlusWattHourReading());// TODO review rounding 196367?
		assertEquals(Integer.valueOf(20733), datum.getGridWatts());
		assertEquals(Long.valueOf(13993341), datum.getGridWattHourReading());
	}

	/**
	 * Tests that the data mapping converting results in the same XPath
	 * mappings.
	 */
	@Test
	public void dataMapping() {
		XmlEGaugeClient client = new XmlEGaugeClient();

		Map<String, String> xpathMap = new LinkedHashMap<String, String>(10);
		xpathMap.put("solarPlusWatts", "r[@n='Solar+'][1]/i");
		xpathMap.put("solarPlusWattHourReading", "r[@n='Solar+'][1]/v");
		xpathMap.put("gridWatts", "r[@n='Grid'][1]/i");
		xpathMap.put("gridWattHourReading", "r[@n='Grid'][1]/v");
		client.setXpathMap(xpathMap);

		String mapping = client.getDataMapping();
		assertTrue(mapping.contains("solarPlusWatts=r[@n='Solar+'][1]/i"));
		assertTrue(mapping.contains("solarPlusWattHourReading=r[@n='Solar+'][1]/v"));
		assertTrue(mapping.contains("gridWatts=r[@n='Grid'][1]/i"));
		assertTrue(mapping.contains("gridWattHourReading=r[@n='Grid'][1]/v"));
		assertEquals(4, mapping.split("\\s*,\\s*").length);

		client.setDataMapping(mapping);

		assertEquals(xpathMap, client.getXpathMap());

	}

	public static XmlEGaugeClient getTestClient(String path) {
		XmlEGaugeClient client = new XmlEGaugeClient() {

			@Override
			protected String getUrl(String host) {
				// Return the path to a local file containg test content
				return getClass().getResource(path).toString();
			}

		};
		client.init();
		return client;
	}

}
