/* ==================================================================
 * BacnetControlConfigTests.java - 11/11/2022 8:41:49 am
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
import net.solarnetwork.node.control.bacnet.BacnetControl;
import net.solarnetwork.node.control.bacnet.BacnetControlConfig;
import net.solarnetwork.node.control.bacnet.BacnetControlConfigCsvParser;
import net.solarnetwork.node.control.bacnet.BacnetControlCsvConfigurer;
import net.solarnetwork.node.settings.SettingValueBean;

/**
 * Test cases for the {@link BacnetControlConfig} class.
 *
 * @author matt
 * @version 1.1
 */
public class BacnetControlConfigTests {

	private static final String TS = "2022-10-11 08:52:00";

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

	private String csv(List<SettingValueBean> settings) {
		StringBuilder buf = new StringBuilder();
		buf.append("key,type,value,flags,modified\n");
		for ( SettingValueBean s : settings ) {
			buf.append(s.getProviderKey()).append('.').append(s.getInstanceKey());
			buf.append(',').append(s.getKey());
			buf.append(',').append(s.getValue());
			buf.append(',').append(0);
			buf.append(',').append(TS).append('\n');
		}
		return buf.toString();
	}

	private String resourceString(String resource) {
		StringBuilder buf = new StringBuilder();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream(resource), UTF_8))) {
			String line = null;
			while ( (line = r.readLine()) != null ) {
				buf.append(line.trim()).append('\n');
			}
		} catch ( IOException e ) {
			throw new RuntimeException(
					"IO error reading resource [" + resource + "]: " + e.getMessage());
		}
		return buf.toString();
	}

	@Test
	public void settings_sample01() throws IOException {
		// GIVEN
		try (Reader in = new InputStreamReader(
				getClass().getResourceAsStream("test-config-sample-01.csv"), UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}
		assertThat("Read device infos", results, hasSize(2));

		// WHEN
		BacnetControlConfig config = results.get(0);
		List<SettingValueBean> settings = config.toSettingValues(BacnetControl.SETTING_UID);

		// THEN
		String[] csv = csv(settings).split("\n");
		String[] expected = resourceString("test-settings-01.csv").split("\n");
		for ( int i = 0; i < expected.length && i < csv.length; i++ ) {
			assertThat(String.format("Settings line %d", i), csv[i], is(equalTo(expected[i])));
		}
		assertThat("Settings count", csv.length, is(equalTo(expected.length)));
	}

}
