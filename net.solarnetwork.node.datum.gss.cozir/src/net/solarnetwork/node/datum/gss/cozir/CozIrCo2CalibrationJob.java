/* ==================================================================
 * CozIrCo2CalibrationJob.java - 28/08/2020 4:41:40 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.gss.cozir;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to calibrate the CO2 sensor in a CozIR device.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class CozIrCo2CalibrationJob extends AbstractJob {

	private CozIrService service;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		try {
			service.calibrateAsCo2FreshAirLevel();
		} catch ( Exception e ) {
			log.error("Error calibrating CozIR CO2 sensor: {}", e.getMessage(), e);
		}
	}

	/**
	 * Get the service.
	 * 
	 * @return the service
	 */
	public CozIrService getService() {
		return service;
	}

	/**
	 * Set the service.
	 * 
	 * @param service
	 *        the service to set
	 */
	public void setService(CozIrService service) {
		this.service = service;
	}

}
