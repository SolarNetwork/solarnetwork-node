/* ==================================================================
 * NetworkAssociationRequest.java - Nov 29, 2012 1:50:10 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.impl;

/**
 * Request bean for the NetworkAssociation service.
 *
 * @author matt
 * @version 1.0
 */
public class NetworkAssociationRequest {

	private String username;
	private String key;

	/**
	 * Constructor.
	 */
	public NetworkAssociationRequest() {
		super();
	}

	/**
	 * Get the uesrname.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 *
	 * @param username
	 *        the uesrname to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key.
	 *
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

}
