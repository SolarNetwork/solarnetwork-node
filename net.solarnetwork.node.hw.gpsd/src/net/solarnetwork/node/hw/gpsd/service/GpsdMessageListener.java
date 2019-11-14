/* ==================================================================
 * GpsdMessageListener.java - 15/11/2019 6:35:52 am
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

package net.solarnetwork.node.hw.gpsd.service;

import net.solarnetwork.node.hw.gpsd.domain.GpsdMessage;

/**
 * API for an observer of GPSd messages.
 * 
 * @param T
 *        the type of message this listener handles
 * @author matt
 * @version 1.0
 */
public interface GpsdMessageListener<T extends GpsdMessage> {

	/**
	 * Handle a GPSd message.
	 * 
	 * @param message
	 *        the message
	 */
	void onGpsdMessage(T message);

}
