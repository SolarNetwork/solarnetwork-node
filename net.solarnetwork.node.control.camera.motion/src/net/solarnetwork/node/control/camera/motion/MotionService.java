/* ==================================================================
 * MotionService.java - 29/10/2019 12:01:43 pm
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

import java.io.IOException;
import net.solarnetwork.domain.Identifiable;

/**
 * API for motion integration.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public interface MotionService extends Identifiable {

	/**
	 * Get the base URL to the motion web server.
	 * 
	 * @return the base URL
	 */
	String getMotionBaseUrl();

	/**
	 * Take a snapshot image.
	 * 
	 * @param cameraId
	 *        the camera ID
	 * @return {@literal true} if the snapshot was taken successfully
	 * @throws IOException
	 *         if any IO error occurs
	 */
	boolean takeSnapshot(int cameraId) throws IOException;

}
