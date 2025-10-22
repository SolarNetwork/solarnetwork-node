/* ==================================================================
 * BackupFilter.java - 18/10/2025 9:37:07 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.backup;

import net.solarnetwork.dao.PaginationCriteria;

/**
 * API for backup search filter.
 *
 * @author matt
 * @version 1.0
 * @since 4.1
 */
public interface BackupFilter extends PaginationCriteria {

	/**
	 * Get a node ID to find.
	 *
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Test if this filter has any node criteria.
	 *
	 * @return {@literal true} if the node ID is non-null
	 */
	default boolean hasNodeCriteria() {
		return getNodeId() != null;
	}

	/**
	 * Test if this filter has any criteria.
	 *
	 * @return {@literal true} if any criteria is non-null
	 */
	default boolean hasAnyCriteria() {
		return hasNodeCriteria() || getMax() != null || getOffset() != null;
	}

}
