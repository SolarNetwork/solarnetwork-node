/* ==================================================================
 * LocationService.java - Feb 19, 2011 2:29:08 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

import java.util.Collection;
import java.util.Set;
import net.solarnetwork.domain.GeneralLocationSourceMetadata;
import net.solarnetwork.node.domain.Location;

/**
 * API for managing locations.
 * 
 * @author matt
 * @version 1.2
 */
public interface LocationService {

	/** An unknown source, which is always available. */
	static final String UNKNOWN_SOURCE = "Unknown";

	/**
	 * An unknown location, which is always available for the UNKNOWN source.
	 */
	static final String UNKNOWN_LOCATION = "Unknown";

	/**
	 * Look up a Location based on a source name and location name.
	 * 
	 * @param <T>
	 *        the location type
	 * @param locationType
	 *        the type of location to look up
	 * @param sourceName
	 *        the source of the location data
	 * @param locationName
	 *        the location within the source (e.g. HAY2201)
	 * @return the matching location, or <em>null</em> if not found
	 * @deprecated see {@link #findLocationMetadata(String, String, Set)}
	 */
	@Deprecated
	<T extends Location> Collection<? extends Location> findLocations(Class<T> locationType,
			String sourceName, String locationName);

	/**
	 * Get a specific Location based on an ID.
	 * 
	 * @param <T>
	 *        the location type
	 * @param locationType
	 *        the type of location to look up
	 * @param locationId
	 *        the ID of the location to find
	 * @return the location, or <em>null</em> if not found
	 * @deprecated see {@link #getLocationMetadata(Long, String)}
	 */
	@Deprecated
	<T extends Location> T getLocation(Class<T> locationType, Long locationId);

	/**
	 * Query for general location metadata.
	 * 
	 * @param query
	 *        the query text
	 * @param sourceId
	 *        an optional source ID to limit the results to
	 * @param tags
	 *        the optional tags
	 * @return the matching location metadata, never <em>null</em>
	 * @since 1.1
	 */
	Collection<GeneralLocationSourceMetadata> findLocationMetadata(String query, String sourceId,
			Set<String> tags);

	/**
	 * Get a specific general location metadata.
	 * 
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @return the location metadata, or <em>null</em> if not found
	 * @since 1.1
	 */
	GeneralLocationSourceMetadata getLocationMetadata(Long locationId, String sourceId);

	/**
	 * Update the node's own location details in SolarNetwork.
	 * 
	 * <p>
	 * This is meant to support updating a node's own GPS coordinates. Other
	 * location properties may or may not be supported.
	 * </p>
	 * 
	 * @param location
	 *        the location details to update
	 * @since 1.2
	 */
	void updateNodeLocation(net.solarnetwork.domain.Location location);
}
