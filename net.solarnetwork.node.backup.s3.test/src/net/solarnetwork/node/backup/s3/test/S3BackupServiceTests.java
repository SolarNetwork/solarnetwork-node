/* ==================================================================
 * S3BackupServiceTests.java - 5/07/2021 2:10:39 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import net.solarnetwork.common.s3.S3Client;
import net.solarnetwork.common.s3.S3ObjectMeta;
import net.solarnetwork.common.s3.S3ObjectReference;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.SimpleBackupFilter;
import net.solarnetwork.node.backup.s3.S3BackupService;
import net.solarnetwork.test.SystemPropertyMatchTestRule;

/**
 * Test cases for the {@link S3BackupService} class.
 *
 * @author matt
 * @version 1.0
 */
public class S3BackupServiceTests {

	private S3BackupService service;

	/** Only run when the {@code s3-int} system property is defined. */
	@ClassRule
	public static SystemPropertyMatchTestRule PROFILE_RULE = new SystemPropertyMatchTestRule("s3-int");

	@Before
	public void setup() throws IOException {
		Properties env = new Properties();
		env.load(getClass().getClassLoader().getResourceAsStream("s3-backup.properties"));

		service = new S3BackupService();
		service.setAccessToken(env.getProperty("accessToken"));
		service.setAccessSecret(env.getProperty("accessSecret"));
		service.setRegionName(env.getProperty("regionName"));
		service.setBucketName(env.getProperty("bucketName"));
		service.setObjectKeyPrefix(env.getProperty("objectKeyPrefix",
				String.format("solarnode-backups/%s/", UUID.randomUUID().toString())));
		service.configurationChanged(null);
	}

	@After
	public void teardown() throws IOException {
		Set<S3ObjectReference> list = service.getS3Client().listObjects(service.getObjectKeyPrefix());
		List<String> keyList = list.stream().map(S3ObjectReference::getKey).collect(Collectors.toList());
		if ( !keyList.isEmpty() ) {
			service.getS3Client().deleteObjects(keyList);
		}
	}

	@Test
	public void listBackups_none() {
		// GIVEN

		// WHEN
		Collection<Backup> backups = service.getAvailableBackups();

		// THEN
		assertThat("No backups exist", backups, hasSize(0));
	}

	@Test
	public void listBackups() throws IOException {
		// GIVEN
		S3Client client = service.getS3Client();
		LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
		for ( int i = 0; i < 10; i++ ) {
			LocalDateTime ts = start.plusSeconds(i * 60);
			Date d = Date.from(ZonedDateTime.of(ts, ZoneId.systemDefault()).toInstant());
			String key = format("%sbackup-meta/node-1-backup-%tY%<tm%<tdT%<tH%<tM%<tS",
					service.getObjectKeyPrefix(), ts);
			S3ObjectMeta meta = new S3ObjectMeta(2, d);
			try (ByteArrayInputStream in = new ByteArrayInputStream("Hi".getBytes())) {
				client.putObject(key, in, meta, null, null);
			}
		}

		// WHEN
		Collection<Backup> backups = service.getAvailableBackups();

		// THEN
		assertThat("Expected backups exist", backups, hasSize(10));
		int i = 0;
		for ( Backup b : backups ) {
			LocalDateTime ts = start.plusSeconds(i * 60);
			Date d = Date.from(ZonedDateTime.of(ts, ZoneId.systemDefault()).toInstant());
			assertThat(format("Backup %d ordered oldest to newest", i), b.getDate(), is(equalTo(d)));
			i++;
		}
	}

	@Test
	public void listBackups_forNode() throws IOException {
		// GIVEN
		S3Client client = service.getS3Client();
		LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
		for ( int nodeId = 1; nodeId < 3; nodeId++ ) {
			for ( int i = 0; i < 10; i++ ) {
				LocalDateTime ts = start.plusSeconds(i * 60);
				Date d = Date.from(ZonedDateTime.of(ts, ZoneId.systemDefault()).toInstant());
				String key = format("%sbackup-meta/node-%d-backup-%tY%<tm%<tdT%<tH%<tM%<tS",
						service.getObjectKeyPrefix(), nodeId, ts);
				S3ObjectMeta meta = new S3ObjectMeta(2, d);
				try (ByteArrayInputStream in = new ByteArrayInputStream("Hi".getBytes())) {
					client.putObject(key, in, meta, null, null);
				}
			}
		}

		// WHEN
		FilterResults<Backup, String> result = service.findBackups(SimpleBackupFilter.filterForNode(2L));

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		List<Backup> resultList = StreamSupport.stream(result.spliterator(), false).toList();
		assertThat("Expected backups for node exist", resultList, hasSize(10));
		int i = 0;
		for ( Backup b : result ) {
			LocalDateTime ts = start.plusSeconds(i * 60);
			Date d = Date.from(ZonedDateTime.of(ts, ZoneId.systemDefault()).toInstant());
			assertThat(format("Backup %d ordered oldest to newest", i), b.getDate(), is(equalTo(d)));
			i++;
		}
	}

	@Test
	public void listBackups_forNode_page() throws IOException {
		// GIVEN
		S3Client client = service.getS3Client();
		LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
		for ( int nodeId = 1; nodeId < 3; nodeId++ ) {
			for ( int i = 0; i < 10; i++ ) {
				LocalDateTime ts = start.plusSeconds(i * 60);
				Date d = Date.from(ZonedDateTime.of(ts, ZoneId.systemDefault()).toInstant());
				String key = format("%sbackup-meta/node-%d-backup-%tY%<tm%<tdT%<tH%<tM%<tS",
						service.getObjectKeyPrefix(), nodeId, ts);
				S3ObjectMeta meta = new S3ObjectMeta(2, d);
				try (ByteArrayInputStream in = new ByteArrayInputStream("Hi".getBytes())) {
					client.putObject(key, in, meta, null, null);
				}
			}
		}

		// WHEN
		SimpleBackupFilter pageFilter = SimpleBackupFilter.filterForNode(2L);
		pageFilter.setOffset(5L);
		pageFilter.setMax(2);
		FilterResults<Backup, String> result = service.findBackups(pageFilter);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		List<Backup> resultList = StreamSupport.stream(result.spliterator(), false).toList();
		assertThat("Expected backups page for node exist", resultList, hasSize(2));
		int i = 5;
		for ( Backup b : result ) {
			LocalDateTime ts = start.plusSeconds(i * 60);
			Date d = Date.from(ZonedDateTime.of(ts, ZoneId.systemDefault()).toInstant());
			assertThat(format("Backup %d ordered oldest to newest", i), b.getDate(), is(equalTo(d)));
			i++;
		}
	}

}
