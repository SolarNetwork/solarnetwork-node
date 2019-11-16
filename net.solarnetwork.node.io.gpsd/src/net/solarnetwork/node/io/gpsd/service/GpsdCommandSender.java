/* ==================================================================
 * GpsdCommandSender.java - 14/11/2019 7:29:56 am
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

package net.solarnetwork.node.io.gpsd.service;

import java.util.concurrent.Future;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;

/**
 * API for service that can send commands to GPSd.
 * 
 * @author matt
 * @version 1.0
 */
public interface GpsdCommandSender {

	/**
	 * Send a command to the GPSd server.
	 * 
	 * @param <T>
	 *        the expected response type
	 * @param command
	 *        the command type
	 * @param argument
	 *        the optional argument, which if provided will be serialized to
	 *        JSON
	 * @return a future for the response
	 */
	<T extends GpsdMessage> Future<T> sendCommand(GpsdMessageType command, Object argument);
}
