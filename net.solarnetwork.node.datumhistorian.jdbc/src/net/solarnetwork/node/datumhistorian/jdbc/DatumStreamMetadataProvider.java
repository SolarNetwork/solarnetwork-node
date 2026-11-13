/* ==================================================================
 * DatumStreamMetadataProvider.java - 13 Nov 2026 10:18:37 am
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;

/**
 * A map-backed {@link ObjectDatumStreamMetadataProvider}.
 *
 * <p>
 * Unlike {@link ObjectDatumStreamMetadataProvider#staticProvider(Iterable)},
 * this finds the metadata of a stream ID without a search, so it suits looking
 * up the metadata of every datum of a large result. The stream IDs are kept in
 * the order of the metadata given.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumStreamMetadataProvider implements ObjectDatumStreamMetadataProvider {

	private final Map<UUID, ObjectDatumStreamMetadata> metadata;

	/**
	 * Constructor.
	 *
	 * @param metadatas
	 *        the metadata; when more than one has the same stream ID, the first
	 *        is used
	 * @throws IllegalArgumentException
	 *         if {@code metadatas} is {@code null}
	 */
	public DatumStreamMetadataProvider(Collection<? extends ObjectDatumStreamMetadata> metadatas) {
		super();
		requireNonNullArgument(metadatas, "metadatas");
		final Map<UUID, ObjectDatumStreamMetadata> m = new LinkedHashMap<>(metadatas.size());
		for ( ObjectDatumStreamMetadata meta : metadatas ) {
			m.putIfAbsent(meta.getStreamId(), meta);
		}
		this.metadata = Collections.unmodifiableMap(m);
	}

	@Override
	public Collection<UUID> metadataStreamIds() {
		return metadata.keySet();
	}

	@Override
	public @Nullable ObjectDatumStreamMetadata metadataForStreamId(UUID streamId) {
		return metadata.get(streamId);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * When more than one stream has the given object and source ID, as the
	 * location streams of more than one node can, the first one is returned.
	 * </p>
	 */
	@Override
	public @Nullable ObjectDatumStreamMetadata metadataForObjectSource(Long objectId, String sourceId) {
		for ( ObjectDatumStreamMetadata meta : metadata.values() ) {
			if ( objectId.equals(meta.getObjectId()) && sourceId.equals(meta.getSourceId()) ) {
				return meta;
			}
		}
		return null;
	}

}
