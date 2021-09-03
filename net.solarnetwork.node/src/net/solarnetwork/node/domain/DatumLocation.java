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

package net.solarnetwork.node.domain;

import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * API for a datum location object.
 * 
 * <p>
 * A <em>location</em> is a standardized reference to some place or some source
 * of information, for example a weather location, a price location, etc.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumLocation {

	/** A price location type. */
	static final String PRICE_TYPE = "price";

	/** A day location type. */
	static final String DAY_TYPE = "day";

	/** A weather location type. */
	static final String WEATHER_TYPE = "weather";

	/**
	 * Get a unique ID of this location.
	 * 
	 * @return the location ID
	 */
	Long getLocationId();

	/**
	 * Get a name for this location.
	 * 
	 * @return the location name
	 */
	String getLocationName();

	/**
	 * Get a unique ID of the source of this location.
	 * 
	 * @return the source ID
	 */
	String getSourceId();

	/**
	 * Get the name of the source of this location.
	 * 
	 * @return the source name
	 */
	String getSourceName();

	/**
	 * Get metadata about the location.
	 * 
	 * @return metadata
	 */
	GeneralDatumMetadata getMetadata();

}
