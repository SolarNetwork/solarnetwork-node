/* ==================================================================
 * CsvDatumDataSourceTests.java - 1/04/2023 7:11:03 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csv.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.csv.CsvDatumDataSource;
import net.solarnetwork.node.datum.csv.CsvPropertyConfig;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Test cases for the {@link CsvDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvDatumDataSourceTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private CsvDatumDataSource dataSource;

	@Before
	public void setup() {
		dataSource = new CsvDatumDataSource();
		dataSource.setCharset(StandardCharsets.UTF_8);
	}

	@Test
	public void readSecondRow() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-01.csv").toString());
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

	@Test
	public void readLastRow() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-01.csv").toString());
		dataSource.setSkipRows(-1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(169.31f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:44:59", Instant::from))));
	}

	@Test
	public void readNextToLastRow() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-01.csv").toString());
		dataSource.setSkipRows(-2);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(169.27f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:34:56", Instant::from))));
	}

	@Test
	public void readNextToLastTwoRows() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-01.csv").toString());
		dataSource.setSkipRows(-3);
		dataSource.setKeepRows(2);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(2));
		List<NodeDatum> results = result.stream().collect(Collectors.toList());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		NodeDatum d = results.get(0);
		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(169.1f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:30:09", Instant::from))));

		d = results.get(1);
		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(169.27f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:34:56", Instant::from))));
	}

	@Test
	public void readAllRows() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-01.csv").toString());
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(0);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(6));
		List<NodeDatum> results = result.stream().collect(Collectors.toList());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		NodeDatum d = results.get(0);
		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));

		d = results.get(2);
		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(169.16f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:50:15", Instant::from))));

		d = results.get(5);
		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(169.31f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:44:59", Instant::from))));
	}

	@Test
	public void multipleDateColumns_list() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-02.csv").toString());
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G,H");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

	@Test
	public void multipleDateColumns_range() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-02.csv").toString());
		dataSource.setSkipRows(1);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G-H");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

	@Test
	public void readFirstRow() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-03.csv").toString());
		dataSource.setSkipRows(0);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

	@Test
	public void multiColumnRange() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-03.csv").toString());
		dataSource.setSkipRows(0);
		dataSource.setKeepRows(1);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
				new CsvPropertyConfig("merged", DatumSamplesType.Status, "H-I,C-D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID set", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
		assertThat("Merged columns parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "merged"),
				is(equalTo("D 0 22 158.87")));
	}

	@Test
	public void sourceIdColumn() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-03.csv").toString());
		dataSource.setSkipRows(0);
		dataSource.setKeepRows(1);
		dataSource.setSourceIdColumn("A");
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID parsed", d.getSourceId(), is(equalTo("OTA2201")));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

	@Test
	public void sourceIdColumns() {
		// GIVEN
		dataSource.setUrl(getClass().getResource("test-03.csv").toString());
		dataSource.setSkipRows(0);
		dataSource.setKeepRows(1);
		dataSource.setSourceIdColumn("A,H-I");
		dataSource.setDateTimeColumn("G");
		// @formatter:off
		dataSource.setPropConfigs(new CsvPropertyConfig[] {
				new CsvPropertyConfig("stationId", DatumSamplesType.Status, "A"),
				new CsvPropertyConfig("price", DatumSamplesType.Instantaneous, "D"),
		});
		// @formatter:on
		dataSource.configurationChanged(null);

		// WHEN
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		assertThat("One datum returned", result, hasSize(1));
		NodeDatum d = result.stream().findFirst().get();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));

		assertThat("Source ID parsed from multiple columns", d.getSourceId(),
				is(equalTo("OTA2201 D 0")));
		assertThat("Station ID parsed",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "stationId"),
				is(equalTo("OTA2201")));
		assertThat("Price parsed",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "price"),
				is(equalTo(158.87f)));
		assertThat("Timestamp", d.getTimestamp(),
				is(equalTo(formatter.parse("23/03/2023 10:54:48", Instant::from))));
	}

}
