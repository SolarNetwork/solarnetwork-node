/* ==================================================================
 * DatumStreams.java - 10 Nov 2026 9:08:40 pm
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
import java.io.Serializable;
import java.util.List;

/**
 * All the datum streams of a node.
 *
 * @param streams
 *        the streams
 * @author matt
 * @version 1.0
 */
public record DatumStreams(List<DatumStreamInfo> streams) implements Serializable {

	/**
	 * Constructor.
	 *
	 * @param streams
	 *        the streams
	 * @throws IllegalArgumentException
	 *         if {@code streams} is {@code null}
	 */
	public DatumStreams {
		streams = List.copyOf(requireNonNullArgument(streams, "streams"));
	}

	/**
	 * Test if a stream is included.
	 *
	 * @param streamId
	 *        the ID of the stream to look for
	 * @return {@code true} if a stream with the given ID is included
	 */
	public boolean contains(int streamId) {
		for ( DatumStreamInfo s : streams ) {
			if ( s.streamId() == streamId ) {
				return true;
			}
		}
		return false;
	}

}
