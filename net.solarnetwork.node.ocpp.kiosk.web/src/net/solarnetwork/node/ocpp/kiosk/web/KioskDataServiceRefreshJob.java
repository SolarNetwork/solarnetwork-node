/* ==================================================================
 * KioskDataServiceRefreshJob.java - 23/10/2016 7:06:40 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.kiosk.web;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to run to refresh the kiosk data model.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class KioskDataServiceRefreshJob extends AbstractJob {

	private KioskDataService dataService;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		assert dataService != null;
		dataService.refreshKioskData();
	}

	public void setDataService(KioskDataService dataService) {
		this.dataService = dataService;
	}

}
