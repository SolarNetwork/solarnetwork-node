/* ==================================================================
 * DatumStreamIdUtilsTests.java - 13 Nov 2026 8:52:16 am
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

package net.solarnetwork.node.service.support.test;

import static net.solarnetwork.node.service.support.DatumStreamIdUtils.decodeLocalStreamId;
import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.node.service.support.DatumStreamIdUtils;
import net.solarnetwork.util.UuidUtils;

/**
 * Test cases for the {@link DatumStreamIdUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumStreamIdUtilsTests {

	private static final long[] NODE_IDS = new long[] { 0L, 1L, 123L, 0x0123_4567_89AB_CDEFL,
			Long.MAX_VALUE, Long.MIN_VALUE, -1L };

	private static final int[] DOMAINS = new int[] { 1, 2, 255 };

	private static final int[] LOCAL_STREAM_IDS = new int[] { 0, 1, 42, Integer.MAX_VALUE,
			Integer.MIN_VALUE, -1 };

	@Test
	public void encode() {
		// WHEN
		final UUID result = encodeLocalStreamId(123L, 1, 42);

		// THEN
		// @formatter:off
		then(result)
			.as("Node ID, domain, marker and local stream ID laid out")
			.isEqualTo(UUID.fromString("00000000-0000-8007-ac04-534e0000002a"))
			.as("Version 8 UUID")
			.returns(8, from(UUID::version))
			.as("RFC 9562 variant")
			.returns(2, from(UUID::variant))
			;
		// @formatter:on
	}

	@Test
	public void encode_allBits() {
		// WHEN
		final UUID result = encodeLocalStreamId(0x0123_4567_89AB_CDEFL, 0xA5, 0x89AB_CDEF);

		// THEN
		then(result).as("Every bit of the node ID, domain and local stream ID placed")
				.isEqualTo(UUID.fromString("01234567-89ab-8cde-be94-534e89abcdef"));
	}

	@Test
	public void roundTrip() {
		for ( long nodeId : NODE_IDS ) {
			for ( int domain : DOMAINS ) {
				for ( int localStreamId : LOCAL_STREAM_IDS ) {
					// WHEN
					final UUID streamId = encodeLocalStreamId(nodeId, domain, localStreamId);
					final Integer result = decodeLocalStreamId(streamId, nodeId, domain);

					// THEN
					then(result).as("Local stream ID decoded from %s (node %d, domain %d)", streamId,
							nodeId, domain).isEqualTo(localStreamId);
				}
			}
		}
	}

	@Test
	public void encode_separatesNodesAndDomains() {
		// GIVEN
		final Set<UUID> streamIds = new HashSet<>();

		// WHEN
		for ( long nodeId : NODE_IDS ) {
			for ( int domain : DOMAINS ) {
				streamIds.add(encodeLocalStreamId(nodeId, domain, 42));
			}
		}

		// THEN
		then(streamIds).as("Same local stream ID gives a different stream ID per node and domain")
				.hasSize(NODE_IDS.length * DOMAINS.length);
	}

	@Test
	public void decode_otherNode() {
		// GIVEN
		final long nodeId = 0x0123_4567_89AB_CDEFL;
		final UUID streamId = encodeLocalStreamId(nodeId, 1, 42);

		// THEN
		then(decodeLocalStreamId(streamId, nodeId ^ (1L << 63), 1))
				.as("Node ID that differs in bits 63-16 not decoded").isNull();
		then(decodeLocalStreamId(streamId, nodeId ^ (1L << 4), 1))
				.as("Node ID that differs in bits 15-4 not decoded").isNull();
		then(decodeLocalStreamId(streamId, nodeId ^ 1L, 1))
				.as("Node ID that differs in bits 3-0 not decoded").isNull();
	}

	@Test
	public void decode_otherDomain() {
		// GIVEN
		final UUID streamId = encodeLocalStreamId(123L, 1, 42);

		// THEN
		then(decodeLocalStreamId(streamId, 123L, 2)).as("Other domain not decoded").isNull();
	}

	@Test
	public void decode_foreign() {
		// GIVEN
		final UUID streamId = encodeLocalStreamId(123L, 1, 42);
		final long msb = streamId.getMostSignificantBits();
		final long lsb = streamId.getLeastSignificantBits();

		// THEN
		then(decodeLocalStreamId(UUID.randomUUID(), 123L, 1)).as("Random UUID not decoded").isNull();
		then(decodeLocalStreamId(UuidUtils.createUuidV7Boundary(Instant.now()), 123L, 1))
				.as("Version 7 UUID not decoded").isNull();
		then(decodeLocalStreamId(new UUID(msb ^ 0xC000L, lsb), 123L, 1))
				.as("Local stream ID with version 4 not decoded").isNull();
		then(decodeLocalStreamId(new UUID(msb, lsb ^ 0x4000_0000_0000_0000L), 123L, 1))
				.as("Local stream ID with other variant not decoded").isNull();
		then(decodeLocalStreamId(new UUID(msb, lsb ^ (1L << 32)), 123L, 1))
				.as("Local stream ID with other marker not decoded").isNull();
	}

	@Test
	public void decode_null() {
		then(decodeLocalStreamId(null, 123L, 1)).as("Null not decoded").isNull();
	}

	@Test
	public void invalidDomain() {
		// GIVEN
		final UUID streamId = encodeLocalStreamId(123L, 1, 42);

		// THEN
		for ( int domain : new int[] { -1, 0, 256 } ) {
			thenThrownBy(() -> encodeLocalStreamId(123L, domain, 42)).as("Encode with domain %d", domain)
					.isInstanceOf(IllegalArgumentException.class);
			thenThrownBy(() -> decodeLocalStreamId(streamId, 123L, domain))
					.as("Decode with domain %d", domain).isInstanceOf(IllegalArgumentException.class);
		}
	}

}
