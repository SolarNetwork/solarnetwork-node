/* ==================================================================
 * SimpleBackupFilter.java - 18/10/2025 10:21:21 am
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

import net.solarnetwork.domain.SimplePagination;

/**
 * Simple implementation of {@link BackupFilter}.
 *
 * @author matt
 * @version 1.0
 * @since 4.1
 */
public class SimpleBackupFilter extends SimplePagination implements BackupFilter {

	private Long nodeId;

	/**
	 * Constructor.
	 */
	public SimpleBackupFilter() {
		super();
	}

	/**
	 * Construct a filter with a node ID.
	 *
	 * @param nodeId
	 *        the node ID to filter on
	 * @return the new filter instance
	 */
	public static SimpleBackupFilter filterForNode(Long nodeId) {
		SimpleBackupFilter f = new SimpleBackupFilter();
		f.setNodeId(nodeId);
		return f;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

}
