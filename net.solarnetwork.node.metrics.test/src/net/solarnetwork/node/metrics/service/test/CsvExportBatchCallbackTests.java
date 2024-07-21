/* ==================================================================
 * CsvExportBatchCallbackTests.java - 21/07/2024 7:39:20 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.service.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.node.metrics.domain.Metric.sampleValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.util.FileCopyUtils.copyToString;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Test;
import net.solarnetwork.node.metrics.service.CsvExportBatchCallback;

/**
 * Test cases for the {@link CsvExportBatchCallback} class.
 *
 * @author matt
 * @version 1.0
 */
public class CsvExportBatchCallbackTests {

	@Test
	public void export() throws IOException {
		// GIVEN
		final Instant start = LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);

		// WHEN
		StringWriter out = new StringWriter();
		try (CsvExportBatchCallback cb = new CsvExportBatchCallback(out)) {
			for ( int row = 0; row < 3; row++ ) {
				cb.handle(sampleValue(start.plusSeconds(row), "m", row));
			}
		}

		// THEN
		assertThat("CSV generated", out.toString(), is(equalTo(copyToString(new InputStreamReader(
				getClass().getResourceAsStream("test-csv-export-01.csv"), UTF_8)))));
	}

}
