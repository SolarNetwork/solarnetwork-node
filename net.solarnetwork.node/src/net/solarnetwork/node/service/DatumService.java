/* ==================================================================
 * DatumService.java - 18/08/2021 7:27:06 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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
import net.solarnetwork.domain.datum.DatumMetadataOperations;

/**
 * API for a service that supports node-wide datum information.
 *
 * @author matt
 * @version 1.3
 * @since 1.89
 */
public interface DatumService extends DatumHistorian {

	/**
	 * Get a "view" of the unfiltered datum history.
	 *
	 * <p>
	 * The returned service's various datum history methods will operate on
	 * unfiltered datum, such as {@link #latest(String, Class)},
	 * {@link #offset(String, int, Class)}, and
	 * {@link #slice(String, int, int, Class)}.
	 * </p>
	 *
	 * @return a service view of unfiltered datum history
	 * @since 1.3
	 */
	DatumHistorian unfiltered();

	/**
	 * Get the metadata for a given datum stream.
	 *
	 * @param sourceId
	 *        the source ID of the datum metadata to get
	 * @return the metadata, or {@literal null} if no such metadata is available
	 * @since 1.1
	 */
	DatumMetadataOperations datumMetadata(String sourceId);

	/**
	 * Get the metadata for a set of datum streams matching a filter.
	 *
	 * @param sourceIdFilter
	 *        an optional set of Ant-style source ID patterns to filter by; use
	 *        {@literal null} or an empty set to return all available sources
	 * @return the matching metadata, never {@literal null}
	 * @since 1.1
	 */
	Collection<DatumMetadataOperations> datumMetadata(Set<String> sourceIdFilter);

}
