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
import java.util.concurrent.Future;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Manager API for node-level backups.
 * 
 * @author matt
 * @version 1.0
 */
public interface BackupManager extends SettingSpecifierProvider {

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
	 * Create a new Backup, using the active backup service, in the background.
	 * This method will immediately return a Future where you can track the
	 * status of the background backup, if desired.
	 * 
	 * @return the backup, or <em>null</em> if none could be created
	 */
	Future<Backup> createAsynchronousBackup();

	/**
	 * Restore all resources from a given backup.
	 * 
	 * @param backup
	 *        the backup to restore
	 */
	void restoreBackup(Backup backup);

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
	 * Import a backup zip archive.
	 * 
	 * <p>
	 * This method can import an archive exported via
	 * {@link #exportBackupArchive(String, OutputStream)}.
	 * </p>
	 * 
	 * @param archive
	 *        the archive input stream to import
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void importBackupArchive(InputStream archive) throws IOException;
}
