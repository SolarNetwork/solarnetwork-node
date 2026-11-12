/* ==================================================================
 * DatumStreamIdUtils.java - 13 Nov 2026 8:41:07 am
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

package net.solarnetwork.node.service.support;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Utilities for the UUID stream IDs of SolarNode local datum streams.
 *
 * <p>
 * These methods let a historian that numbers its datum streams with integers
 * give each stream a UUID stream ID, and find the integer again from that UUID,
 * without any lookup. The UUID is an RFC 9562 version 8 UUID holding the node
 * ID, a historian domain, a fixed marker and the integer stream ID:
 * </p>
 *
 * <pre>
 * bits     field
 * 127-80   node ID bits 63-16
 * 79-76    version (8)
 * 75-64    node ID bits 15-4
 * 63-62    variant (binary 10)
 * 61-58    node ID bits 3-0
 * 57-50    domain
 * 49-32    marker (0x0534E)
 * 31-0     integer stream ID
 * </pre>
 *
 * <p>
 * The node ID keeps apart the stream IDs of nodes that share a database. The
 * domain does the same for historians that number their streams independently,
 * so each historian of a node must use a different domain, from 1 to 255. The
 * marker identifies a SolarNode local stream ID. Decoding checks every field,
 * so a stream ID from SolarNetwork, another node or another historian does not
 * decode.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 4.14
 */
public final class DatumStreamIdUtils {

	/** The fixed 18-bit marker of a local stream ID. */
	private static final int LOCAL_STREAM_ID_MARKER = 0x0534E;

	private DatumStreamIdUtils() {
		// not available
	}

	/**
	 * Encode a local stream ID as a UUID stream ID.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param domain
	 *        the historian domain, from 1 to 255
	 * @param localStreamId
	 *        the local stream ID
	 * @return the UUID stream ID
	 * @throws IllegalArgumentException
	 *         if {@code domain} is not between 1 and 255
	 */
	public static UUID encodeLocalStreamId(long nodeId, int domain, int localStreamId) {
		requireValidDomain(domain);
		// @formatter:off
		final long msb = (nodeId & 0xFFFF_FFFF_FFFF_0000L)  // node ID bits 63-16
				| 0x8000L                                   // version 8
				| ((nodeId >>> 4) & 0x0FFFL);               // node ID bits 15-4
		final long lsb = 0x8000_0000_0000_0000L             // variant 10
				| ((nodeId & 0xFL) << 58)                   // node ID bits 3-0
				| ((long) domain << 50)
				| ((long) LOCAL_STREAM_ID_MARKER << 32)
				| (localStreamId & 0xFFFF_FFFFL);
		// @formatter:on
		return new UUID(msb, lsb);
	}

	/**
	 * Decode a UUID stream ID into a local stream ID.
	 *
	 * <p>
	 * This is the reverse of {@link #encodeLocalStreamId(long, int, int)}.
	 * </p>
	 *
	 * @param streamId
	 *        the UUID stream ID to decode
	 * @param nodeId
	 *        the node ID the stream ID must have been encoded with
	 * @param domain
	 *        the historian domain the stream ID must have been encoded with,
	 *        from 1 to 255
	 * @return the local stream ID, or {@code null} if {@code streamId} is
	 *         {@code null} or was not encoded with {@code nodeId} and
	 *         {@code domain}
	 * @throws IllegalArgumentException
	 *         if {@code domain} is not between 1 and 255
	 */
	public static @Nullable Integer decodeLocalStreamId(@Nullable UUID streamId, long nodeId,
			int domain) {
		requireValidDomain(domain);
		if ( streamId == null ) {
			return null;
		}
		final int localStreamId = (int) streamId.getLeastSignificantBits();
		// encoding again checks the version, variant, node ID, domain, and marker
		return (encodeLocalStreamId(nodeId, domain, localStreamId).equals(streamId) ? localStreamId
				: null);
	}

	private static void requireValidDomain(int domain) {
		if ( domain < 1 || domain > 255 ) {
			throw new IllegalArgumentException(
					"The domain %d is not between 1 and 255.".formatted(domain));
		}
	}

}
