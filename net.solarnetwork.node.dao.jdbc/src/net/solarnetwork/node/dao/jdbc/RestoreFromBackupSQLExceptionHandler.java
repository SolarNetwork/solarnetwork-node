/* ==================================================================
 * RestoreFromBackupSQLExceptionHandler.java - 24/07/2016 4:04:23 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import net.solarnetwork.dao.jdbc.SQLExceptionHandler;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.util.OptionalService;

/**
 * Recover from connection exceptions by restoring from backup.
 * 
 * @author matt
 * @version 1.1
 */
public class RestoreFromBackupSQLExceptionHandler implements SQLExceptionHandler {

	private final int minimumExceptionCount;
	private final BundleContext bundleContext;
	private int restoreDelaySeconds = 15;
	private String backupResourceProviderFilter;
	private List<Pattern> sqlStatePatterns;
	private OptionalService<IdentityService> identityService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicInteger exceptionCount = new AtomicInteger(0);
	private final AtomicBoolean restoreScheduled = new AtomicBoolean(false);

	/**
	 * Constructor.
	 * 
	 * @param bundleContext
	 *        The active bundle context.
	 * @param minimumExceptionCount
	 *        The minimum number of exceptions to witness before attempting to
	 *        restore from backup.
	 */
	public RestoreFromBackupSQLExceptionHandler(BundleContext bundleContext, int minimumExceptionCount) {
		super();
		this.bundleContext = bundleContext;
		this.minimumExceptionCount = minimumExceptionCount;
	}

	@Override
	public synchronized void handleGetConnectionException(SQLException e) {
		handleConnectionException(null, e);
	}

	@Override
	public void handleConnectionException(Connection conn, SQLException e) {
		SQLException root = e;
		while ( root.getNextException() != null ) {
			root = root.getNextException();
		}
		String state = root.getSQLState();
		if ( state == null ) {
			return;
		}
		List<Pattern> statePatterns = sqlStatePatterns;
		if ( statePatterns == null || statePatterns.isEmpty() ) {
			return;
		}
		for ( Pattern pat : statePatterns ) {
			if ( pat.matcher(state).matches() ) {
				log.error("Recovery triggering error {} on database connection: {}", state,
						e.getMessage());
				final int count = exceptionCount.incrementAndGet();
				if ( count < minimumExceptionCount ) {
					return;
				}
				scheduleRestoreFromBackup(count);
				return;
			}
		}
	}

	private File getDbDir() {
		String dbDir = System.getProperty("derby.system.home");
		if ( dbDir == null ) {
			return null;
		}
		// Should we get database name from JDBC connection properties? For now, this is hard-coded.
		return new File(dbDir, "solarnode");
	}

	private void cleanupExistingDatabase() {
		File f = getDbDir();
		if ( f == null ) {
			return;
		}
		if ( f.isDirectory() ) {
			log.warn("Deleting DB dir {}", f.getAbsolutePath());
			if ( FileSystemUtils.deleteRecursively(f) ) {
				log.warn("Deleted database directory " + f.getAbsolutePath());
			} else {
				try {
					java.nio.file.Files.delete(f.toPath());
				} catch ( IOException e ) {
					log.warn("Unable to delete database directory " + f.getAbsolutePath(), e);
				}
			}
		}
	}

	private void scheduleRestoreFromBackup(final int count) {
		if ( restoreScheduled.get() ) {
			return;
		}
		log.warn("Scheduling restore from backup in {} seconds due to database connection exception",
				restoreDelaySeconds);
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(restoreDelaySeconds));
				} catch ( InterruptedException e ) {
					// ignore and continue
				}
				BackupService backupService = getBackupService();
				if ( backupService != null ) {
					log.warn("Looking for backup to restore from {}", backupService);
					Backup backup = getBackupToRestore(backupService);
					if ( backup != null && restoreScheduled.compareAndSet(false, true) ) {
						Map<String, String> props = Collections.singletonMap(
								BackupManager.RESOURCE_PROVIDER_FILTER, backupResourceProviderFilter);
						log.warn("Discovered backup {} for scheduled restore using props {}",
								backup.getKey(), props);
						if ( backupService.markBackupForRestore(backup, props) ) {
							cleanupExistingDatabase();
							shutdown(backup);
						} else {
							log.warn(
									"BackupService {} failed to mark backup {} for restore); cannot schedule restore from backup",
									backupService, backup.getKey());
						}
					} else if ( backup == null ) {
						log.warn("No backup available to restore; cannot schedule restore from backup");
					}
				} else {
					log.warn(
							"No BackupService available for restore; will try scheduling restore again");
					scheduleRestoreFromBackup(count);
				}
			}

		});
		t.setContextClassLoader(Thread.currentThread().getContextClassLoader());
		t.start();
	}

	private BackupService getBackupService() {
		BackupManager mgr = backupManager();
		if ( mgr == null ) {
			log.debug("No BackupManager available to restore from");
			return null;
		}
		BackupService result = mgr.activeBackupService();
		return result;
	}

	private Backup getBackupToRestore(BackupService backupService) {
		if ( backupService == null ) {
			log.debug("No BackupService available to restore from");
			return null;
		}
		final IdentityService identService = (identityService != null ? identityService.service()
				: null);
		final Long nodeId = (identService != null ? identService.getNodeId() : null);
		Collection<Backup> backups = backupService.getAvailableBackups();
		if ( backups == null || backups.isEmpty() ) {
			log.debug("No Backup available to restore from");
			return null;
		}
		Backup backup = null;
		for ( Backup b : backups ) {
			// get the most recent available backup for this node
			if ( b.isComplete() && (backup == null || backup.getDate().before(b.getDate()))
					&& (nodeId == null || nodeId.equals(b.getNodeId())) ) {
				backup = b;
			}
		}
		return backup;
	}

	private BackupManager backupManager() {
		ServiceReference<BackupManager> mgrRef = bundleContext.getServiceReference(BackupManager.class);
		if ( mgrRef == null ) {
			return null;
		}
		return bundleContext.getService(mgrRef);
	}

	private void shutdown(Backup backup) {
		log.warn("Shutting down now to force restore from backup {}", backup.getKey());
		// graceful would be bundleContext.getBundle(0).stop();, but we don't need to wait for that here
		System.exit(0);
	}

	/**
	 * Set the number of seconds to delay the restore from backup. This is
	 * mainly to give the framework a time to boot up and provide the
	 * {@link BackupManager} service.
	 * 
	 * @param restoreDelaySeconds
	 *        The number of seconds to delay attempting the restore from backup.
	 */
	public void setRestoreDelaySeconds(int restoreDelaySeconds) {
		this.restoreDelaySeconds = restoreDelaySeconds;
	}

	/**
	 * Set a filter to pass as {@link BackupManager#RESOURCE_PROVIDER_FILTER} to
	 * limit the scope of the backup.
	 * 
	 * @param backupResourceProviderFilter
	 *        The filter to set.
	 */
	public void setBackupResourceProviderFilter(String backupResourceProviderFilter) {
		this.backupResourceProviderFilter = backupResourceProviderFilter;
	}

	/**
	 * Set a list of regular expressions that should trigger a restore from
	 * backup.
	 * 
	 * @param sqlStatePatterns
	 *        The regular expressions that should trigger a restore from backup.
	 */
	public void setSqlStatePatterns(List<Pattern> sqlStatePatterns) {
		this.sqlStatePatterns = sqlStatePatterns;
	}

	/**
	 * Set a comma-delimited list of regular expressions that should trigger a
	 * restore from backup.
	 * 
	 * @param regexes
	 *        A comma-delimited list of regular expressions that should trigger
	 *        a restore from backup.
	 * @see #setSqlStatePatterns(List)
	 */
	public void setSqlStateRegex(String regexes) {
		List<Pattern> pats = null;
		String[] list = StringUtils.delimitedListToStringArray(regexes, ",");
		if ( regexes != null && list.length > 0 ) {
			pats = new ArrayList<Pattern>();
			for ( String regex : list ) {
				pats.add(Pattern.compile(regex));
			}
		}
		setSqlStatePatterns(pats);
	}

	/**
	 * Set an {@link IdentityService} to know the current node identity to
	 * associate backups with.
	 * 
	 * @param identityService
	 *        the identity service to use
	 * @since 1.1
	 */
	public void setIdentityService(OptionalService<IdentityService> identityService) {
		this.identityService = identityService;
	}

}
