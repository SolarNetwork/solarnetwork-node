/* ==================================================================
 * CsvBackupService.java - 19/10/2025 6:44:03 am
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

package net.solarnetwork.node.service;

import java.io.Reader;
import java.util.List;
import net.solarnetwork.node.domain.StringDateKey;

/**
 * Service API for CSV configuration backup management.
 *
 * @author matt
 * @version 1.0
 * @since 4.1
 */
public interface CsvConfigurableBackupService extends CsvConfigurableService {

	/**
	 * Create a backup of all local state, and return a backup object if the
	 * backup was performed.
	 *
	 * <p>
	 * A new backup need not be created if the configuration is unchanged. In
	 * that case, or if this method does not create a backup for any reason,
	 * this method will return {@code null}.
	 * </p>
	 *
	 * @return the backup ID, or {@code null} if no backup created
	 */
	StringDateKey backupCsvConfiguration();

	/**
	 * Get a collection of all known configuration backups.
	 *
	 * @return the backup IDs, never {@code null}, in natural order
	 */
	List<StringDateKey> getAvailableCsvConfigurationBackups();

	/**
	 * Get a {@link Reader} of CSV formatted configuration backup data for a
	 * given backup ID.
	 *
	 * @param backupId
	 *        the ID of the backup to get the CSV for
	 * @return the Reader, or {@code null} if the backup cannot be found
	 */
	Reader getCsvConfigurationBackup(StringDateKey backupId);

}
