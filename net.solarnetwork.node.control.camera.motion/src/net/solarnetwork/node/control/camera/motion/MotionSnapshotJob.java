/* ==================================================================
 * MotionSnapshotJob.java - 29/10/2019 11:59:53 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.camera.motion;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to create snapshots in motion.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MotionSnapshotJob extends AbstractJob {

	public static final int DEFAULT_CAMERA_ID = 1;

	private MotionService service;
	private int cameraId = DEFAULT_CAMERA_ID;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		try {
			service.takeSnapshot(cameraId);
		} catch ( Exception e ) {
			log.error("Error requesting snapshot from motion camera {} @ {}: {}", cameraId,
					service.getMotionBaseUrl(), e.getMessage(), e);
		}
	}

	/**
	 * Set the motion service.
	 * 
	 * @param service
	 *        the service to use
	 */
	public void setService(MotionService service) {
		this.service = service;
	}

	/**
	 * The camera ID.
	 * 
	 * @param cameraId
	 *        the camera ID
	 */
	public void setCameraId(int cameraId) {
		this.cameraId = cameraId;
	}

}
