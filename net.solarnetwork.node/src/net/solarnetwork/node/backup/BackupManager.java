/* ==================================================================
 * BackupManager.java - Mar 27, 2013 9:10:39 AM
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Manager API for node-level backups.
 * 
 * @author matt
 * @version 1.2
 */
public interface BackupManager extends SettingSpecifierProvider {

	/**
	 * A property key for a comma-delimited list of
	 * {@link BackupResourceProvider#getKey()} values to limit the backup
	 * operation to. If not specified, all providers are included.
	 * 
	 * @since 1.1
	 */
	String RESOURCE_PROVIDER_FILTER = "ResourceProviderFilter";

	/**
	 * A property key a {@link Backup} {@code key} value.
	 * 
	 * @since 1.2
	 */
	String BACKUP_KEY = "BackupKey";

	/**
	 * Get the active {@link BackupService}.
	 * 
	 * @return the BackupService, or <em>null</em> if none configured
	 */
	BackupService activeBackupService();

	/**
	 * Get a {@link Iterator} of {@link BackupResource} needing to be backed up.
	 * 
	 * @return
	 */
	Iterable<BackupResource> resourcesForBackup();

	/**
	 * Create a new Backup, using the active backup service.
	 * 
	 * @return the backup, or <em>null</em> if none could be created
	 */
	Backup createBackup();

	/**
	 * Create a new Backup, using the active backup service.
	 * 
	 * @param props
	 *        An optional set of properties to customize the backup with.
	 * @return the backup, or <em>null</em> if none could be created
	 * @since 1.1
	 */
	Backup createBackup(Map<String, String> props);

	/**
	 * Create a new Backup, using the active backup service, in the background.
	 * This method will immediately return a Future where you can track the
	 * status of the background backup, if desired.
	 * 
	 * @return the backup, or <em>null</em> if none could be created
	 */
	Future<Backup> createAsynchronousBackup();

	/**
	 * Create a new Backup, using the active backup service, in the background.
	 * This method will immediately return a Future where you can track the
	 * status of the background backup, if desired.
	 * 
	 * @param props
	 *        An optional set of properties to customize the backup with.
	 * @return the backup, or <em>null</em> if none could be created
	 * @since 1.1
	 */
	Future<Backup> createAsynchronousBackup(Map<String, String> props);

	/**
	 * Restore all resources from a given backup.
	 * 
	 * @param backup
	 *        the backup to restore
	 */
	void restoreBackup(Backup backup);

	/**
	 * Restore all resources from a given backup.
	 * 
	 * @param backup
	 *        the backup to restore
	 * @param props
	 *        An optional set of properties to customize the backup with.
	 * @since 1.1
	 */
	void restoreBackup(Backup backup, Map<String, String> props);

	/**
	 * Export a backup zip archive.
	 * 
	 * @param backupKey
	 *        the backup to export
	 * @param out
	 *        the output stream to export to
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void exportBackupArchive(String backupKey, OutputStream out) throws IOException;

	/**
	 * Export a backup zip archive.
	 * 
	 * @param backupKey
	 *        the backup to export
	 * @param out
	 *        the output stream to export to
	 * @param props
	 *        An optional set of properties to customize the backup with.
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	public void exportBackupArchive(String backupKey, OutputStream out, Map<String, String> props)
			throws IOException;

	/**
	 * Import a backup archive into the active backup service.
	 * 
	 * This method can import an archive exported via
	 * {@link #exportBackupArchive(String, OutputStream)}. Once imported, the
	 * backup will appear as a new backup in the active backup service.
	 * 
	 * @param archive
	 *        the archive input stream to import
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public Future<Backup> importBackupArchive(InputStream archive) throws IOException;

	/**
	 * Import a backup archive with properties.
	 * 
	 * This method can import an archive exported via
	 * {@link #exportBackupArchive(String, OutputStream)}. The
	 * {@link #RESOURCE_PROVIDER_FILTER} property can be used to filter which
	 * provider resources are included in the imported backup. The
	 * {@link #BACKUP_KEY} can be used to provide a hint of the original backup
	 * key (and possibly date). Once imported, the backup will appear as a new
	 * backup in the active backup service.
	 * 
	 * @param archive
	 *        the archive input stream to import
	 * @param props
	 *        An optional set of properties to customize the backup with.
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	public Future<Backup> importBackupArchive(InputStream archive, Map<String, String> props)
			throws IOException;
}
