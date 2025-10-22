/* ==================================================================
 * SettingBackup.java - Nov 6, 2012 9:55:37 AM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings;

import java.io.Serial;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import net.solarnetwork.node.domain.StringDateKey;

/**
 * A backup domain object.
 *
 * @author matt
 * @deprecated use {@link StringDateKey}
 * @version 1.1
 */
@Deprecated
public class SettingsBackup extends StringDateKey {

	@Serial
	private static final long serialVersionUID = 1605119146770492234L;

	/**
	 * Construct with values.
	 *
	 * @param backupKey
	 *        the backup key
	 * @param backupDate
	 *        the backup date
	 */
	public SettingsBackup(String backupKey, Date backupDate) {
		super(backupKey, backupDate != null ? backupDate.toInstant() : null);
	}

	/**
	 * Get a standardized representation of the backup date.
	 *
	 * @return date string
	 */
	public String getStandardDateString() {
		Date date = getBackupDate();
		if ( date == null ) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
		return sdf.format(date);
	}

	/**
	 * Get the backup key.
	 *
	 * @return the backup key
	 */
	public String getBackupKey() {
		return getKey();
	}

	/**
	 * Get the backup date.
	 *
	 * @return the backup date
	 */
	public Date getBackupDate() {
		Instant ts = getTimestamp();
		return ts != null ? Date.from(ts) : null;
	}
}
