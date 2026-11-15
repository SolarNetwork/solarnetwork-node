/* ==================================================================
 * DatumStreamMetadataProviderTests.java - 13 Nov 2026 10:18:37 am
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamMetadataProvider;

/**
 * Test cases for the {@link DatumStreamMetadataProvider} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class DatumStreamMetadataProviderTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_LOC_ID = 345L;

	private static ObjectDatumStreamMetadata meta(UUID streamId, ObjectDatumKind kind, Long objectId,
			String sourceId) {
		return new BasicObjectDatumStreamMetadata(streamId, "UTC", kind, objectId, sourceId,
				new String[] { "i" }, null, null);
	}

	@Test
	public void construct_invalid() {
		thenIllegalArgumentException().isThrownBy(() -> new DatumStreamMetadataProvider(null));
	}

	@Test
	public void empty() {
		// WHEN
		DatumStreamMetadataProvider provider = new DatumStreamMetadataProvider(List.of());

		// THEN
		// @formatter:off
		and.then(provider.metadataStreamIds())
			.as("No stream IDs")
			.isEmpty()
			;
		and.then(provider.metadataForStreamId(randomUUID()))
			.as("No metadata by stream ID")
			.isNull()
			;
		and.then(provider.metadataForObjectSource(TEST_NODE_ID, "a"))
			.as("No metadata by object and source ID")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void lookups() {
		// GIVEN
		final ObjectDatumStreamMetadata m1 = meta(randomUUID(), ObjectDatumKind.Node, TEST_NODE_ID, "b");
		final ObjectDatumStreamMetadata m2 = meta(randomUUID(), ObjectDatumKind.Node, TEST_NODE_ID, "a");
		final ObjectDatumStreamMetadata m3 = meta(randomUUID(), ObjectDatumKind.Location, TEST_LOC_ID,
				"weather");
		// the stream of the same location and source stored by another node
		final ObjectDatumStreamMetadata m4 = meta(randomUUID(), ObjectDatumKind.Location, TEST_LOC_ID,
				"weather");

		// WHEN
		DatumStreamMetadataProvider provider = new DatumStreamMetadataProvider(List.of(m1, m2, m3, m4));

		// THEN
		// @formatter:off
		and.then(provider.metadataStreamIds())
			.as("Stream IDs in the order of the metadata given")
			.containsExactly(m1.getStreamId(), m2.getStreamId(), m3.getStreamId(), m4.getStreamId())
			;
		and.then(provider.metadataForStreamId(m2.getStreamId()))
			.as("Metadata found by stream ID")
			.isSameAs(m2)
			;
		and.then(provider.metadataForStreamId(m4.getStreamId()))
			.as("Metadata of stream with same object and source as other found by stream ID")
			.isSameAs(m4)
			;
		and.then(provider.metadataForStreamId(randomUUID()))
			.as("No metadata for unknown stream ID")
			.isNull()
			;
		and.then(provider.metadataForObjectSource(TEST_NODE_ID, "a"))
			.as("Metadata found by object and source ID")
			.isSameAs(m2)
			;
		and.then(provider.metadataForObjectSource(TEST_LOC_ID, "weather"))
			.as("First metadata of streams with same object and source ID found")
			.isSameAs(m3)
			;
		and.then(provider.metadataForObjectSource(TEST_NODE_ID, "weather"))
			.as("No metadata for unknown source of object")
			.isNull()
			;
		and.then(provider.metadataForObjectSource(TEST_LOC_ID, "a"))
			.as("No metadata for source of other object")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void duplicateStreamId_firstUsed() {
		// GIVEN
		final UUID streamId = randomUUID();
		final ObjectDatumStreamMetadata m1 = meta(streamId, ObjectDatumKind.Node, TEST_NODE_ID, "a");
		final ObjectDatumStreamMetadata m2 = meta(streamId, ObjectDatumKind.Node, TEST_NODE_ID, "b");

		// WHEN
		DatumStreamMetadataProvider provider = new DatumStreamMetadataProvider(List.of(m1, m2));

		// THEN
		// @formatter:off
		and.then(provider.metadataStreamIds())
			.as("Stream ID provided once")
			.containsExactly(streamId)
			;
		and.then(provider.metadataForStreamId(streamId))
			.as("First metadata of stream ID used")
			.isSameAs(m1)
			;
		and.then(provider.metadataForObjectSource(TEST_NODE_ID, "b"))
			.as("Other metadata of stream ID not used")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void metadataCopied() {
		// GIVEN
		final ObjectDatumStreamMetadata m1 = meta(randomUUID(), ObjectDatumKind.Node, TEST_NODE_ID, "a");
		final List<ObjectDatumStreamMetadata> metadata = new ArrayList<>(List.of(m1));
		DatumStreamMetadataProvider provider = new DatumStreamMetadataProvider(metadata);

		// WHEN
		metadata.add(meta(randomUUID(), ObjectDatumKind.Node, TEST_NODE_ID, "b"));

		// THEN
		// @formatter:off
		and.then(provider.metadataStreamIds())
			.as("Metadata added after construction not provided")
			.containsExactly(m1.getStreamId())
			;
		thenExceptionOfType(UnsupportedOperationException.class)
			.as("Stream IDs cannot be changed")
			.isThrownBy(() -> provider.metadataStreamIds().clear())
			;
		// @formatter:on
	}

}
