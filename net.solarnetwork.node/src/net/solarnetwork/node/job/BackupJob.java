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

import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceInfo;
import net.solarnetwork.node.backup.BackupStatus;
import net.solarnetwork.util.OptionalService;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Scheduled backup job using {@link BackupManager}.
 * 
 * @author matt
 * @version 1.0
 */
public class BackupJob extends AbstractJob implements StatefulJob {

	private OptionalService<BackupManager> backupManagerTracker;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			log.debug("No backup manager available, cannot perform backup");
			return;
		}

		final BackupService service = manager.activeBackupService();
		if ( service == null ) {
			log.debug("No active backup service available, cannot perform backup");
			return;
		}
		final BackupServiceInfo info = service.getInfo();
		final BackupStatus status = info.getStatus();
		if ( status != BackupStatus.Configured ) {
			log.info("BackupService {} is in the {} state; cannot perform backup", service.getKey(),
					status);
			return;
		}

		log.info("Initiating backup to service {}", service.getKey());
		final Backup backup = service.performBackup(manager.resourcesForBackup());
		log.info("Backup {} {} with service {}", backup.getKey(), (backup.isComplete() ? "completed"
				: "initiated"), service.getKey());
	}

}
