/* ==================================================================
 * TablesMaintenanceJob.java - Jul 29, 2017 2:09:24 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to execute the {@link TablesMaintenanceService#processTables(String)}
 * method.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class TablesMaintenanceJob extends AbstractJob {

	private TablesMaintenanceService maintenanceService;

	public static final String JOB_KEY_LAST_TABLE_KEY = "TablesMaintenanceLastKey";

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( maintenanceService == null ) {
			return;
		}
		JobDataMap jd = jobContext.getJobDetail().getJobDataMap();
		String startAfterKey = (String) jd.get(JOB_KEY_LAST_TABLE_KEY);
		startAfterKey = maintenanceService.processTables(startAfterKey);
		if ( startAfterKey == null ) {
			jd.remove(JOB_KEY_LAST_TABLE_KEY);
		} else {
			jd.put(JOB_KEY_LAST_TABLE_KEY, startAfterKey);
		}
	}

	/**
	 * Set the service to use.
	 * 
	 * @param maintenanceService
	 *        The maintenance service.
	 */
	public void setMaintenanceService(TablesMaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

}
