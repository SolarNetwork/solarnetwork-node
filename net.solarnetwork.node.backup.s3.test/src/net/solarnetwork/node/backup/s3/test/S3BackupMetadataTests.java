/* ==================================================================
 * S3BackupMetadataTests.java - 20/10/2025 5:23:56 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.backup.s3.test;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.common.s3.S3ObjectRef;
import net.solarnetwork.node.backup.BackupServiceSupport;
import net.solarnetwork.node.backup.s3.S3BackupMetadata;

/**
 * Test cases for the {@link S3BackupMetadata} class.
 *
 * @author matt
 * @version 1.0
 */
public class S3BackupMetadataTests {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	@Test
	public void toJson() throws IOException {
		// GIVEN
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final DateTimeFormatter dateFmt = DateTimeFormatter
				.ofPattern(BackupServiceSupport.BACKUP_KEY_DATE_FORMAT).withZone(UTC);
		final String key = "node-2-backup-%s".formatted(dateFmt.format(ts));
		final S3BackupMetadata backup = new S3BackupMetadata(
				new S3ObjectRef(key, 2, Date.from(ts), null));

		// WHEN
		String json = MAPPER.writeValueAsString(backup);

		// THEN
		assertThat("JSON generated", json, is(equalTo("""
				{"key":"%s","nodeId":%d,"date":%d,"complete":%s}""".formatted(backup.getKey(),
				backup.getNodeId(), backup.getDate().getTime(), backup.isComplete()))));
	}

}
