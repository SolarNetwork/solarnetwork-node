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
import org.junit.Test;
import net.solarnetwork.node.datum.egauge.ws.EGaugeDatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig.EGaugeReadingType;

/**
 * Test cases for the XmlEGaugeClient.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class XmlEGaugeClientTest {

	public static final String TEST_FILE_INSTANTANEOUS = "instantaneous.xml";
	public static final String TEST_FILE_TOTAL = "total.xml";

	private static final String HOST = "testhost";
	private static final String SOURCE_ID = "test-source";

	@Test
	public void instantaneousData() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.init();
		client.setHost(HOST);

		EGaugeDatumDataSource source = new EGaugeDatumDataSource();
		source.init();
		source.setSourceId(SOURCE_ID);

		EGaugePowerDatum datum = client.getCurrent(source);
		checkInstantaneousGenerationReadings(datum);
		checkInstantaneousConsumptionReadings(datum);
	}

	public static void checkInstantaneousGenerationReadings(EGaugePowerDatum datum) {
		assertNotNull(datum);
		assertEquals(Integer.valueOf(0), datum.getInstantaneousSampleInteger("generationWatts"));
		assertEquals(Long.valueOf(196366), datum.getAccumulatingSampleLong("generationWattHourReading"));
	}

	public static void checkInstantaneousConsumptionReadings(EGaugePowerDatum datum) {
		assertNotNull(datum);
		assertEquals(Integer.valueOf(20733), datum.getInstantaneousSampleInteger("consumptionWatts"));
		assertEquals(Long.valueOf(13993341),
				datum.getAccumulatingSampleLong("consumptionWattHourReading"));
	}

	@Test
	public void totalData() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_TOTAL);
		client.init();
		client.setHost(HOST);

		EGaugeDatumDataSource source = new EGaugeDatumDataSource();
		source.init();
		source.setSourceId(SOURCE_ID);
		source.setPropertyConfigs(new EGaugePropertyConfig[] {
				new EGaugePropertyConfig("test1", "Grid", EGaugeReadingType.TOTAL),
				new EGaugePropertyConfig("test2", "Solar", EGaugeReadingType.TOTAL),
				new EGaugePropertyConfig("test3", "Solar+", EGaugeReadingType.TOTAL),
				new EGaugePropertyConfig("test4", "Total Usage", EGaugeReadingType.TOTAL),
				new EGaugePropertyConfig("test5", "Total Generation", EGaugeReadingType.TOTAL), });

		EGaugePowerDatum datum = client.getCurrent(source);
		checkTotalReadings(datum);
	}

	public void checkTotalReadings(EGaugePowerDatum datum) {
		assertNotNull(datum);
		assertEquals(Long.valueOf(13956806), datum.getAccumulatingSampleLong("test1WattHourReading"));
		assertEquals(Long.valueOf(163286), datum.getAccumulatingSampleLong("test2WattHourReading"));
		assertEquals(Long.valueOf(196366), datum.getAccumulatingSampleLong("test3WattHourReading"));
		assertEquals(Long.valueOf(14153173), datum.getAccumulatingSampleLong("test4WattHourReading"));
		assertEquals(Long.valueOf(163286), datum.getAccumulatingSampleLong("test5WattHourReading"));
	}

	public static XmlEGaugeClient getTestClient(String path) {
		XmlEGaugeClient client = new XmlEGaugeClient() {

			@Override
			protected String getUrl(String host, EGaugeReadingType type) {
				// Return the path to a local file containing test content
				return getClass().getResource(path).toString();
			}

		};
		client.init();
		return client;
	}

}
