/* ==================================================================
 * WMBusDatumDataSourceConfigCsvParserTests.java - 30/09/2022 1:36:00 pm
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

package net.solarnetwork.node.datum.mbus.test;

import static net.solarnetwork.node.datum.mbus.test.MBusDatumDataSourceConfigCsvParserTests.assertPropConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
import net.solarnetwork.node.datum.mbus.WMBusCsvConfigurer;
import net.solarnetwork.node.datum.mbus.WMBusDatumDataSourceConfig;
import net.solarnetwork.node.datum.mbus.WMBusDatumDataSourceConfigCsvParser;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataType;

/**
 * Test cases for the {@link WMBusDatumDataSourceConfigCsvParser} class.
 *
 * @author matt
 * @version 1.0
 */
public class WMBusDatumDataSourceConfigCsvParserTests {

	private ResourceBundleMessageSource messageSource;
	private WMBusDatumDataSourceConfigCsvParser parser;

	private List<WMBusDatumDataSourceConfig> results;
	private List<String> messages;

	@Before
	public void setup() {
		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(WMBusCsvConfigurer.class.getName());

		results = new ArrayList<>();
		messages = new ArrayList<>();
		parser = new WMBusDatumDataSourceConfigCsvParser(results, messageSource, messages);
	}

	@Test
	public void parse_deviceDetails_explicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-wmbus-config-01.csv"), StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		WMBusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Service name parsed", config.getServiceName(), is("S1"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G1"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("123ABC"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("ABCDEF"));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Service name parsed", config.getServiceName(), is("S2"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G2"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("234DEF"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("DEFABC"));
	}

	@Test
	public void parse_deviceDetails_implicitKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-wmbus-config-02.csv"), StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(2));
		WMBusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Service name parsed", config.getServiceName(), is("S1"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G1"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("123ABC"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("ABCDEF"));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("2"));
		assertThat("Service name parsed", config.getServiceName(), is("S2"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G2"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("234DEF"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("DEFABC"));
	}

	@Test
	public void parse_deviceDetails_mixedKeys() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-wmbus-config-03.csv"), StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}

		// THEN
		assertThat("Read device infos", results, hasSize(3));
		WMBusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("1"));
		assertThat("Service name parsed", config.getServiceName(), is("S1"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G1"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("123ABC"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("ABCDEF"));

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P"));
		assertThat("Service name parsed", config.getServiceName(), is("S2"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G2"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("234DEF"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("DEFABC"));

		config = results.get(2);
		assertThat("Key parsed", config.getKey(), is("3"));
		assertThat("Service name parsed", config.getServiceName(), is("S3"));
		assertThat("Service group parsed", config.getServiceGroup(), is("G3"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("345EF0"));
		assertThat("Decryption key parsed", config.getDecryptionKey(), is("012345"));
	}

	@Test
	public void parse_sample01() throws IOException {
		// GIVEN

		// WHEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-wmbus-config-sample-01.csv"),
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
		WMBusDatumDataSourceConfig config = results.get(0);
		assertThat("Key parsed", config.getKey(), is("P1"));
		assertThat("Service name parsed", config.getServiceName(), is(nullValue()));
		assertThat("Service group parsed", config.getServiceGroup(), is(nullValue()));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("0123456789ABCDEF"));
		assertThat("Decryption key parsed", config.getDecryptionKey(),
				is("0123456789ABCDEF0123456789ABCDEF"));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(3));
		assertPropConfig("1.1", config.getPropertyConfigs().get(0), "current",
				DatumSamplesType.Instantaneous, MBusDataType.Double, MBusDataDescription.Current,
				BigDecimal.ONE, -1);
		assertPropConfig("1.2", config.getPropertyConfigs().get(1), "voltage",
				DatumSamplesType.Instantaneous, MBusDataType.Double, MBusDataDescription.Voltage,
				BigDecimal.ONE, 1);
		assertPropConfig("1.3", config.getPropertyConfigs().get(2), "wattHours",
				DatumSamplesType.Accumulating, MBusDataType.Long, MBusDataDescription.Energy, null,
				null);

		config = results.get(1);
		assertThat("Key parsed", config.getKey(), is("P2"));
		assertThat("Network name parsed", config.getNetworkName(), is("WM-Bus Port"));
		assertThat("Address parsed", config.getAddress(), is("123456789ABCDEF0"));
		assertThat("Decryption key parsed", config.getDecryptionKey(),
				is("123456789ABCDEF0123456789ABCDEF0"));

		assertThat("Properties parsed", config.getPropertyConfigs(), hasSize(2));
		assertPropConfig("2.1", config.getPropertyConfigs().get(0), "frequency",
				DatumSamplesType.Instantaneous, MBusDataType.Double, MBusDataDescription.Frequency,
				BigDecimal.ONE, 2);
		assertPropConfig("2.2", config.getPropertyConfigs().get(1), "watts",
				DatumSamplesType.Instantaneous, MBusDataType.Double, MBusDataDescription.Power,
				BigDecimal.ONE, 0);
	}

}
