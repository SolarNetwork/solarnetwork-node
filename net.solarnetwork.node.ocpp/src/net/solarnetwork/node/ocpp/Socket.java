/* ==================================================================
 * Socket.java - 15/06/2015 12:16:41 pm
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

import java.util.Date;

/**
 * Domain object for a socket, which is a physical connector for charging a
 * device with.
 * 
 * @author matt
 * @version 1.0
 */
public class Socket {

	private Date created;
	private String socketId;
	private boolean enabled = true;

	/**
	 * Default constructor.
	 */
	public Socket() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param socketId
	 *        The socket ID to set.
	 * @param enabled
	 *        The enabled state to set.
	 */
	public Socket(String socketId, boolean enabled) {
		super();
		this.socketId = socketId;
		this.enabled = enabled;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getSocketId() {
		return socketId;
	}

	public void setSocketId(String socketId) {
		this.socketId = socketId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
