/* ==================================================================
 * DatumHistorian.java - 24/09/2024 6:08:30 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for node-wide datum history operations.
 *
 * @author matt
 * @version 1.0
 * @since 3.19
 */
public interface DatumHistorian {

	/**
	 * Get the latest available datum of a given type, optionally filtered by
	 * source IDs.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceIdFilter, 0, type)}.
	 * </p>
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceIdFilter
	 *        an optional set of Ant-style source ID patterns to filter by
	 * @param type
	 *        the type of datum
	 * @return the matching datum, never {@literal null}
	 * @see #offset(Set, int, Class)
	 */
	<T extends NodeDatum> Collection<T> latest(Set<String> sourceIdFilter, Class<T> type);

	/**
	 * Get the latest available datum of a given type, optionally filtered by
	 * source IDs.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceIdFilter, 0, type)}.
	 * </p>
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceId
	 *        the source ID to find
	 * @param type
	 *        the type of datum
	 * @return the matching datum, or {@literal null} if not available
	 * @see #offset(String, int, Class)
	 */
	<T extends NodeDatum> T latest(String sourceId, Class<T> type);

	/**
	 * Get an offset from the latest available datum of a given type, optionally
	 * filtered by source IDs.
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceIdFilter
	 *        an optional set of Ant-style source ID patterns to filter by; use
	 *        {@literal null} or an empty set to return all available sources
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @param type
	 *        the type of datum
	 * @return the matching datum, never {@literal null}
	 */
	<T extends NodeDatum> Collection<T> offset(Set<String> sourceIdFilter, int offset, Class<T> type);

	/**
	 * Get an offset from the latest available datum of a given type, optionally
	 * filtered by source IDs.
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceId
	 *        the source ID to find
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @param type
	 *        the type of datum
	 * @return the matching datum, or {@literal null} if not available
	 */
	<T extends NodeDatum> T offset(String sourceId, int offset, Class<T> type);

	/**
	 * Get datum offset from a given timestamp, optionally filtered by source
	 * IDs.
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceIdFilter
	 *        an optional set of Ant-style source ID patterns to filter by; use
	 *        {@literal null} or an empty set to return all available sources
	 * @param timestamp
	 *        the timestamp to reference
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @param type
	 *        the type of datum
	 * @return the matching datum, never {@literal null}
	 * @since 1.1
	 */
	<T extends NodeDatum> Collection<T> offset(Set<String> sourceIdFilter, Instant timestamp, int offset,
			Class<T> type);

	/**
	 * Get a datum offset from a given timestamp.
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceId
	 *        the source ID of the datum to find
	 * @param timestamp
	 *        the timestamp to reference
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @param type
	 *        the type of datum
	 * @return the datum, or {@literal null} if no such datum is available
	 */
	<T extends NodeDatum> T offset(String sourceId, Instant timestamp, int offset, Class<T> type);

	/**
	 * Get an set of datum offset from the latest available datum of a given
	 * type.
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceId
	 *        the source ID of the datum to find
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @param count
	 *        the maximum number of datum to return, starting from
	 *        {@code offset} and iterating over earlier datum
	 * @param type
	 *        the type of datum to filter the results by, or {@literal null} to
	 *        accept all datum
	 * @return the matching datum, never {@literal null}
	 */
	<T extends NodeDatum> Collection<T> slice(String sourceId, int offset, int count, Class<T> type);

	/**
	 * Get a set of datum offset from a given timestamp.
	 *
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceId
	 *        the source ID of the datum to find
	 * @param timestamp
	 *        the timestamp to reference
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @param count
	 *        the maximum number of datum to return, starting from
	 *        {@code offset} and iterating over earlier datum
	 * @param type
	 *        the type of datum to filter the results by, or {@literal null} to
	 *        accept all datum
	 * @return the matching datum, never {@literal null}
	 */
	<T extends NodeDatum> Collection<T> slice(String sourceId, Instant timestamp, int offset, int count,
			Class<T> type);

}
