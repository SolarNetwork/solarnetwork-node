/* ==================================================================
 * AuthorizationManager.java - 8/06/2015 7:10:32 am
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
 * API to handle OCPP authorization functionality, which may include local
 * caching and/or synchronization with the OCPP central system.
 * 
 * @author matt
 * @version 1.0
 */
public interface AuthorizationManager {

	/**
	 * Request authorization of a specific ID tag value.
	 * 
	 * @param idTag
	 *        The ID tag value to authorize.
	 * @return <em>true</em> if the tag is authorized, <em>false</em> otherwise.
	 */
	boolean authorize(String idTag);

}
