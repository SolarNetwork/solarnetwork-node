/* ==================================================================
 * EGaugeXMLDatumDataSource.java - Oct 2, 2011 8:50:13 PM
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

package net.solarnetwork.node.datum.egauge.ws.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.datum.egauge.ws.EGaugeDatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeDatumSamplePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;
import net.solarnetwork.node.datum.egauge.ws.test.client.test.XmlEGaugeClientTest;

/**
 * Test case for the EGaugeDatumDataSource.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class EGaugeDatumDataSourceTest {

	private static final String TEST_SOURCE = "solarnetwork egague test";

	@Test
	public void getCurrent() {
		EGaugeDatumDataSource source = new EGaugeDatumDataSource();
		XmlEGaugeClient client = XmlEGaugeClientTest
				.getTestClient(XmlEGaugeClientTest.TEST_FILE_INSTANTANEOUS);
		client.setSourceId(TEST_SOURCE);

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("consumptionWatts",
						GeneralDatumSamplesType.Instantaneous, new EGaugePropertyConfig("Grid")),
				new EGaugeDatumSamplePropertyConfig("consumptionWattHourReading",
						GeneralDatumSamplesType.Accumulating, new EGaugePropertyConfig("Grid")),
				new EGaugeDatumSamplePropertyConfig("generationWatts",
						GeneralDatumSamplesType.Instantaneous, new EGaugePropertyConfig("Solar+")),
				new EGaugeDatumSamplePropertyConfig("generationWattHourReading",
						GeneralDatumSamplesType.Accumulating, new EGaugePropertyConfig("Solar+")) };

		client.setPropertyConfigs(defaultConfigs);

		source.setClient(client);

		EGaugePowerDatum datum = source.readCurrentDatum();
		assertEquals(TEST_SOURCE, datum.getSourceId());

		XmlEGaugeClientTest.checkInstantaneousGenerationReadings(datum);
		XmlEGaugeClientTest.checkInstantaneousConsumptionReadings(datum);
	}

}
