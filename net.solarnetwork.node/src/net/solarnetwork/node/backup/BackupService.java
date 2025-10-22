/* ==================================================================
 * BackupService.java - Mar 27, 2013 6:59:13 AM
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for node backup.
 *
 * @author matt
 * @version 2.1
 */
public interface BackupService {

	/**
	 * Get a unique key for this service.
	 *
	 * <p>
	 * This key should be unique among all possible implementations of
	 * BackupService.
	 * </p>
	 *
	 * @return a unique key
	 */
	String getKey();

	/**
	 * Get general status information about the service.
	 *
	 * @return status info (never {@literal null})
	 */
	BackupServiceInfo getInfo();

	/**
	 * Execute a backup now.
	 *
	 * <p>
	 * This method may block until the backup completes.
	 * </p>
	 *
	 * @param resources
	 *        the resources to include in the backup
	 * @return backup instance
	 */
	Backup performBackup(Iterable<BackupResource> resources);

	/**
	 * Get a backup for a given backup key.
	 *
	 * @param key
	 *        the key
	 * @return the backup
	 */
	Backup backupForKey(String key);

	/**
	 * Get a collection of Backup instances known by this service.
	 *
	 * <p>
	 * These should be ideally ordered in newest to oldest order.
	 * </p>
	 *
	 * @return the available backups, never {@literal null}
	 */
	Collection<Backup> getAvailableBackups();

	/**
	 * Find a filtered list of backups.
	 *
	 * @param filter
	 *        the search criteria, or {@code null} to find all available backups
	 * @return the filtered backups, never {@code null}
	 * @since 2.1
	 */
	default FilterResults<Backup, String> findBackups(final BackupFilter filter) {
		final Collection<Backup> allBackups = getAvailableBackups();
		if ( allBackups == null ) {
			return new BasicFilterResults<>(List.of());
		} else if ( filter == null || !filter.hasAnyCriteria() || allBackups.isEmpty() ) {
			return new BasicFilterResults<>(allBackups);
		}
		final List<Backup> allMatching = allBackups.stream().filter(b -> {
			if ( filter.hasNodeCriteria() && !filter.getNodeId().equals(b.getNodeId()) ) {
				return false;
			}
			return true;
		}).toList();
		if ( filter.getOffset() != null || filter.getMax() != null ) {
			int from = filter.getOffset() != null ? filter.getOffset().intValue() : 0;
			int to = filter.getMax() != null ? Math.min(from + filter.getMax(), allMatching.size())
					: allMatching.size();
			List<Backup> filtered = List.of();
			if ( from < allMatching.size() ) {
				filtered = allMatching.subList(from, to);
			}
			return new BasicFilterResults<>(filtered, (long) allMatching.size(), from, filtered.size());
		}
		return new BasicFilterResults<>(allMatching);
	}

	/**
	 * Get the resources for a specific backup.
	 *
	 * @param backup
	 *        the backup to get the resources for
	 * @return an {@link Iterable} for the backup resources
	 */
	BackupResourceIterable getBackupResources(Backup backup);

	/**
	 * Get a {@link SettingSpecifierProvider} for this service.
	 *
	 * @return provider, or {@literal null} if not supported
	 */
	SettingSpecifierProvider getSettingSpecifierProvider();

	/**
	 * Mark a specific backup to be restored in the future call to
	 * {@link #markedBackupForRestore(Map)}.
	 *
	 * @param backup
	 *        The backup to mark for restoration later, or {@literal null} to
	 *        clear a previous marked backup.
	 * @param props
	 *        An optional map of properties to save with the mark.
	 * @return {@literal true} on success
	 * @since 1.1
	 */
	boolean markBackupForRestore(Backup backup, Map<String, String> props);

	/**
	 * Get a backup previously set via
	 * {@link BackupService#markBackupForRestore(Backup, Map)}.
	 *
	 * @param props
	 *        An optional map in which any properties passed to
	 *        {@link #markBackupForRestore(Backup, Map)} should be populated
	 *        into
	 * @return The marked backup, or {@literal null} if none exists.
	 * @since 1.1
	 */
	Backup markedBackupForRestore(Map<String, String> props);

	/**
	 * Import a set of backup resources as a new {@code Backup}.
	 *
	 * @param date
	 *        The backup date, or {@literal null} if not known.
	 * @param resources
	 *        The resources to include in the backup.
	 * @param props
	 *        An optional map of properties to pass to the import process. The
	 *        {@link BackupManager#BACKUP_KEY} property can be used to provide a
	 *        proposed backup key.
	 * @return A backup instance for the imported resources.
	 * @since 1.2
	 */
	Backup importBackup(Date date, BackupResourceIterable resources, Map<String, String> props);

	/**
	 * Get a {@link SettingSpecifierProvider} for this service, when restoring
	 * from an existing backup.
	 *
	 * @return provider, or {@literal null} if not supported
	 * @since 1.3
	 */
	SettingSpecifierProvider getSettingSpecifierProviderForRestore();

}
