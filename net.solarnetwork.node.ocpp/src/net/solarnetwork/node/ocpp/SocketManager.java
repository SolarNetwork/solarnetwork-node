/* ==================================================================
 * SocketManager.java - 31/07/2016 7:31:53 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

import net.solarnetwork.node.Identifiable;

/**
 * API for something that manages the on/off state of OCPP sockets.
 * 
 * @author matt
 * @version 1.0
 */
public interface SocketManager extends Identifiable {

	/**
	 * Set the socket enabled state for a given socket ID.
	 * 
	 * @param socketId
	 *        The ID of the socket to set the state of.
	 * @param enabled
	 *        The desired state to set.
	 * @return <em>true</em> if the state was set
	 */
	boolean adjustSocketEnabledState(String socketId, boolean enabled);

}
