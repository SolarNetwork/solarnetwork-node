/* ==================================================================
 * ModbusServerConfigCsvWriterTests.java - 10/03/2022 10:34:05 AM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.server.impl.ModbusServerConfigCsvWriter;
import net.solarnetwork.node.io.modbus.server.tcp.ModbusServer;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link ModbusServerConfigCsvWriter} class.
 *
 * @author matt
 * @version 1.0
 */
public class ModbusServerConfigCsvWriterTests {

	private List<Setting> loadSettingsCsv(String resource) throws IOException {
		List<Setting> result = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream(resource), ByteUtils.UTF8))) {
			String line = r.readLine(); // skip header
			while ( (line = r.readLine()) != null ) {
				String[] components = line.split(",");
				if ( components.length > 2 ) {
					result.add(new Setting(components[0], components[1], components[2], null));
				}
			}
		}
		return result;
	}

	private String[] resourceLines(String resource) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream(resource), ByteUtils.UTF8))) {
			String line = null;
			while ( (line = r.readLine()) != null ) {
				lines.add(line.trim());
			}
		}
		return lines.toArray(new String[lines.size()]);
	}

	@Test
	public void writeCsv() throws IOException {
		// GIVEN
		List<Setting> settings = loadSettingsCsv("test-settings-01.csv");

		// WHEN
		final StringWriter out = new StringWriter(4096);
		try (CsvWriter writer = CsvWriter.builder().build(out)) {
			ModbusServerConfigCsvWriter gen = new ModbusServerConfigCsvWriter(writer);
			gen.generateCsv(ModbusServer.SETTING_UID, "1", settings);
		}

		// THEN
		String[] expected = resourceLines("test-settings-01-output.csv");
		try (BufferedReader r = new BufferedReader(new StringReader(out.toString()))) {
			int i = 0;
			String line = null;
			while ( (line = r.readLine()) != null ) {
				assertThat(String.format("Wrote CSV line %d", (i + 1)), line.trim(), is(expected[i]));
				i++;
			}
			assertThat("Generated expected line count", i, is(expected.length));
		}
	}

	@Test
	public void writeCsv_withParams() throws IOException {
		// GIVEN
		List<Setting> settings = loadSettingsCsv("test-settings-01-with-params.csv");

		// WHEN
		final StringWriter out = new StringWriter(4096);
		try (CsvWriter writer = CsvWriter.builder().commentCharacter('!').build(out)) {
			ModbusServerConfigCsvWriter gen = new ModbusServerConfigCsvWriter(writer);
			gen.generateCsv(ModbusServer.SETTING_UID, "1", settings);
		}

		// THEN
		String[] expected = resourceLines("test-settings-01-output-with-params.csv");
		try (BufferedReader r = new BufferedReader(new StringReader(out.toString()))) {
			int i = 0;
			String line = null;
			while ( (line = r.readLine()) != null ) {
				assertThat(String.format("Wrote CSV line %d", (i + 1)), line.trim(), is(expected[i]));
				i++;
			}
			assertThat("Generated expected line count", i, is(expected.length));
		}
	}

}
