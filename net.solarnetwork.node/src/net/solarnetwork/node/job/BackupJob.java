/* ==================================================================
 * BackupJob.java - Mar 27, 2013 7:01:22 AM
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

package net.solarnetwork.node.job;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Scheduled backup job using {@link BackupManager}.
 * 
 * @author matt
 * @version 2.0
 */
public class BackupJob extends BaseIdentifiable implements JobService {

	private BackupManager backupManager;

	/**
	 * Constructor.
	 * 
	 * @param backupManager
	 *        the backup manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BackupJob(BackupManager backupManager) {
		super();
		this.backupManager = requireNonNullArgument(backupManager, "backupManager");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.job.BackupJob";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		backupManager.createBackup();
	}
}
