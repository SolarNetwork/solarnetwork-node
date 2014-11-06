/* ==================================================================
 * LocationLookupSettingSpecifier.java - Nov 19, 2013 1:07:03 PM
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

package net.solarnetwork.node.settings;

import net.solarnetwork.node.domain.Location;

/**
 * A setting for a location ID.
 * 
 * @author matt
 * @version 1.1
 */
public interface LocationLookupSettingSpecifier extends KeyedSettingSpecifier<Long>, Location {

	/**
	 * Get the location this setting is for.
	 * 
	 * @return a Location, or <em>null</em> if none available
	 */
	Location getLocation();

	/**
	 * Get the location type or tag, e.g. "weather", "price", etc.
	 * 
	 * @return the location type
	 */
	String getLocationTypeKey();

}
