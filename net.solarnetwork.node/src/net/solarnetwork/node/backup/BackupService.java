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
import java.util.Map;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * API for node backup.
 * 
 * @author matt
 * @version 1.1
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
	 * @return status info (never <em>null</em>)
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
	 * @return the available backups, never <em>null</em>
	 */
	Collection<Backup> getAvailableBackups();

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
	 * @return provider, or <em>null</em> if not supported
	 */
	SettingSpecifierProvider getSettingSpecifierProvider();

	/**
	 * Mark a specific backup to be restored in the future call to
	 * {@link #markedBackupForRestore()}.
	 * 
	 * @param backup
	 *        The backup to mark for restoration later, or <em>null</em> to
	 *        clear a previous marked backup.
	 * @param props
	 *        An optional map of properties to save with the mark.
	 * @return <em>true</em> on success
	 * @since 1.1
	 */
	boolean markBackupForRestore(Backup backup, Map<String, String> props);

	/**
	 * Get a backup previously set via
	 * {@link BackupService#markBackupForRestore(Backup)}.
	 * 
	 * @param props
	 *        An optional map in which any properties passed to
	 *        {@link #markBackupForRestore(Backup, Map)} should be populated
	 *        into
	 * @return The marked backup, or <em>null</em> if none exists.
	 * @since 1.1
	 */
	Backup markedBackupForRestore(Map<String, String> props);

}
