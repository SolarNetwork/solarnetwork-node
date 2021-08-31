/* ==================================================================
 * FfmpegSnapshotJob.java - 31/08/2021 3:47:50 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.camera.ffmpeg;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to take a new snapshot.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class FfmpegSnapshotJob extends AbstractJob {

	private FfmpegService service;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		try {
			service.takeSnapshot();
		} catch ( Exception e ) {
			log.error("Error requesting snapshot from ffmpeg {}: {}", service.getDisplayName(),
					e.getMessage(), e);
		}
	}

	/**
	 * Set the FFmpeg service.
	 * 
	 * @param service
	 *        the service to use
	 */
	public void setService(FfmpegService service) {
		this.service = service;
	}

}
