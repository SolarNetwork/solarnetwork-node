/* ==================================================================
 * BacnetControlCofnigCsvParserTests.java - 11/11/2022 6:21:51 am
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

package net.solarnetwork.node.control.bacnet.test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.bacnet.BacnetControlConfig;
import net.solarnetwork.node.control.bacnet.BacnetControlConfigCsvParser;
import net.solarnetwork.node.control.bacnet.BacnetControlCsvConfigurer;
import net.solarnetwork.node.control.bacnet.BacnetWritePropertyConfig;
import net.solarnetwork.node.io.bacnet.BacnetObjectType;
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link BacnetControlConfigCsvParser}.
 *
 * @author matt
 * @version 1.0
 */
public class BacnetControlConfigCsvParserTests {

	private ResourceBundleMessageSource messageSource;
	private BacnetControlConfigCsvParser parser;

	private List<BacnetControlConfig> results;
	private List<String> messages;

	@Before
	public void setup() {
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(BacnetControlCsvConfigurer.class.getName());

		results = new ArrayList<>();
		messages = new ArrayList<>();
		parser = new BacnetControlConfigCsvParser(results, messageSource, messages);
	}

	private void assertPropConfig(String msg, BacnetWritePropertyConfig propConfig, String controlId,
			NodeControlPropertyType controlType, BacnetObjectType objectType, Integer objectNumber,
			BacnetPropertyType propType, BigDecimal mult, Integer scale) {
		assertThat(format("Prop config %s control ID", msg), propConfig.getControlId(),
				is(equalTo(controlId)));
		assertThat(format("Prop config %s control type", msg), propConfig.getControlPropertyType(),
				is(equalTo(controlType)));
		assertThat(format("Prop config %s object type", msg), propConfig.getObjectType(),
				is(equalTo(objectType != null ? (Integer) objectType.getCode() : null)));
		assertThat(format("Prop config %s object number", msg), propConfig.getObjectNumber(),
				is(equalTo(objectNumber)));
		assertThat(format("Prop config %s property type", msg), propConfig.getPropertyId(),
				is(equalTo(propType != null ? (Integer) propType.getCode() : null)));
		assertThat(format("Prop config %s unitMultiplier", msg), propConfig.getUnitMultiplier(),
				is(equalTo(mult != null ? new BigDecimal(mult.intValue()) : null)));
		assertThat(format("Prop config %s scale", msg), propConfig.getDecimalScale(),
				is(equalTo(scale)));
	}

	@Test
	public void parse_deviceDetails_explicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-01.csv"),
				ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		BacnetControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
	}

	@Test
	public void parse_deviceDetails_implicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-02.csv"),
				ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		BacnetControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("2"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
	}

	@Test
	public void parse_deviceDetails_mixedKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-03.csv"),
				ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(3));
		BacnetControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));

		config = results.get(2);
		assertThat("Key parsed", config.getKey(), is("3"));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
	}

	@Test
	public void parse_sample01() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-config-sample-01.csv"), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		BacnetControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("Therm"));
		assertThat("Service name parsed", config.getServiceName(), is(equalTo("Thermostat")));
		assertThat("Service group parsed", config.getServiceGroup(), is(equalTo("HVAC")));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(3));
		assertPropConfig("D1 P1", config.getPropertyConfigs().get(0), "therm/setpoint/1",
				NodeControlPropertyType.Float, BacnetObjectType.AnalogValue, 0,
				BacnetPropertyType.PresentValue, BigDecimal.ONE, 2);
		assertPropConfig("D1 P1", config.getPropertyConfigs().get(1), "therm/setpoint/2",
				NodeControlPropertyType.Float, BacnetObjectType.AnalogValue, 1,
				BacnetPropertyType.PresentValue, BigDecimal.ONE, 2);
		assertPropConfig("D1 P1", config.getPropertyConfigs().get(2), "therm/away",
				NodeControlPropertyType.Boolean, BacnetObjectType.BinaryValue, 0,
				BacnetPropertyType.PresentValue, null, null);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("Relay"));
		assertThat("Service name parsed", config.getServiceName(), is(nullValue()));
		assertThat("Service group parsed", config.getServiceGroup(), is(nullValue()));
		assertThat("Network name parsed", config.getBacnetNetworkName(), is("BACnet/IP"));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(1));
		assertPropConfig("D2 P1", config.getPropertyConfigs().get(0), "switch/1",
				NodeControlPropertyType.Boolean, BacnetObjectType.BinaryValue, 0,
				BacnetPropertyType.PresentValue, null, null);

	}

}
