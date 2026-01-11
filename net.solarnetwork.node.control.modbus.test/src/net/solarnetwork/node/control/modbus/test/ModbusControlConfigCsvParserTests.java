/* ==================================================================
 * ModbusControlConfigCsvParserTests.java - 20/09/2022 1:30:19 pm
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

package net.solarnetwork.node.control.modbus.test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.modbus.ModbusControlConfig;
import net.solarnetwork.node.control.modbus.ModbusControlConfigCsvParser;
import net.solarnetwork.node.control.modbus.ModbusControlCsvConfigurer;
import net.solarnetwork.node.control.modbus.ModbusWritePropertyConfig;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;

/**
 * Test cases for the {@link ModbusControlConfigCsvParser} class.
 *
 * @author matt
 * @version 1.1
 */
public class ModbusControlConfigCsvParserTests {

	private ResourceBundleMessageSource messageSource;
	private ModbusControlConfigCsvParser parser;

	private List<ModbusControlConfig> results;
	private List<String> messages;

	@Before
	public void setup() {
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(ModbusControlCsvConfigurer.class.getName());

		results = new ArrayList<>();
		messages = new ArrayList<>();
		parser = new ModbusControlConfigCsvParser(results, messageSource, messages);
	}

	private void assertPropConfig(String msg, ModbusWritePropertyConfig propConfig, String controlId,
			NodeControlPropertyType propType, int address, ModbusWriteFunction fn,
			ModbusDataType dataType, Integer wordLength, BigDecimal mult, Integer scale) {
		assertThat(format("Prop config %s controlId", msg), propConfig.getControlId(), is(controlId));

	}

	@Test
	public void parse_deviceDetails_explicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-01.csv"),
				StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.LeastToMostSignificant));
	}

	@Test
	public void parse_deviceDetails_implicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-02.csv"),
				StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.LeastToMostSignificant));
	}

	@Test
	public void parse_deviceDetails_mixedKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-03.csv"),
				StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(3));
		ModbusControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.LeastToMostSignificant));

		config = results.get(2);
		assertThat("Key parsed", config.getKey(), is("3"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(3));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));
	}

	@Test
	public void parse_sample01() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-config-sample-01.csv"), StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusControlConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(5));
		assertPropConfig("1", config.getPropertyConfigs().get(0), "msg/1",
				NodeControlPropertyType.String, 1000, ModbusWriteFunction.WriteHoldingRegister,
				ModbusDataType.StringAscii, 16, null, null);
		assertPropConfig("2", config.getPropertyConfigs().get(1), "analog/1",
				NodeControlPropertyType.Float, 0, ModbusWriteFunction.WriteHoldingRegister,
				ModbusDataType.Float32, null, BigDecimal.ONE, -1);
		assertPropConfig("3", config.getPropertyConfigs().get(2), "analog/2",
				NodeControlPropertyType.Float, 2, ModbusWriteFunction.WriteHoldingRegister,
				ModbusDataType.Float32, null, BigDecimal.ONE, 1);
		assertPropConfig("4", config.getPropertyConfigs().get(3), "meter/1",
				NodeControlPropertyType.Integer, 70, ModbusWriteFunction.WriteHoldingRegister,
				ModbusDataType.UInt64, null, null, null);
		assertPropConfig("5", config.getPropertyConfigs().get(4), "switch/1",
				NodeControlPropertyType.Integer, 100, ModbusWriteFunction.WriteCoil,
				ModbusDataType.Boolean, null, null, null);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(2));
		assertPropConfig("1", config.getPropertyConfigs().get(0), "power/1",
				NodeControlPropertyType.Integer, 10, ModbusWriteFunction.WriteHoldingRegister,
				ModbusDataType.UInt16, null, new BigDecimal("0.01"), null);
		assertPropConfig("2", config.getPropertyConfigs().get(1), "power/2",
				NodeControlPropertyType.Integer, 11, ModbusWriteFunction.WriteHoldingRegister,
				ModbusDataType.UInt32, null, null, null);
	}

}
