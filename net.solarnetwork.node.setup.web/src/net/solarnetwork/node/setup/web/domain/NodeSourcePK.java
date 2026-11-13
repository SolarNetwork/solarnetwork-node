/* ==================================================================
 * NodeSourcePK.java - 13 Nov 2026 3:21:37 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;

/**
 * A node and source ID pair.
 *
 * <p>
 * This has the same shape as a SolarQuery node source, for example
 * <code>{"nodeId":123,"sourceId":"a/b"}</code>. Instances are ordered by node
 * ID and then source ID.
 * </p>
 *
 * @param nodeId
 *        the node ID
 * @param sourceId
 *        the source ID
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public record NodeSourcePK(long nodeId, String sourceId) implements Comparable<NodeSourcePK> {

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if {@code sourceId} is {@code null}
	 */
	public NodeSourcePK {
		requireNonNullArgument(sourceId, "sourceId");
	}

	@Override
	public int compareTo(NodeSourcePK o) {
		int result = Long.compare(nodeId, o.nodeId);
		if ( result == 0 ) {
			result = sourceId.compareTo(o.sourceId);
		}
		return result;
	}

	/**
	 * Get the node ID.
	 *
	 * <p>
	 * This JavaBean style accessor is for serializers that only use those, such
	 * as the CSV message converter.
	 * </p>
	 *
	 * @return the node ID
	 */
	public long getNodeId() {
		return nodeId;
	}

	/**
	 * Get the source ID.
	 *
	 * <p>
	 * This JavaBean style accessor is for serializers that only use those, such
	 * as the CSV message converter.
	 * </p>
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

}
