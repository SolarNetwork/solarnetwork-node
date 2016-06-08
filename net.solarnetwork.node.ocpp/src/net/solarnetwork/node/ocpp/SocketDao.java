/* ==================================================================
 * SocketDao.java - 15/06/2015 12:16:19 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

/**
 * DAO API for {@link Socket} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface SocketDao {

	/**
	 * Store (create or update) a socket. The {@code socketId} value is the
	 * primary key.
	 * 
	 * @param socket
	 *        The socket to store.
	 */
	void storeSocket(Socket socket);

	/**
	 * Get an {@link Socket} for a given socket ID.
	 * 
	 * @param socketId
	 *        The socketId ID of the socket to get.
	 * @return The associated socket, or <em>null</em> if not available.
	 */
	Socket getSocket(String socketId);

	/**
	 * Get the {@code enabled} state of a socket. If no entity exists for the
	 * given {@code socketId} this method returns <em>true</em>.
	 * 
	 * @param socketId
	 *        The socketId ID of the socket to get the enabled state of.
	 * @return The enabled state.
	 */
	boolean isEnabled(String socketId);

}
