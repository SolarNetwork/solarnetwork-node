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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job to create snapshots in motion.
 * 
 * @author matt
 * @version 2.0
 */
public class MotionSnapshotJob implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(MotionSnapshotJob.class);

	private final MotionService service;
	private final int cameraId;

	public MotionSnapshotJob(MotionService service, int cameraId) {
		super();
		this.service = requireNonNullArgument(service, "service");
		this.cameraId = cameraId;
	}

	@Override
	public void run() {
		try {
			service.takeSnapshot(cameraId);
		} catch ( Exception e ) {
			log.error("Error requesting snapshot from motion camera {} @ {}: {}", cameraId,
					service.getMotionBaseUrl(), e.getMessage(), e);
		}
	}

	/**
	 * Get the motion service.
	 * 
	 * @return the service
	 */
	public MotionService getService() {
		return service;
	}

	/**
	 * Get the camera ID.
	 * 
	 * @return the cameraId
	 */
	public int getCameraId() {
		return cameraId;
	}

}
