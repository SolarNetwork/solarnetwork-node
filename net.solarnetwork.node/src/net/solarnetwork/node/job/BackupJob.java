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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.service.OptionalService;

/**
 * Scheduled backup job using {@link BackupManager}.
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class BackupJob extends AbstractJob {

	private OptionalService<BackupManager> backupManagerTracker;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		BackupManager manager = OptionalService.service(backupManagerTracker);
		if ( manager == null ) {
			log.debug("No backup manager available, cannot perform backup");
			return;
		}

		manager.createBackup();
	}
}
