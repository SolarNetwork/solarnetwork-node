/* ==================================================================
 * Identifiable.java - Mar 24, 2014 1:19:50 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node;

/**
 * API for a standardized way of identifying services, to support configuring
 * links to specific instances of a service at runtime. Many managed services in
 * SolarNode allow any number of them to be deployed.
 * 
 * @author matt
 * @version 1.0
 */
public interface Identifiable {

	/**
	 * Get a unique identifier for this service. This should be meaningful to
	 * the service implementation.
	 * 
	 * @return unique identifier (should never be <em>null</em>)
	 */
	String getUID();

	/**
	 * Get a grouping identifier for this service. This should be meaningful to
	 * the service implementation.
	 * 
	 * @return a group identifier, or <em>null</em> if not part of any group
	 */
	String getGroupUID();

}
