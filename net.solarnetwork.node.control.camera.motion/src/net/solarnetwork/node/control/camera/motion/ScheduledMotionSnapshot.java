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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.Trigger;

/**
 * A scheduled motion snapshot.
 * 
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public class ScheduledMotionSnapshot {

	private final int cameraId;
	private final Trigger trigger;
	private final ScheduledFuture<?> future;

	/**
	 * Constructor.
	 * 
	 * @param cameraId
	 *        the camera ID
	 * @param trigger
	 *        the job trigger
	 * @param future
	 *        the future
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ScheduledMotionSnapshot(int cameraId, Trigger trigger, ScheduledFuture<?> future) {
		super();
		this.cameraId = cameraId;
		this.trigger = requireNonNullArgument(trigger, "trigger");
		this.future = requireNonNullArgument(future, "future");
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

	/**
	 * Get the future.
	 * 
	 * @return the future
	 */
	public ScheduledFuture<?> getFuture() {
		return future;
	}

}
