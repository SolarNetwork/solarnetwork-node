/* ==================================================================
 * FileSystemBackupServiceTest.java - Mar 27, 2013 3:36:28 PM
 *
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.backup.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceIterable;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceInfo;
import net.solarnetwork.node.backup.BackupStatus;
import net.solarnetwork.node.backup.FileSystemBackupService;
import net.solarnetwork.node.backup.ResourceBackupResource;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test case for the {@link FileSystemBackupService}.
 *
 * @author matt
 * @version 2.0
 */
public class FileSystemBackupServiceTest {

	private static final Long TEST_NODE_ID = 12345L;

	private Long identityNodeId = TEST_NODE_ID;
	private FileSystemBackupService service;

	@Before
	public void setup() {
		service = new FileSystemBackupService();
		service.setBackupDir(new File(System.getProperty("java.io.tmpdir")));
		service.setAdditionalBackupCount(0);
		service.setIdentityService(new StaticOptionalService<IdentityService>(new IdentityService() {

			@Override
			public Long getNodeId() {
				return identityNodeId;
			}

			@Override
			public Principal getNodePrincipal() {
				return null;
			}

			@Override
			public String getSolarNetHostName() {
				return null;
			}

			@Override
			public Integer getSolarNetHostPort() {
				return null;
			}

			@Override
			public String getSolarNetSolarInUrlPrefix() {
				return null;
			}

			@Override
			public String getSolarInBaseUrl() {
				return null;
			}

			@Override
			public String getSolarInMqttUrl() {
				return null;
			}

		}));
		service.removeAllBackups();
	}

	@After
	public void pause() throws Exception {
		// pause so backup file names differ
		Thread.sleep(1000);
	}

	@Test
	public void getInitialInfo() {
		BackupServiceInfo info = service.getInfo();
		assertNotNull(info);
		assertEquals(FileSystemBackupService.KEY, service.getKey());
		assertNull("No backup has been made", info.getMostRecentBackupDate());
		assertEquals(BackupStatus.Configured, info.getStatus());
	}

	@Test
	public void noAvailableBackups() {
		final BackupService bs = service;
		Collection<Backup> backups = bs.getAvailableBackups();
		assertNotNull(backups);
		assertEquals(0, backups.size());
	}

	@Test
	public void backupNull() {
		final BackupService bs = service;
		Backup result = bs.performBackup(null);
		assertNull(result);
	}

	@Test
	public void backupEmpty() {
		final BackupService bs = service;
		Backup result = bs.performBackup(new ArrayList<BackupResource>());
		assertNull(result);
	}

	@Test
	public void backupOne() throws IOException, InterruptedException {
		final ClassPathResource testResource = new ClassPathResource("test-context.xml",
				AbstractNodeTransactionalTest.class);
		final BackupService bs = service;
		final List<BackupResource> resources = new ArrayList<BackupResource>(1);
		final Calendar now = new GregorianCalendar();
		now.set(Calendar.MILLISECOND, 0);
		resources.add(new ResourceBackupResource(testResource, "test.xml"));
		Backup result = bs.performBackup(resources);
		assertNotNull(result);
		assertNotNull(result.getDate());
		assertTrue(!now.after(result.getDate()));
		assertNotNull(result.getKey());
		assertTrue(result.isComplete());

		// now let's verify we can get that file back out of the backup
		Collection<Backup> backups = bs.getAvailableBackups();
		assertNotNull(backups);
		assertEquals(1, backups.size());
		Backup b = backups.iterator().next();
		assertEquals(result.getKey(), b.getKey());
		assertEquals(result.getDate().getTime(), b.getDate().getTime());

		int count = 0;
		final BackupResourceIterable backupResources = bs.getBackupResources(b);
		try {
			for ( BackupResource r : backupResources ) {
				count++;
				assertEquals("test.xml", r.getBackupPath());
				Assert.assertArrayEquals(FileCopyUtils.copyToByteArray(testResource.getInputStream()),
						FileCopyUtils.copyToByteArray(r.getInputStream()));
			}
		} finally {
			backupResources.close();
		}
		assertEquals("Should only have one backup resource", 1, count);
	}

	@Test
	public void backupMultiple() throws IOException {
		final ClassPathResource testResource1 = new ClassPathResource("test-context.xml",
				AbstractNodeTransactionalTest.class);
		final ClassPathResource testResource2 = new ClassPathResource("test-file.txt",
				FileSystemBackupServiceTest.class);
		final BackupService bs = service;
		final List<BackupResource> resources = new ArrayList<BackupResource>(1);
		final Calendar now = new GregorianCalendar();
		now.set(Calendar.MILLISECOND, 0);
		resources.add(new ResourceBackupResource(testResource1, "test.xml"));
		resources.add(new ResourceBackupResource(testResource2, "test.txt"));

		Backup result = bs.performBackup(resources);
		assertNotNull(result);
		assertNotNull(result.getDate());
		assertTrue(!now.after(result.getDate()));
		assertNotNull(result.getKey());
		assertTrue(result.isComplete());

		// now let's verify we can get that file back out of the backup
		Collection<Backup> backups = bs.getAvailableBackups();
		assertNotNull(backups);
		assertEquals(1, backups.size());
		Backup b = backups.iterator().next();
		assertEquals(result.getKey(), b.getKey());
		assertEquals(result.getDate().getTime(), b.getDate().getTime());

		int count = 0;
		try (final BackupResourceIterable backupResources = bs.getBackupResources(b)) {
			for ( BackupResource r : bs.getBackupResources(b) ) {
				if ( count == 0 ) {
					assertEquals("test.xml", r.getBackupPath());
					Assert.assertArrayEquals(
							FileCopyUtils.copyToByteArray(testResource1.getInputStream()),
							FileCopyUtils.copyToByteArray(r.getInputStream()));
				} else if ( count == 1 ) {
					assertEquals("test.txt", r.getBackupPath());
					Assert.assertArrayEquals(
							FileCopyUtils.copyToByteArray(testResource2.getInputStream()),
							FileCopyUtils.copyToByteArray(r.getInputStream()));
				}
				count++;
			}
		}
		assertEquals("Should have 2 backup resources", 2, count);
	}

	@Test
	public void availableBackupsNoIdentityOrderedByDateDescending() throws IOException {
		// change the node ID to null (not associated yet, i.e. simulate a restore)
		// see NODE-123
		identityNodeId = null;

		File backupFromOtherNode = new File(service.getBackupDir(),
				"node-" + TEST_NODE_ID + "-backup-20170101T000000.zip");
		backupFromOtherNode.createNewFile();
		backupFromOtherNode.deleteOnExit();

		File backupFromThisNode = new File(service.getBackupDir(), "node-0-backup-20070101T000000.zip");
		backupFromThisNode.createNewFile();
		backupFromThisNode.deleteOnExit();

		File backupFromThisNode2 = new File(service.getBackupDir(), "node-0-backup-20070101T010000.zip");
		backupFromThisNode2.createNewFile();
		backupFromThisNode2.deleteOnExit();

		List<Backup> result = new ArrayList<Backup>(service.getAvailableBackups());

		assertThat("All backups returned, regardless of node ID", result, hasSize(3));
		assertThat("Backup keys in descending order",
				result.stream().map(Backup::getKey).toArray(String[]::new),
				is(arrayContaining("20170101T000000", "20070101T010000", "20070101T000000")));
	}

	@Test
	public void availableBackupsNoIdentityOrderedByDateDescendingThenNodeAscending() throws IOException {
		// change the node ID to null (not associated yet, i.e. simulate a restore)
		// see NODE-123
		identityNodeId = null;

		File backupFromOtherNode = new File(service.getBackupDir(),
				"node-" + TEST_NODE_ID + "-backup-20070101T000000.zip");
		backupFromOtherNode.createNewFile();
		backupFromOtherNode.deleteOnExit();

		File backupFromThisNode = new File(service.getBackupDir(), "node-0-backup-20070101T000000.zip");
		backupFromThisNode.createNewFile();
		backupFromThisNode.deleteOnExit();

		File backupFromThisNode2 = new File(service.getBackupDir(), "node-0-backup-20070101T010000.zip");
		backupFromThisNode2.createNewFile();
		backupFromThisNode2.deleteOnExit();

		List<Backup> result = new ArrayList<Backup>(service.getAvailableBackups());

		assertThat("All backups returned, regardless of node ID", result, hasSize(3));
		assertThat("Backup keys in descending order",
				result.stream().map(Backup::getKey).toArray(String[]::new),
				is(arrayContaining("20070101T010000", "20070101T000000", "20070101T000000")));
		assertThat("Backup node IDs in descending order",
				result.stream().map(Backup::getNodeId).toArray(Long[]::new),
				is(arrayContaining(0L, 0L, TEST_NODE_ID)));
	}
}
