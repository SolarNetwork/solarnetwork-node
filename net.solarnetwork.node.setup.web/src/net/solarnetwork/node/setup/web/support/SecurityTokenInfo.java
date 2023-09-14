/* ==================================================================
 * SecurityTokenInfo.java - 7/09/2023 7:54:44 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

/**
 * Security token information.
 * 
 * @author matt
 * @version 1.0
 */
public class SecurityTokenInfo {

	private String id;
	private String name;
	private String description;

	/**
	 * Constructor.
	 */
	public SecurityTokenInfo() {
		super();
	}

	/**
	 * Get the token ID.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the token ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 * 
	 * @param description
	 *        the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
