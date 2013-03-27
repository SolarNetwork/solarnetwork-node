/* ==================================================================
 * SimpleBackupServiceInfo.java - Mar 27, 2013 11:39:25 AM
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

import java.util.Date;

/**
 * Simple implementation of {@link BackupServiceInfo}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleBackupServiceInfo implements BackupServiceInfo {

	private final Date mostRecentBackupDate;
	private final BackupStatus status;

	/**
	 * Construct with values.
	 * 
	 * @param mostRecentBackupDate
	 *        the backup date
	 * @param status
	 *        the status
	 */
	public SimpleBackupServiceInfo(Date mostRecentBackupDate, BackupStatus status) {
		super();
		this.mostRecentBackupDate = mostRecentBackupDate;
		this.status = status;
	}

	@Override
	public Date getMostRecentBackupDate() {
		return mostRecentBackupDate;
	}

	@Override
	public BackupStatus getStatus() {
		return status;
	}

}
