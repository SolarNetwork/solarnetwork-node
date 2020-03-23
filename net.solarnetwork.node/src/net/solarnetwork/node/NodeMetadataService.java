/* ==================================================================
 * NodeMetadataService.java - 21/06/2017 1:23:04 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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
 * API for managing node metadata.
 * 
 * @author matt
 * @version 1.0
 * @since 1.50
 */
public interface NodeMetadataService {

	/**
	 * Add node metadata. If metadata already exists for the given source, the
	 * values will be merged such that tags are only added and only new info
	 * values will be added.
	 * 
	 * @param meta
	 *        the metadata to add
	 */
	void addNodeMetadata(GeneralDatumMetadata meta);

	/**
	 * Get all metadata for the active node.
	 * 
	 * @return the metadata, or {@literal null} if none available
	 */
	GeneralDatumMetadata getNodeMetadata();

}
