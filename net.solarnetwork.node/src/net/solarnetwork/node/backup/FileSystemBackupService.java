/* ==================================================================
 * FileSystemBackupService.java - Mar 27, 2013 11:38:08 AM
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

package net.solarnetwork.node.backup;

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.node.backup.BackupStatus.Configured;
import static net.solarnetwork.node.backup.BackupStatus.Error;
import static net.solarnetwork.node.backup.BackupStatus.RunningBackup;
import static net.solarnetwork.node.backup.BackupStatus.Unconfigured;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicSliderSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * {@link BackupService} implementation that copies files to another location in
 * the file system.
 *
 * @author matt
 * @version 2.4
 */
public class FileSystemBackupService extends BackupServiceSupport implements SettingSpecifierProvider {

	/** The value returned by {@link #getKey()}. */
	public static final String KEY = FileSystemBackupService.class.getName();

	/**
	 * A format for turning a {@link Backup#getKey()} value into a zip file
	 * name.
	 */
	public static final String ARCHIVE_KEY_NAME_FORMAT = "node-%2$d-backup-%1$s.zip";

	private static final String ARCHIVE_NAME_FORMAT = "node-%2$d-backup-%1$tY%1$tm%1$tdT%1$tH%1$tM%1$tS.zip";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private MessageSource messageSource;
	private File backupDir = defaultBackuprDir();
	private OptionalService<IdentityService> identityService;
	private int additionalBackupCount = 1;
	private BackupStatus status = Configured;

	/**
	 * Default constructor.
	 */
	public FileSystemBackupService() {
		super();
	}

	@Override
	public String getSettingUid() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "File System Backup Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set the message source.
	 *
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		FileSystemBackupService defaults = new FileSystemBackupService();
		results.add(new BasicTitleSettingSpecifier("status", getStatus().toString(), true));
		results.add(new BasicTextFieldSettingSpecifier("backupDir",
				defaults.getBackupDir().getAbsolutePath()));
		results.add(new BasicSliderSettingSpecifier("additionalBackupCount",
				(double) defaults.getAdditionalBackupCount(), 0.0, 10.0, 1.0));
		return results;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public BackupServiceInfo getInfo() {
		return new SimpleBackupServiceInfo(null, getStatus());
	}

	private String getArchiveKey(String archiveName) {
		Matcher m = NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(archiveName);
		if ( m.find() ) {
			return m.group(2);
		}
		return archiveName;
	}

	@Override
	public Backup backupForKey(String key) {
		final File archiveFile = getArchiveFileForBackup(key);
		if ( !archiveFile.canRead() ) {
			return null;
		}
		return createBackupForFile(archiveFile);
	}

	@Override
	public Backup performBackup(final Iterable<BackupResource> resources) {
		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		return performBackupInternal(resources, now, null);
	}

	private Backup performBackupInternal(final Iterable<BackupResource> resources, final Instant now,
			Map<String, String> props) {
		if ( resources == null ) {
			return null;
		}
		final Iterator<BackupResource> itr = resources.iterator();
		if ( !itr.hasNext() ) {
			log.debug("No resources provided, nothing to backup");
			return null;
		}
		BackupStatus status = setStatusIf(RunningBackup, Configured);
		if ( status != RunningBackup ) {
			// try to reset from error
			status = setStatusIf(RunningBackup, Error);
			if ( status != RunningBackup ) {
				return null;
			}
		}
		if ( !backupDir.exists() ) {
			backupDir.mkdirs();
		}
		final Long nodeId = nodeIdForArchiveFileName(props);
		final String archiveName = String.format(ARCHIVE_NAME_FORMAT, now.atOffset(UTC), nodeId);
		final File archiveFile = new File(backupDir, archiveName);
		final String archiveKey = getArchiveKey(archiveName);
		log.info("Starting backup to archive {}", archiveName);
		log.trace("Backup archive: {}", archiveFile.getAbsolutePath());
		Backup backup = null;
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archiveFile)));
			while ( itr.hasNext() ) {
				BackupResource r = itr.next();
				log.debug("Backup up resource {} to archive {}", r.getBackupPath(), archiveName);
				zos.putNextEntry(new ZipEntry(r.getBackupPath()));
				FileCopyUtils.copy(r.getInputStream(), new FilterOutputStream(zos) {

					@Override
					public void close() throws IOException {
						// FileCopyUtils closes the stream, which we don't want
					}

				});
			}
			zos.flush();
			zos.finish();
			log.info("Backup complete to archive {}", archiveName);
			backup = new SimpleBackup(nodeId, Date.from(now), archiveKey, archiveFile.length(), true);

			// clean out older backups
			File[] backupFiles = getAvailableBackupFiles();
			if ( backupFiles != null && backupFiles.length > additionalBackupCount + 1 ) {
				// delete older files
				for ( int i = additionalBackupCount + 1; i < backupFiles.length; i++ ) {
					log.info("Deleting old backup archive {}", backupFiles[i].getName());
					if ( !backupFiles[i].delete() ) {
						log.warn("Unable to delete backup archive {}", backupFiles[i].getAbsolutePath());
					}
				}
			}
		} catch ( IOException e ) {
			log.error("IO error creating backup: {}", e.getMessage());
			setStatus(Error);
		} catch ( RuntimeException e ) {
			log.error("Error creating backup: {}", e.getMessage());
			setStatus(Error);
		} finally {
			if ( zos != null ) {
				try {
					zos.close();
				} catch ( IOException e ) {
					// ignore this
				}
			}
			status = setStatusIf(Configured, RunningBackup);
			if ( status != Configured ) {
				// clean up if we encountered an error
				if ( archiveFile.exists() ) {
					archiveFile.delete();
				}
			}
		}
		return backup;
	}

	private final Long nodeIdForArchiveFileName(Map<String, String> props) {
		Long nodeId = backupNodeIdFromProps(null, props);
		if ( nodeId == 0L ) {
			nodeId = nodeIdForArchiveFileName();
		}
		return nodeId;
	}

	private final Long nodeIdForArchiveFileName() {
		IdentityService service = (identityService != null ? identityService.service() : null);
		final Long nodeId = (service != null ? service.getNodeId() : null);
		return (nodeId != null ? nodeId : 0L);
	}

	private File getArchiveFileForBackup(final String backupKey) {
		final Long nodeId = nodeIdForArchiveFileName();
		if ( nodeId.intValue() == 0 ) {
			// hmm, might be restoring from corrupted db; look for file with matching key only
			File[] matches = backupDir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.contains(backupKey);
				}
			});
			if ( matches != null && matches.length > 0 ) {
				// take first available
				return matches[0];
			}
			// not found
			return null;
		} else {
			return new File(backupDir, String.format(ARCHIVE_KEY_NAME_FORMAT, backupKey, nodeId));
		}
	}

	@Override
	public BackupResourceIterable getBackupResources(Backup backup) {
		final File archiveFile = getArchiveFileForBackup(backup.getKey());
		if ( !(archiveFile.isFile() && archiveFile.canRead()) ) {
			log.warn("No backup archive exists for key [{}]", backup.getKey());
			Collection<BackupResource> col = Collections.emptyList();
			return new CollectionBackupResourceIterable(col);
		}
		try {
			final ZipFile zf = new ZipFile(archiveFile);
			Enumeration<? extends ZipEntry> entries = zf.entries();
			List<BackupResource> result = new ArrayList<BackupResource>(20);
			while ( entries.hasMoreElements() ) {
				result.add(new ZipEntryBackupResource(zf, entries.nextElement()));
			}
			return new CollectionBackupResourceIterable(result) {

				@Override
				public void close() throws IOException {
					zf.close();
				}

			};
		} catch ( IOException e ) {
			log.error("Error extracting backup archive entries: {}", e.getMessage());
		}
		Collection<BackupResource> col = Collections.emptyList();
		return new CollectionBackupResourceIterable(col);
	}

	@Override
	public Backup importBackup(Date date, BackupResourceIterable resources, Map<String, String> props) {
		final Date backupDate = backupDateFromProps(date, props);
		final Instant ts = backupDate.toInstant().truncatedTo(ChronoUnit.SECONDS);
		return performBackupInternal(resources, ts, props);
	}

	/**
	 * Delete any existing backups.
	 */
	public void removeAllBackups() {
		File[] archives = backupDir.listFiles(new ArchiveFilter());
		if ( archives == null ) {
			return;
		}
		for ( File archive : archives ) {
			log.debug("Deleting backup archive {}", archive.getName());
			if ( !archive.delete() ) {
				log.warn("Unable to delete archive file {}", archive.getAbsolutePath());
			}
		}
	}

	/**
	 * Get all available backup files, ordered in descending backup order
	 * (newest to oldest).
	 *
	 * @return ordered array of backup files, or {@literal null} if directory
	 *         does not exist
	 */
	private File[] getAvailableBackupFiles() {
		File[] archives = backupDir.listFiles(new ArchiveFilter());
		if ( archives != null ) {
			Arrays.sort(archives, new Comparator<File>() {

				@Override
				public int compare(File o1, File o2) {
					Matcher m1 = NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(o1.getName());
					Matcher m2 = NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(o2.getName());
					if ( m1.find() && m2.find() ) {
						// sort in reverse date order, ascending node order
						String s1 = m1.group(2);
						String s2 = m2.group(2);
						int result = s2.compareTo(s1);
						if ( result == 0 ) {
							long n1 = Long.parseLong(m1.group(1));
							long n2 = Long.parseLong(m2.group(1));
							result = Long.compare(n1, n2);
						}
						return result;
					}
					// sort in reverse order, so most recent backup first
					return o2.getName().compareTo(o1.getName());
				}
			});
		}
		return archives;
	}

	private SimpleBackup createBackupForFile(File f) {
		Matcher m = NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(f.getName());
		if ( m.find() ) {
			try {
				Instant d = BACKUP_KEY_DATE_FORMATTER.parse(m.group(2), Instant::from);
				Long nodeId = 0L;
				try {
					nodeId = Long.valueOf(m.group(1));
				} catch ( NumberFormatException e ) {
					// ignore this
				}
				return new SimpleBackup(nodeId, Date.from(d), m.group(2), f.length(), true);
			} catch ( DateTimeParseException e ) {
				log.error("Error parsing date from archive " + f.getName() + ": " + e.getMessage());
			}
		}
		return null;
	}

	@Override
	public Collection<Backup> getAvailableBackups() {
		File[] archives = getAvailableBackupFiles();
		if ( archives == null ) {
			return Collections.emptyList();
		}
		List<Backup> result = new ArrayList<Backup>(archives.length);
		for ( File f : archives ) {
			SimpleBackup b = createBackupForFile(f);
			if ( b != null ) {
				result.add(b);
			}
		}
		return result;
	}

	@Override
	public SettingSpecifierProvider getSettingSpecifierProvider() {
		return this;
	}

	@Override
	public SettingSpecifierProvider getSettingSpecifierProviderForRestore() {
		return new SettingSpecifierProvider() {

			@Override
			public String getSettingUid() {
				return FileSystemBackupService.this.getSettingUid();
			}

			@Override
			public List<SettingSpecifier> getSettingSpecifiers() {
				List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
				results.add(new BasicTextFieldSettingSpecifier("backupDir",
						defaultBackuprDir().getAbsolutePath()));
				return results;
			}

			@Override
			public MessageSource getMessageSource() {
				return FileSystemBackupService.this.getMessageSource();
			}

			@Override
			public String getDisplayName() {
				return FileSystemBackupService.this.getDisplayName();
			}
		};
	}

	private static class ArchiveFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			Matcher m = NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(name);
			return (m.find() && name.endsWith(".zip"));
		}

	}

	private BackupStatus getStatus() {
		synchronized ( status ) {
			if ( backupDir == null ) {
				return Unconfigured;
			}
			if ( !backupDir.exists() ) {
				if ( !backupDir.mkdirs() ) {
					log.warn("Could not create backup dir {}", backupDir.getAbsolutePath());
					return Unconfigured;
				}
			}
			if ( !backupDir.isDirectory() ) {
				log.error("Configured backup location is not a directory: {}",
						backupDir.getAbsolutePath());
				return Unconfigured;
			}
			return status;
		}
	}

	private void setStatus(BackupStatus newStatus) {
		synchronized ( status ) {
			status = newStatus;
		}
	}

	private BackupStatus setStatusIf(BackupStatus newStatus, BackupStatus ifStatus) {
		synchronized ( status ) {
			if ( status == ifStatus ) {
				status = newStatus;
			}
			return status;
		}
	}

	/**
	 * Get the directory to backup to.
	 *
	 * @return the backup directory
	 */
	public File getBackupDir() {
		return backupDir;
	}

	/**
	 * Set the directory to backup to.
	 *
	 * @param backupDir
	 *        the directory to use
	 */
	public void setBackupDir(File backupDir) {
		this.backupDir = backupDir;
	}

	/**
	 * Get the number of additional backups to maintain.
	 *
	 * @return the additional backup count
	 */
	public int getAdditionalBackupCount() {
		return additionalBackupCount;
	}

	/**
	 * Set the number of additional backups to maintain.
	 *
	 * <p>
	 * If greater than zero, then this service will maintain this many copies of
	 * past backups.
	 * </p>
	 *
	 * @param additionalBackupCount
	 *        the additional backup count to use
	 */
	public void setAdditionalBackupCount(int additionalBackupCount) {
		this.additionalBackupCount = additionalBackupCount;
	}

	/**
	 * Get the identity service.
	 *
	 * @return the service
	 */
	public OptionalService<IdentityService> getIdentityService() {
		return identityService;
	}

	/**
	 * Set the identity service.
	 *
	 * @param identityService
	 *        the service to use
	 */
	public void setIdentityService(OptionalService<IdentityService> identityService) {
		this.identityService = identityService;
	}

}
