/* ==================================================================
 * StoredDatum.java - 10 Nov 2026 9:02:31 pm
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

package net.solarnetwork.node.datumhistorian.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * A datum read from the datum history.
 *
 * @param streamId
 *        the stream ID
 * @param sourceId
 *        the source ID
 * @param locationId
 *        the location ID, for location datum, or {@code null} for node datum
 * @param timestamp
 *        the timestamp
 * @param className
 *        the name of the class of the stored datum
 * @param typeNames
 *        the names of all classes and interfaces the stored datum class can be
 *        assigned to
 * @param samples
 *        the samples
 * @author matt
 * @version 1.0
 */
public record StoredDatum(int streamId, String sourceId, @Nullable Long locationId, Instant timestamp,
		String className, Set<String> typeNames, DatumSamples samples) {

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param sourceId
	 *        the source ID
	 * @param locationId
	 *        the location ID, for location datum, or {@code null} for node
	 *        datum
	 * @param timestamp
	 *        the timestamp
	 * @param className
	 *        the name of the class of the stored datum
	 * @param typeNames
	 *        the names of all classes and interfaces the stored datum class can
	 *        be assigned to
	 * @param samples
	 *        the samples
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code locationId} is {@code null}
	 */
	public StoredDatum {
		requireNonNullArgument(sourceId, "sourceId");
		requireNonNullArgument(timestamp, "timestamp");
		requireNonNullArgument(className, "className");
		requireNonNullArgument(typeNames, "typeNames");
		requireNonNullArgument(samples, "samples");
	}

	/**
	 * Test if the stored datum class can be assigned to a given type.
	 *
	 * <p>
	 * This tests the stored type names, so it works even if the stored datum
	 * class is not available.
	 * </p>
	 *
	 * @param type
	 *        the type to test, or {@code null} for any type
	 * @return {@code true} if {@code type} is {@code null} or the stored datum
	 *         class can be assigned to {@code type}
	 */
	public boolean isAssignableTo(@Nullable Class<?> type) {
		return (type == null || typeNames.contains(type.getName()));
	}

	/**
	 * Get a datum ID for this datum.
	 *
	 * <p>
	 * Node datum get a {@code null} object ID, as datum captured by SolarNode
	 * normally have.
	 * </p>
	 *
	 * @return the datum ID
	 */
	public DatumId datumId() {
		return (locationId != null ? DatumId.locationId(locationId, sourceId, timestamp)
				: DatumId.nodeId(null, sourceId, timestamp));
	}

}
