/* ==================================================================
 * BacnetDatumDataSourceConfigCsvParserTests.java - 9/11/2022 2:42:31 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.bacnet.test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.bacnet.BacnetCsvConfigurer;
import net.solarnetwork.node.datum.bacnet.BacnetDatumDataSourceConfig;
import net.solarnetwork.node.datum.bacnet.BacnetDatumDataSourceConfigCsvParser;
import net.solarnetwork.node.datum.bacnet.BacnetDatumMode;
import net.solarnetwork.node.datum.bacnet.BacnetDeviceConfig;
import net.solarnetwork.node.datum.bacnet.BacnetPropertyConfig;
import net.solarnetwork.node.io.bacnet.BacnetObjectType;
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link BacnetDatumDataSourceConfigCsvParser} class.
 *
 * @author matt
 * @version 1.0
 */
public class BacnetDatumDataSourceConfigCsvParserTests {

	private ResourceBundleMessageSource messageSource;
	private BacnetDatumDataSourceConfigCsvParser parser;

	private List<BacnetDatumDataSourceConfig> results;
	private List<String> messages;

	@Before
	public void setup() {
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(BacnetCsvConfigurer.class.getName());

		results = new ArrayList<>();
		messages = new ArrayList<>();
		parser = new BacnetDatumDataSourceConfigCsvParser(results, messageSource, messages);
	}

	@Test
	public void parse_deviceDetails_explicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-bacnet-config-01.csv"), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		BacnetDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is(equalTo("Therm")));
		assertThat("Service name parsed", config.getServiceName(), is(equalTo("Thermometer")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("therm/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		BacnetDeviceConfig deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(3637469)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D1 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "temp", DatumSamplesType.Instantaneous, 1, 5);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is(equalTo("Meter")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("meter/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(112821)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D2 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "watts", DatumSamplesType.Instantaneous, 1, 0);
	}

	@Test
	public void parse_deviceDetails_implicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-bacnet-config-02.csv"), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		BacnetDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is(equalTo("1")));
		assertThat("Service name parsed", config.getServiceName(), is(equalTo("Thermometer")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("therm/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		BacnetDeviceConfig deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(3637469)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D1 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "temp", DatumSamplesType.Instantaneous, 1, 5);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is(equalTo("2")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("meter/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(112821)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D2 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "watts", DatumSamplesType.Instantaneous, 1, 0);
	}

	@Test
	public void parse_deviceDetails_mixedKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-bacnet-config-03.csv"), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(3));
		BacnetDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is(equalTo("1")));
		assertThat("Service name parsed", config.getServiceName(), is(equalTo("Thermometer")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("therm/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		BacnetDeviceConfig deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(3637469)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D1 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "temp", DatumSamplesType.Instantaneous, 1, 5);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is(equalTo("P")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("meter/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(112821)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D2 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "watts", DatumSamplesType.Instantaneous, 1, 0);

		config = results.get(2);
		assertThat("Key parsed", config.getKey(), is(equalTo("3")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("therm/2")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(110001)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(1)));
		assertPropConfig("D2 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "temp", DatumSamplesType.Instantaneous, 1, 5);
	}

	private void assertPropConfig(String msg, BacnetPropertyConfig propConfig,
			BacnetObjectType objectType, Integer objectNumber, BacnetPropertyType propType,
			Float covIncrement, String datumPropName, DatumSamplesType datumPropType, Integer mult,
			int scale) {
		assertThat(format("Prop config %s object type", msg), propConfig.getObjectType(),
				is(equalTo(objectType != null ? (Integer) objectType.getCode() : null)));
		assertThat(format("Prop config %s object number", msg), propConfig.getObjectNumber(),
				is(equalTo(objectNumber)));
		assertThat(format("Prop config %s property type", msg), propConfig.getPropertyId(),
				is(equalTo(propType != null ? (Integer) propType.getCode() : null)));
		assertThat(format("Prop config %s COV increment", msg), propConfig.getCovIncrement(),
				is(equalTo(covIncrement)));
		assertThat(format("Prop config %s datum property type", msg), propConfig.getPropertyType(),
				is(equalTo(datumPropType)));
		assertThat(format("Prop config %s datum property name", msg), propConfig.getPropertyKey(),
				is(equalTo(datumPropName)));
		assertThat(format("Prop config %s slope", msg), propConfig.getSlope(),
				is(equalTo(mult != null ? new BigDecimal(mult.intValue()) : null)));
		assertThat(format("Prop config %s scale", msg), propConfig.getDecimalScale(),
				is(equalTo(scale)));
	}

	@Test
	public void parse_deviceDetails_sample01() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-bacnet-config-sample-01.csv"), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		BacnetDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is(equalTo("Therm")));
		assertThat("Service name parsed", config.getServiceName(), is(equalTo("Thermometer")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("therm/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		BacnetDeviceConfig deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(3637469)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(3)));
		assertPropConfig("D1 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "temp", DatumSamplesType.Instantaneous, 1, 5);
		assertPropConfig("D1 P2", deviceConfig.getPropConfigs()[1], BacnetObjectType.AnalogInput, 1,
				BacnetPropertyType.PresentValue, null, "tempWater", DatumSamplesType.Instantaneous, 1,
				5);
		assertPropConfig("D1 P3", deviceConfig.getPropConfigs()[2], BacnetObjectType.AnalogInput, 2,
				BacnetPropertyType.PresentValue, null, "tempOutdoor", DatumSamplesType.Instantaneous, 1,
				5);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is(equalTo("Meter")));
		assertThat("Source ID parsed", config.getSourceId(), is(equalTo("meter/1")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is(equalTo("BACnet/IP")));
		assertThat("Datum mode parsed", config.getDatumMode(), is(equalTo(BacnetDatumMode.PollOnly)));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(equalTo(5000L)));

		assertThat("Device configs parsed", config.getDeviceConfigs(), hasSize(1));
		deviceConfig = config.getDeviceConfigs().get(0);
		assertThat("Device ID", deviceConfig.getDeviceId(), is(equalTo(112821)));

		assertThat("Property configs parsed", deviceConfig.getPropConfigs(), is(arrayWithSize(2)));
		assertPropConfig("D2 P1", deviceConfig.getPropConfigs()[0], BacnetObjectType.AnalogInput, 0,
				BacnetPropertyType.PresentValue, null, "watts", DatumSamplesType.Instantaneous, 1, 0);
		assertPropConfig("D2 P2", deviceConfig.getPropConfigs()[1], BacnetObjectType.Accumulator, 0,
				BacnetPropertyType.PresentValue, null, "wattHours", DatumSamplesType.Accumulating, 1, 0);
	}

}
