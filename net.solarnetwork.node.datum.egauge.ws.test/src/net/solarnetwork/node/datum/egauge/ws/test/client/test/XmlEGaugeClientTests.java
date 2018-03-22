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

package net.solarnetwork.node.datum.egauge.ws.test.client.test;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeDatumSamplePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;

/**
 * Test cases for the XmlEGaugeClient.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class XmlEGaugeClientTests {

	public static final String TEST_FILE_INSTANTANEOUS = "instantaneous.xml";
	public static final String TEST_FILE_TOTAL = "total.xml";

	private static final String SOURCE_ID = "test-source";

	@Test
	public void getUrl() {
		XmlEGaugeClient client = new XmlEGaugeClient();

		client.setBaseUrl("http://example.com");
		client.setQueryUrl("/cgi-bin/egauge?inst");
		assertEquals("http://example.com/cgi-bin/egauge?inst", client.getUrl());

		client.setBaseUrl("http://example.com/");
		client.setQueryUrl("/cgi-bin/egauge?inst");
		assertEquals("http://example.com/cgi-bin/egauge?inst", client.getUrl());

		client.setBaseUrl("http://example.com/");
		client.setQueryUrl("cgi-bin/egauge?inst");
		assertEquals("http://example.com/cgi-bin/egauge?inst", client.getUrl());

		client.setBaseUrl("http://example.com");
		client.setQueryUrl("cgi-bin/egauge?inst");
		assertEquals("http://example.com/cgi-bin/egauge?inst", client.getUrl());

		client.setBaseUrl("http://example.com/proxy/");
		client.setQueryUrl("/cgi-bin/egauge?inst");
		assertEquals("http://example.com/proxy/cgi-bin/egauge?inst", client.getUrl());
	}

	@Test
	public void instantaneousRegisterNames() throws Exception {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		assertEquals(Arrays.asList(new String[] { "Grid", "Solar", "Solar+" }),
				client.getRegisterNames());
	}

	@Test
	public void totalRegisterNames() throws Exception {
		XmlEGaugeClient client = getTestClient(TEST_FILE_TOTAL);
		assertEquals(
				Arrays.asList(
						new String[] { "Grid", "Solar", "Solar+", "Total Usage", "Total Generation" }),
				client.getRegisterNames());
	}

	@Test
	public void instantaneousData() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.init();
		client.setSourceId(SOURCE_ID);

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

		EGaugePowerDatum datum = client.getCurrent();
		checkInstantaneousGenerationReadings(datum);
		checkInstantaneousConsumptionReadings(datum);
	}

	@Test
	public void getCurrentInvalidPropertyConfiguration() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.init();
		client.setSourceId(SOURCE_ID);

		EGaugeDatumSamplePropertyConfig[] configs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig() };

		client.setPropertyConfigs(configs);

		EGaugePowerDatum datum = client.getCurrent();
		assertThat(datum, nullValue());
	}

	@Test
	public void getCurrentValidAndInvalidPropertyConfiguration() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.init();
		client.setSourceId(SOURCE_ID);

		EGaugeDatumSamplePropertyConfig[] configs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig(), new EGaugeDatumSamplePropertyConfig(),
				new EGaugeDatumSamplePropertyConfig("generationWatts",
						GeneralDatumSamplesType.Instantaneous, new EGaugePropertyConfig("Solar+")),
				new EGaugeDatumSamplePropertyConfig("generationWattHourReading",
						GeneralDatumSamplesType.Accumulating, new EGaugePropertyConfig("Solar+")) };

		client.setPropertyConfigs(configs);

		EGaugePowerDatum datum = client.getCurrent();
		checkInstantaneousGenerationReadings(datum);
		assertThat("No consumption watts", datum.getSampleData(), not(hasKey("consumptionWatts")));
		assertThat("No consumption watt hours", datum.getSampleData(),
				not(hasKey("consumptionWattHourReading")));
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

	public static XmlEGaugeClient getTestClient(String path) {
		XmlEGaugeClient client = new XmlEGaugeClient() {

			@Override
			public String getBaseUrl() {
				return "";
			}

			@Override
			public String getUrl() {
				// Return the path to a local file containing test content
				return getClass().getResource(path).toString();
			}

		};
		client.init();
		return client;
	}

}
