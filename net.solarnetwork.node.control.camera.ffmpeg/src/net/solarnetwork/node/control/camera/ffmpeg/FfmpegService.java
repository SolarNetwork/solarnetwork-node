/* ==================================================================
 * FfmpegService.java - 31/08/2021 3:27:18 PM
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

import java.io.IOException;
import net.solarnetwork.service.Identifiable;

/**
 * API for ffmpeg integration.
 * 
 * @author matt
 * @version 2.0
 */
public interface FfmpegService extends Identifiable {

	/**
	 * Take a snapshot image.
	 * 
	 * @return {@literal true} if the snapshot was taken successfully
	 * @throws IOException
	 *         if any IO error occurs
	 */
	boolean takeSnapshot() throws IOException;

}
