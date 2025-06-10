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

package net.solarnetwork.node.datum.egauge.ws.client.test;

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeDatumSamplePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the XmlEGaugeClient.
 * 
 * @author maxieduncan
 * @version 2.0
 */
public class XmlEGaugeClientTests {

	public static final String TEST_FILE_INSTANTANEOUS = "instantaneous.xml";
	public static final String TEST_FILE_TOTAL = "total.xml";

	private static final String SOURCE_ID = "test-source";

	public static void checkInstantaneousGenerationReadings(AcDcEnergyDatum datum) {
		assertNotNull(datum);
		assertEquals(Integer.valueOf(0),
				datum.asSampleOperations().getSampleInteger(Instantaneous, "generationWatts"));
		assertEquals(Long.valueOf(196366),
				datum.asSampleOperations().getSampleLong(Accumulating, "generationWattHourReading"));
	}

	public static void checkInstantaneousConsumptionReadings(AcDcEnergyDatum datum) {
		assertNotNull(datum);
		assertEquals(Integer.valueOf(20733),
				datum.asSampleOperations().getSampleInteger(Instantaneous, "consumptionWatts"));
		assertEquals(Long.valueOf(13993341),
				datum.asSampleOperations().getSampleLong(Accumulating, "consumptionWattHourReading"));
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
		client.setSourceId(SOURCE_ID);
		return client;
	}

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

		AcDcEnergyDatum datum = client.getCurrent();
		checkInstantaneousGenerationReadings(datum);
		checkInstantaneousConsumptionReadings(datum);
	}

	@Test
	public void getCurrentInvalidPropertyConfiguration() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);

		EGaugeDatumSamplePropertyConfig[] configs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig() };

		client.setPropertyConfigs(configs);

		AcDcEnergyDatum datum = client.getCurrent();
		assertThat(datum, nullValue());
	}

	@Test
	public void getCurrentValidAndInvalidPropertyConfiguration() {
		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);

		EGaugeDatumSamplePropertyConfig[] configs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig(), new EGaugeDatumSamplePropertyConfig(),
				new EGaugeDatumSamplePropertyConfig("generationWatts", Instantaneous,
						new EGaugePropertyConfig("Solar+")),
				new EGaugeDatumSamplePropertyConfig("generationWattHourReading", Accumulating,
						new EGaugePropertyConfig("Solar+")) };

		client.setPropertyConfigs(configs);

		AcDcEnergyDatum datum = client.getCurrent();
		checkInstantaneousGenerationReadings(datum);
		assertThat("No consumption watts", datum.getSampleData(), not(hasKey("consumptionWatts")));
		assertThat("No consumption watt hours", datum.getSampleData(),
				not(hasKey("consumptionWattHourReading")));
	}

	@Test
	public void getSampleInfo() {
		SimpleAcDcEnergyDatum datum = new SimpleAcDcEnergyDatum(SOURCE_ID, Instant.now(),
				new DatumSamples());
		datum.asMutableSampleOperations().putSampleValue(Instantaneous, DcEnergyDatum.WATTS_KEY, 123);
		datum.asMutableSampleOperations().putSampleValue(Accumulating,
				DcEnergyDatum.WATT_HOUR_READING_KEY, 190823741982L);

		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.setPropertyConfigs(new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig(DcEnergyDatum.WATTS_KEY, Instantaneous,
						new EGaugePropertyConfig("Solar+")),
				new EGaugeDatumSamplePropertyConfig(DcEnergyDatum.WATT_HOUR_READING_KEY, Accumulating,
						new EGaugePropertyConfig("Solar+")) });

		String info = client.getSampleInfo(datum);
		assertThat(info, equalTo("watts (i) = 123; wattHours (a) = 190823741982"));
	}

	@Test
	public void getSampleInfoSkippingUnconfigured() {
		SimpleAcDcEnergyDatum datum = new SimpleAcDcEnergyDatum(SOURCE_ID, Instant.now(),
				new DatumSamples());
		datum.asMutableSampleOperations().putSampleValue(Instantaneous, DcEnergyDatum.WATTS_KEY, 123);
		datum.asMutableSampleOperations().putSampleValue(Accumulating,
				DcEnergyDatum.WATT_HOUR_READING_KEY, 190823741982L);

		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.setPropertyConfigs(new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig(), new EGaugeDatumSamplePropertyConfig(),
				new EGaugeDatumSamplePropertyConfig(DcEnergyDatum.WATTS_KEY, Instantaneous,
						new EGaugePropertyConfig("Solar+")),
				new EGaugeDatumSamplePropertyConfig(DcEnergyDatum.WATT_HOUR_READING_KEY, Accumulating,
						new EGaugePropertyConfig("Solar+")) });

		String info = client.getSampleInfo(datum);
		assertThat(info, equalTo("watts (i) = 123; wattHours (a) = 190823741982"));
	}

	@Test
	public void getSampleInfoSingle() {
		SimpleAcDcEnergyDatum datum = new SimpleAcDcEnergyDatum(SOURCE_ID, Instant.now(),
				new DatumSamples());
		datum.asMutableSampleOperations().putSampleValue(Instantaneous, DcEnergyDatum.WATTS_KEY, 123);
		datum.asMutableSampleOperations().putSampleValue(Accumulating,
				DcEnergyDatum.WATT_HOUR_READING_KEY, 190823741982L);

		XmlEGaugeClient client = getTestClient(TEST_FILE_INSTANTANEOUS);
		client.setPropertyConfigs(new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig(DcEnergyDatum.WATT_HOUR_READING_KEY, Accumulating,
						new EGaugePropertyConfig("Solar+")) });

		String info = client.getSampleInfo(datum);
		assertThat(info, equalTo("wattHours (a) = 190823741982"));
	}

	private OptionalServiceCollection<ExpressionService> spelExpressionService() {
		return new StaticOptionalServiceCollection<>(
				Collections.singletonList(new SpelExpressionService()));
	}

	@Test
	public void instantaneousDerivedDataListAccess() {
		XmlEGaugeClient client = getTestClient("instantaneous-2.xml");
		client.setExpressionServices(spelExpressionService());

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("whr", Accumulating, new EGaugePropertyConfig(
						"data.?[name == 'Grid+' || name == 'Grid'].size() == 2 ? (data.?[name == 'Grid+'][0].value - data.?[name == 'Grid'][0].value) / -3600 : null",
						SpelExpressionService.class.getName())) };

		client.setPropertyConfigs(defaultConfigs);

		AcDcEnergyDatum datum = client.getCurrent();
		assertThat("Evaluated property value",
				datum.asSampleOperations().getSampleBigDecimal(Accumulating, "whr"),
				equalTo(new BigDecimal("74548565")));
	}

	@Test
	public void instantaneousDerivedDataMapAccess() {
		XmlEGaugeClient client = getTestClient("instantaneous-2.xml");
		client.setExpressionServices(spelExpressionService());

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("whr", Accumulating,
						new EGaugePropertyConfig(
								"(registers['Grid+']?.value - registers['Grid']?.value) / -3600",
								SpelExpressionService.class.getName())) };

		client.setPropertyConfigs(defaultConfigs);

		AcDcEnergyDatum datum = client.getCurrent();
		assertThat("Evaluated property value",
				datum.asSampleOperations().getSampleBigDecimal(Accumulating, "whr"),
				equalTo(new BigDecimal("74548565")));
	}

	@Test
	public void instantaneousDerivedMissingValue() {
		XmlEGaugeClient client = getTestClient("instantaneous-2.xml");
		client.setExpressionServices(spelExpressionService());

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("whr", Accumulating, new EGaugePropertyConfig(
						"((registers['Grid_NOT']?.value ?: 0) - (registers['Grid']?.value) ?: 0) / -3600",
						SpelExpressionService.class.getName())) };

		client.setPropertyConfigs(defaultConfigs);

		AcDcEnergyDatum datum = client.getCurrent();
		assertThat("Evaluated property value",
				datum.asSampleOperations().getSampleBigDecimal(Accumulating, "whr"),
				equalTo(new BigDecimal("80917310")));
	}

	@Test
	public void instantaneousDerivedBadExpressionParse() {
		XmlEGaugeClient client = getTestClient("instantaneous-2.xml");
		client.setExpressionServices(spelExpressionService());

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("whr", Accumulating,
						new EGaugePropertyConfig("reg - !!", SpelExpressionService.class.getName())) };

		client.setPropertyConfigs(defaultConfigs);

		AcDcEnergyDatum datum = client.getCurrent();
		assertThat("Evaluated data", datum, nullValue());
	}

	@Test
	public void instantaneousDerivedBadExpressionEvaluation() {
		XmlEGaugeClient client = getTestClient("instantaneous-2.xml");
		client.setExpressionServices(spelExpressionService());

		EGaugeDatumSamplePropertyConfig[] defaultConfigs = new EGaugeDatumSamplePropertyConfig[] {
				new EGaugeDatumSamplePropertyConfig("whr", Accumulating, new EGaugePropertyConfig(
						"reg[Grid].foo", SpelExpressionService.class.getName())) };

		client.setPropertyConfigs(defaultConfigs);

		AcDcEnergyDatum datum = client.getCurrent();
		assertThat("Evaluated datum", datum, nullValue());
	}
}
