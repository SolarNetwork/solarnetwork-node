/* ==================================================================
 * ScheduledMotionSnapshot.java - 29/10/2019 12:15:35 pm
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

import org.quartz.Trigger;

/**
 * A scheduled motion snapshot.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class ScheduledMotionSnapshot {

	private final int cameraId;
	private final Trigger trigger;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the scheduled configuration
	 * @param trigger
	 *        the job trigger
	 */
	public ScheduledMotionSnapshot(int cameraId, Trigger trigger) {
		super();
		this.cameraId = cameraId;
		this.trigger = trigger;
	}

	/**
	 * Get the camera ID.
	 * 
	 * @return the camera ID
	 */
	public int getCameraId() {
		return cameraId;
	}

	/**
	 * Get the job trigger.
	 * 
	 * @return the trigger
	 */
	public Trigger getTrigger() {
		return trigger;
	}

}
