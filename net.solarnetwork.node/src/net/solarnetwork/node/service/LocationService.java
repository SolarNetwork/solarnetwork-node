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

package net.solarnetwork.node.service;

import java.util.Collection;
import java.util.Set;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.GeneralLocationSourceMetadata;

/**
 * API for managing locations.
 * 
 * @author matt
 * @version 2.1
 */
public interface LocationService {

	/** An unknown source, which is always available. */
	static final String UNKNOWN_SOURCE = "Unknown";

	/**
	 * An unknown location, which is always available for the UNKNOWN source.
	 */
	static final String UNKNOWN_LOCATION = "Unknown";

	/**
	 * Query for general location metadata.
	 * 
	 * @param query
	 *        the query text
	 * @param sourceId
	 *        an optional source ID to limit the results to
	 * @param tags
	 *        the optional tags
	 * @return the matching location metadata, never {@literal null}
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
	 * @return the location metadata, or {@literal null} if not found
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
	void updateNodeLocation(Location location);

	/**
	 * Get the node's own location details from SolarNetwork.
	 * 
	 * @return the location, if available
	 * @since 2.1
	 */
	Location getNodeLocation();

}
