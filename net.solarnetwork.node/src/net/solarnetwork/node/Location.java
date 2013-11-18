/* ==================================================================
 * Location.java - Nov 17, 2013 7:36:36 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
 * API for a location object.
 * 
 * <p>
 * A <em>location</em> is a standardized reference to some place or some source
 * of information, for example a weather location, a price location, etc.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface Location {

	/**
	 * Get a unique ID of this location.
	 * 
	 * @return the location ID
	 */
	public Long getLocationId();

	/**
	 * Get a name for this location.
	 * 
	 * @return the location name
	 */
	public String getLocationName();

	/**
	 * Get a unique ID of the source of this location.
	 * 
	 * @return the source ID
	 */
	public Long getSourceId();

	/**
	 * Get the name of the source of this location.
	 * 
	 * @return the source name
	 */
	public String getSourceName();
}
