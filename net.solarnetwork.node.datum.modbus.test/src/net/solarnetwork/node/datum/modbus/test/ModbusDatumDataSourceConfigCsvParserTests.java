/* ==================================================================
 * ModbusDatumDataSourceConfigcsvParserTests.java - 9/03/2022 3:17:45 PM
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

package net.solarnetwork.node.datum.modbus.test;

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
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.modbus.ExpressionConfig;
import net.solarnetwork.node.datum.modbus.ModbusCsvConfigurer;
import net.solarnetwork.node.datum.modbus.ModbusDatumDataSourceConfig;
import net.solarnetwork.node.datum.modbus.ModbusDatumDataSourceConfigCsvParser;
import net.solarnetwork.node.datum.modbus.ModbusPropertyConfig;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;

/**
 * Test cases for the {@link ModbusDatumDataSourceConfigCsvParser} class.
 *
 * @author matt
 * @version 1.0
 */
public class ModbusDatumDataSourceConfigCsvParserTests {

	private ResourceBundleMessageSource messageSource;
	private ModbusDatumDataSourceConfigCsvParser parser;

	private List<ModbusDatumDataSourceConfig> results;
	private List<String> messages;

	@Before
	public void setup() {
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(ModbusCsvConfigurer.class.getName());

		results = new ArrayList<>();
		messages = new ArrayList<>();
		parser = new ModbusDatumDataSourceConfigCsvParser(results, messageSource, messages);
	}

	@Test
	public void parse_deviceDetails_explicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-01.csv"),
				StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
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
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("2"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
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
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(3));
		ModbusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.LeastToMostSignificant));

		config = results.get(2);
		assertThat("Key parsed", config.getKey(), is("3"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/3"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(3));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));
	}

	private void assertPropConfig(String msg, ModbusPropertyConfig propConfig, String name,
			DatumSamplesType propType, int address, ModbusReadFunction fn, ModbusDataType dataType,
			Integer wordLength, BigDecimal mult, Integer scale) {
		assertThat(format("Prop config %s name", msg), propConfig.getName(), is(name));

	}

	@Test
	public void parse_expression() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(getClass().getResourceAsStream("test-config-04.csv"),
				StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(1));
		ModbusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		assertThat("No property configs", config.getPropertyConfigs(), hasSize(0));

		assertThat("Parsed expression config", config.getExpressionConfigs(), hasSize(1));
		ExpressionConfig exprConfig = config.getExpressionConfigs().get(0);
		assertThat("Property name", exprConfig.getName(), is("foo"));
		assertThat("Property type", exprConfig.getDatumPropertyType(),
				is(DatumSamplesType.Instantaneous));
		assertThat("Expression service ID", exprConfig.getExpressionServiceId(),
				is(ModbusDatumDataSourceConfigCsvParser.DEFAULT_EXPRESSION_SERVICE_ID));
		assertThat("Expression", exprConfig.getExpression(), is("foo + bar"));
	}

	@Test
	public void parse_deviceDetails_sample01() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-config-sample-01.csv"), StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		ModbusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/1"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(1));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(4));
		assertPropConfig("1", config.getPropertyConfigs().get(0), "error", DatumSamplesType.Status, 1000,
				ModbusReadFunction.ReadHoldingRegister, ModbusDataType.StringAscii, 16, null, null);
		assertPropConfig("2", config.getPropertyConfigs().get(1), "current",
				DatumSamplesType.Instantaneous, 0, ModbusReadFunction.ReadHoldingRegister,
				ModbusDataType.Float32, null, BigDecimal.ONE, -1);
		assertPropConfig("3", config.getPropertyConfigs().get(2), "voltage",
				DatumSamplesType.Instantaneous, 2, ModbusReadFunction.ReadHoldingRegister,
				ModbusDataType.Float32, null, BigDecimal.ONE, 1);
		assertPropConfig("4", config.getPropertyConfigs().get(3), "wattHours",
				DatumSamplesType.Accumulating, 70, ModbusReadFunction.ReadHoldingRegister,
				ModbusDataType.UInt64, null, null, null);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Source ID parsed", config.getSourceId(), is("power/2"));
		assertThat("Network name parsed", config.getModbusNetworkName(), is("Modbus Port"));
		assertThat("Unit ID parsed", config.getUnitId(), is(2));
		assertThat("Sample cache ms parsed", config.getSampleCacheMs(), is(5000L));
		assertThat("Max read parsed", config.getMaxReadWordCount(), is(64));
		assertThat("Word order parsed", config.getWordOrder(),
				is(ModbusWordOrder.MostToLeastSignificant));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(2));
		assertPropConfig("1", config.getPropertyConfigs().get(0), "frequency",
				DatumSamplesType.Instantaneous, 10, ModbusReadFunction.ReadInputRegister,
				ModbusDataType.UInt16, null, new BigDecimal("0.01"), null);
		assertPropConfig("2", config.getPropertyConfigs().get(1), "watts",
				DatumSamplesType.Instantaneous, 11, ModbusReadFunction.ReadInputRegister,
				ModbusDataType.UInt32, null, null, null);

	}

}
