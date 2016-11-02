/* ==================================================================
 * DefaultBackupManagerTest.java - Mar 28, 2013 12:42:02 PM
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceInfo;
import net.solarnetwork.node.backup.BackupResourceProvider;
import net.solarnetwork.node.backup.BackupResourceProviderInfo;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.DefaultBackupManager;
import net.solarnetwork.node.backup.FileSystemBackupService;
import net.solarnetwork.node.backup.ResourceBackupResource;
import net.solarnetwork.node.backup.SimpleBackupResourceInfo;
import net.solarnetwork.node.backup.SimpleBackupResourceProviderInfo;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test case for the {@link DefaultBackupManager} class.
 * 
 * @author matt
 * @version 1.2
 */
public class DefaultBackupManagerTest {

	private static final String TEST_FILE_TXT = "test-file.txt";

	private static final Logger log = LoggerFactory.getLogger(DefaultBackupManagerTest.class);

	private static final File RESTORE_DIR = new File(System.getProperty("java.io.tmpdir"), "restore");

	private DefaultBackupManager manager;
	private FileSystemBackupService service;
	private Backup backup;
	private File backupArchiveFile;

	@Before
	public void setup() {
		service = new FileSystemBackupService();
		service.setBackupDir(new File(System.getProperty("java.io.tmpdir"), "backup"));
		service.setAdditionalBackupCount(0);
		service.removeAllBackups();

		manager = new DefaultBackupManager();
		manager.setBackupServiceTracker(new StaticOptionalService<BackupService>(service));
		List<BackupService> services = new ArrayList<BackupService>();
		services.add(service);
		manager.setBackupServices(services);
	}

	@Test
	public void createBackup() throws IOException {
		List<BackupResourceProvider> providers = new ArrayList<BackupResourceProvider>();
		List<BackupResource> resources = new ArrayList<BackupResource>();
		ClassPathResource txtResource = new ClassPathResource(TEST_FILE_TXT,
				DefaultBackupManagerTest.class);
		resources.add(new ResourceBackupResource(txtResource, TEST_FILE_TXT));
		providers.add(new StaticBackupResourceProvider(resources, RESTORE_DIR));
		manager.setResourceProviders(providers);
		final Backup backup = manager.createBackup();
		assertNotNull(backup);

		final File archiveFile = new File(service.getBackupDir(),
				String.format(FileSystemBackupService.ARCHIVE_KEY_NAME_FORMAT, backup.getKey(), 0L));
		assertTrue(archiveFile.canRead());
		ZipFile zipFile = new ZipFile(archiveFile);
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			int entryCount;
			for ( entryCount = 0; entries.hasMoreElements(); entryCount++ ) {
				ZipEntry entry = entries.nextElement();
				assertEquals("The zip entry should be prefixed by the BackupResourceProvider key",
						DefaultBackupManagerTest.class.getName() + '/' + TEST_FILE_TXT, entry.getName());
			}
			assertEquals(1, entryCount);
			this.backup = backup;
			this.backupArchiveFile = archiveFile;
		} finally {
			zipFile.close();
		}
	}

	@Test
	public void restoreBackup() throws IOException {
		createBackup();
		manager.restoreBackup(this.backup);
		final File restoredFile = new File(RESTORE_DIR, TEST_FILE_TXT);
		assertTrue("Restored file should exist", restoredFile.canRead());
		final byte[] restoredData = FileCopyUtils.copyToByteArray(restoredFile);
		assertArrayEquals(FileCopyUtils.copyToByteArray(
				new ClassPathResource(TEST_FILE_TXT, DefaultBackupManagerTest.class).getInputStream()),
				restoredData);
	}

	@Test
	public void importBackup() throws Exception {
		createBackup();
		Map<String, String> props = new HashMap<String, String>(2);
		props.put(BackupManager.BACKUP_KEY, backupArchiveFile.getName());
		File importFile = new File(System.getProperty("java.io.tmpdir"), "backup-import.tmp");
		backupArchiveFile.renameTo(importFile);
		importFile.deleteOnExit();
		Future<Backup> backupFuture = manager.importBackupArchive(new FileInputStream(importFile),
				props);
		assertNotNull("Backup future", backupFuture);
		Backup backup = backupFuture.get(10, TimeUnit.MINUTES);
		assertNotNull("Backup", backup);

		final File archiveFile = new File(service.getBackupDir(),
				String.format(FileSystemBackupService.ARCHIVE_KEY_NAME_FORMAT, backup.getKey(), 0L));
		assertTrue(archiveFile.canRead());
		assertEquals("Imported backup file name", props.get(BackupManager.BACKUP_KEY),
				archiveFile.getName());
		ZipFile zipFile = new ZipFile(archiveFile);
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			int entryCount;
			for ( entryCount = 0; entries.hasMoreElements(); entryCount++ ) {
				ZipEntry entry = entries.nextElement();
				assertEquals("The zip entry should be prefixed by the BackupResourceProvider key",
						DefaultBackupManagerTest.class.getName() + '/' + TEST_FILE_TXT, entry.getName());
			}
			assertEquals(1, entryCount);
			this.backup = backup;
			this.backupArchiveFile = archiveFile;
		} finally {
			zipFile.close();
		}

	}

	private static class StaticBackupResourceProvider implements BackupResourceProvider {

		private final File restoreDir;
		private final Collection<BackupResource> resources;

		private StaticBackupResourceProvider(Collection<BackupResource> resources, File restoreDir) {
			super();
			this.resources = resources;
			this.restoreDir = restoreDir;
		}

		@Override
		public String getKey() {
			return DefaultBackupManagerTest.class.getName();
		}

		@Override
		public Iterable<BackupResource> getBackupResources() {
			return resources;
		}

		@Override
		public boolean restoreBackupResource(BackupResource resource) {
			if ( !restoreDir.isDirectory() ) {
				restoreDir.mkdirs();
			}
			File out = new File(restoreDir, resource.getBackupPath());
			try {
				FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(out));
				return true;
			} catch ( IOException e ) {
				log.error("Error restoring resource {}", resource.getBackupPath(), e);
			}
			return false;
		}

		@Override
		public BackupResourceProviderInfo providerInfo(Locale locale) {
			// TODO Auto-generated method stub
			return new SimpleBackupResourceProviderInfo(getKey(), "Static Provider", null);
		}

		@Override
		public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
			return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(),
					null);
		}

	}
}
