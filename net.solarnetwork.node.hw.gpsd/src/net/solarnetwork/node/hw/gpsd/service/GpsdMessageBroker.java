/* ==================================================================
 * GpsdMessageBroker.java - 15/11/2019 6:46:14 am
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
 * API for a GPSd message broker service.
 * 
 * @author matt
 * @version 1.0
 */
public interface GpsdMessageBroker {

	/**
	 * Add a message listener.
	 * 
	 * @param <M>
	 *        the message type to listen for
	 * @param messageType
	 *        the message type class
	 * @param listener
	 *        the listener
	 */
	<M extends GpsdMessage> void addMessageListener(Class<? extends M> messageType,
			GpsdMessageListener<M> listener);

	/**
	 * Remove a message listener.
	 * 
	 * @param <M>
	 *        the message type of the listener to remove
	 * @param messageType
	 *        the message type class
	 * @param listener
	 *        the listener
	 */
	<M extends GpsdMessage> void removeMessageListener(Class<? extends M> messageType,
			GpsdMessageListener<M> listener);

}
