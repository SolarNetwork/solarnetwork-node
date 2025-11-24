/* ==================================================================
 * ModbusServerConfigcsvParserTests.java - 9/03/2022 3:17:45 PM
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

package net.solarnetwork.node.io.modbus.server.impl.test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasEntry;
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
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusServerConfig;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;
import net.solarnetwork.node.io.modbus.server.impl.ModbusServerConfigCsvParser;
import net.solarnetwork.node.io.modbus.server.impl.ModbusServerCsvConfigurer;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link ModbusServerConfigCsvParser} class.
 *
 * @author matt
 * @version 1.0
 */
public class ModbusServerConfigCsvParserTests {

	private ResourceBundleMessageSource messageSource;
	private ModbusServerConfigCsvParser parser;

	private List<ModbusServerConfig> results;
	private List<String> messages;

	@Before
	public void setup() {
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(ModbusServerCsvConfigurer.class.getName());

		results = new ArrayList<>();
		messages = new ArrayList<>();
		parser = new ModbusServerConfigCsvParser(results, messageSource, messages);
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
		ModbusServerConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Bind address parsed", config.getBindAddress(), is("0.0.0.0"));
		assertThat("Port parsed", config.getPort(), is(5502));
		assertThat("Request throttle parsed", config.getRequestThrottle(), is(10L));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Bind address parsed", config.getBindAddress(), is("127.0.0.1"));
		assertThat("Port parsed", config.getPort(), is(5503));
		assertThat("Request throttle parsed", config.getRequestThrottle(), is(11L));
	}

	private void assertMeasConfig(String msg, MeasurementConfig measConfig, String sourceId,
			String propertyName, ModbusDataType dataType, Integer wordLength, BigDecimal mult,
			Integer scale) {
		assertThat(format("Measurement config %s source ID", msg), measConfig.getSourceId(),
				is(sourceId));
		assertThat(format("Measurement config %s property name", msg), measConfig.getPropertyName(),
				is(propertyName));
		assertThat(format("Measurement config %s data type", msg), measConfig.getDataType(),
				is(dataType));
		assertThat(format("Measurement config %s word length", msg), measConfig.getWordLength(),
				is(wordLength));
		assertThat(format("Measurement config %s unit multiplier", msg), measConfig.getUnitMultiplier(),
				is(mult));
		assertThat(format("Measurement config %s decimal scale", msg), measConfig.getDecimalScale(),
				is(scale));

	}

	@Test
	public void parse_deviceDetails_withParams() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-02.csv"),
				ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusServerConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Bind address parsed", config.getBindAddress(), is("0.0.0.0"));
		assertThat("Port parsed", config.getPort(), is(5502));
		assertThat("Request throttle parsed", config.getRequestThrottle(), is(10L));
		assertThat("Metadata UID parsed", config.getMeta(), hasEntry("uid", "Test1"));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Bind address parsed", config.getBindAddress(), is("127.0.0.1"));
		assertThat("Port parsed", config.getPort(), is(5503));
		assertThat("Request throttle parsed", config.getRequestThrottle(), is(11L));
		assertThat("Metadata UID parsed", config.getMeta(), hasEntry("uid", "Test2"));
		assertThat("Metadata allowWrites parsed", config.getMeta(), hasEntry("allowWrites", "true"));
		assertThat("Metadata daoRequired parsed", config.getMeta(), hasEntry("daoRequired", "true"));
	}

	@Test
	public void parse_deviceDetails_addrBlockJump() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-03.csv"),
				ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(1));
		ModbusServerConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Bind address parsed", config.getBindAddress(), is("0.0.0.0"));
		assertThat("Port parsed", config.getPort(), is(5502));
		assertThat("Request throttle parsed", config.getRequestThrottle(), is(100L));

		assertThat("Units parsed", config.getUnitConfigs(), hasSize(1));

		UnitConfig unitConfig = config.getUnitConfigs().get(0);
		assertThat("Unit ID parsed", unitConfig.getUnitId(), is(1));
		assertThat("2 block configs parsed because of address jump",
				unitConfig.getRegisterBlockConfigs(), is(arrayWithSize(2)));

		RegisterBlockConfig blockConfig = unitConfig.getRegisterBlockConfigs()[0];
		assertThat("Parsed holding block 1", blockConfig.getBlockType(),
				is(ModbusRegisterBlockType.Holding));
		assertThat("Parsed starting address", blockConfig.getStartAddress(), is(1000));
		assertThat("Measurement configs parsed", blockConfig.getMeasurementConfigs(),
				is(arrayWithSize(3)));
		assertMeasConfig("1", blockConfig.getMeasurementConfigs()[0], "/ev/1", "amps_a",
				ModbusDataType.Float32, null, null, null);
		assertMeasConfig("2", blockConfig.getMeasurementConfigs()[1], "/ev/1", "amps_b",
				ModbusDataType.Float32, null, null, null);
		assertMeasConfig("3", blockConfig.getMeasurementConfigs()[2], "/ev/1", "amps_c",
				ModbusDataType.Float32, null, null, null);

		RegisterBlockConfig blockConfig2 = unitConfig.getRegisterBlockConfigs()[1];
		assertThat("Parsed holding block 2", blockConfig2.getBlockType(),
				is(ModbusRegisterBlockType.Holding));
		assertThat("Parsed starting address", blockConfig2.getStartAddress(), is(2000));
		assertThat("Measurement configs parsed", blockConfig2.getMeasurementConfigs(),
				is(arrayWithSize(3)));
		assertMeasConfig("1", blockConfig2.getMeasurementConfigs()[0], "/ev/1", "meter_amps_a",
				ModbusDataType.Float32, null, null, null);
		assertMeasConfig("2", blockConfig2.getMeasurementConfigs()[1], "/ev/1", "meter_amps_b",
				ModbusDataType.Float32, null, null, null);
		assertMeasConfig("3", blockConfig2.getMeasurementConfigs()[2], "/ev/1", "meter_amps_c",
				ModbusDataType.Float32, null, null, null);
	}

	@Test
	public void parse_deviceDetails_sample01() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-config-sample-01.csv"), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(1));
		ModbusServerConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Bind address parsed", config.getBindAddress(), is("0.0.0.0"));
		assertThat("Port parsed", config.getPort(), is(5020));
		assertThat("Request throttle parsed", config.getRequestThrottle(), is(100L));

		assertThat("Units parsed", config.getUnitConfigs(), hasSize(1));

		UnitConfig unitConfig = config.getUnitConfigs().get(0);
		assertThat("Unit ID parsed", unitConfig.getUnitId(), is(1));
		assertThat("Block configs parsed", unitConfig.getRegisterBlockConfigs(), is(arrayWithSize(2)));

		RegisterBlockConfig blockConfig = unitConfig.getRegisterBlockConfigs()[0];
		assertThat("Parsed holding block", blockConfig.getBlockType(),
				is(ModbusRegisterBlockType.Holding));
		assertThat("Parsed starting address", blockConfig.getStartAddress(), is(0));
		assertThat("Measurement configs parsed", blockConfig.getMeasurementConfigs(),
				is(arrayWithSize(3)));

		assertMeasConfig("1", blockConfig.getMeasurementConfigs()[0], "power/meter/1", "watts",
				ModbusDataType.UInt16, null, BigDecimal.ONE, 0);
		assertMeasConfig("2", blockConfig.getMeasurementConfigs()[1], "power/meter/1", "wattHours",
				ModbusDataType.UInt64, null, BigDecimal.ONE, 0);
		assertMeasConfig("3", blockConfig.getMeasurementConfigs()[2], "power/meter/1", "voltage",
				ModbusDataType.Float32, null, BigDecimal.ONE, 3);

		blockConfig = unitConfig.getRegisterBlockConfigs()[1];
		assertThat("Parsed input block", blockConfig.getBlockType(), is(ModbusRegisterBlockType.Input));
		assertThat("Parsed starting address", blockConfig.getStartAddress(), is(100));
		assertThat("Measurement configs parsed", blockConfig.getMeasurementConfigs(),
				is(arrayWithSize(2)));

		assertMeasConfig("1", blockConfig.getMeasurementConfigs()[0], "power/meter/2", "frequency",
				ModbusDataType.UInt32, null, BigDecimal.TEN, 0);
		assertMeasConfig("2", blockConfig.getMeasurementConfigs()[1], "power/meter/2", "current",
				ModbusDataType.Float32, null, BigDecimal.ONE, -1);

	}

}
