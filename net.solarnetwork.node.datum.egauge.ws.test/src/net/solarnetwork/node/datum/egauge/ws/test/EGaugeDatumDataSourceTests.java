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

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;
import net.solarnetwork.node.datum.egauge.ws.EGaugeDatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeDatumSamplePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;
import net.solarnetwork.node.datum.egauge.ws.client.test.XmlEGaugeClientTests;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.DcEnergyDatum;
import net.solarnetwork.util.ClassUtils;

/**
 * Test case for the EGaugeDatumDataSource.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class EGaugeDatumDataSourceTests {

	private static final String TEST_SOURCE = "solarnetwork egague test";

	@Test
	public void getCurrent() {
		EGaugeDatumDataSource source = new EGaugeDatumDataSource();
		XmlEGaugeClient client = XmlEGaugeClientTests
				.getTestClient(XmlEGaugeClientTests.TEST_FILE_INSTANTANEOUS);
		client.setSourceId(TEST_SOURCE);

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("consumptionWatts", Instantaneous,
						new EGaugePropertyConfig("Grid")),
				new EGaugeDatumSamplePropertyConfig("consumptionWattHourReading", Accumulating,
						new EGaugePropertyConfig("Grid")),
				new EGaugeDatumSamplePropertyConfig("generationWatts", Instantaneous,
						new EGaugePropertyConfig("Solar+")),
				new EGaugeDatumSamplePropertyConfig("generationWattHourReading", Accumulating,
						new EGaugePropertyConfig("Solar+")) };

		client.setPropertyConfigs(defaultConfigs);

		source.setClient(client);

		AcDcEnergyDatum datum = source.readCurrentDatum();
		assertEquals(TEST_SOURCE, datum.getSourceId());

		XmlEGaugeClientTests.checkInstantaneousGenerationReadings(datum);
		XmlEGaugeClientTests.checkInstantaneousConsumptionReadings(datum);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> loadSettings(String resource) {
		Properties settings = new Properties();
		try {
			settings.load(getClass().getResourceAsStream(resource));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return (Map) settings;
	}

	/**
	 * Simulate configuring via SettingSpecifier framework.
	 */
	@Test
	public void configureViaSettings() throws IOException {
		EGaugeDatumDataSource source = new EGaugeDatumDataSource();
		XmlEGaugeClient client = XmlEGaugeClientTests
				.getTestClient(XmlEGaugeClientTests.TEST_FILE_INSTANTANEOUS);
		source.setClient(client);

		ClassUtils.setBeanProperties(source, loadSettings("config-01.properties"));

		assertThat("Client source ID", client.getSourceId(), equalTo("test.source"));
		assertThat("Client prop configs", client.getPropertyConfigs(), arrayWithSize(2));

		DcEnergyDatum datum = source.readCurrentDatum();
		assertThat("Datum created", datum.getTimestamp(), notNullValue());
		assertThat("Datum sourceId", datum.getSourceId(), equalTo("test.source"));
		assertThat("Datum watts", datum.getWatts(), equalTo(20733));
		assertThat("Datum wattHours", datum.getWattHourReading(), equalTo(13993341L));
	}

}
