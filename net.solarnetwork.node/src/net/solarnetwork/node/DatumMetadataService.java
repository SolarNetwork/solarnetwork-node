/* ==================================================================
 * DatumMetadataService.java - Oct 6, 2014 12:17:53 PM
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

import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * API for manipulating {@link GeneralDatumMetadata} associated with a node.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumMetadataService {

	/**
	 * Add metadata to a specific source. If metadata already exists for the
	 * given source, the values will be merged such that tags are only added and
	 * only new info values will be added.
	 * 
	 * @param sourceId
	 *        the source ID to add to
	 * @param meta
	 *        the metadata to add
	 */
	void addSourceMetadata(String sourceId, GeneralDatumMetadata meta);

	/**
	 * Find datum metadata for a given source.
	 * 
	 * @param sourceId
	 *        the sourceId to get the metadta for
	 * @return the metadata, or <em>null</em> if none available
	 */
	GeneralDatumMetadata getSourceMetadata(String sourceId);

}
