/* ==================================================================
 * DatumStreamMetadataRowMapper.java - 13 Nov 2026 10:18:37 am
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

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.getArray;
import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreamMetadata;
import net.solarnetwork.node.service.support.DatumStreamIdUtils;

/**
 * Row mapper for {@link ObjectDatumStreamMetadata} instances.
 *
 * <p>
 * Expects the columns returned by {@link SelectDatumStreamMetadata}:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code node_id}</li>
 * <li>{@code source_id}</li>
 * <li>{@code loc_id}</li>
 * <li>{@code names_i}</li>
 * <li>{@code names_a}</li>
 * <li>{@code names_s}</li>
 * </ol>
 *
 * <p>
 * A stream with a location ID is a location stream, with the location ID as its
 * object ID; any other stream is a node stream. Each stream is given the UUID
 * stream ID that {@link DatumStreamIdUtils} encodes from its node ID, a
 * historian domain, and its {@code stream_id}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumStreamMetadataRowMapper implements RowMapper<ObjectDatumStreamMetadata> {

	private final int streamIdDomain;
	private final @Nullable String timeZoneId;

	/**
	 * Constructor.
	 *
	 * @param streamIdDomain
	 *        the historian domain to encode stream IDs with, from 1 to 255
	 * @param timeZoneId
	 *        the time zone ID to give each stream, or {@code null}
	 */
	public DatumStreamMetadataRowMapper(int streamIdDomain, @Nullable String timeZoneId) {
		super();
		this.streamIdDomain = streamIdDomain;
		this.timeZoneId = timeZoneId;
	}

	@Override
	public ObjectDatumStreamMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
		final int streamId = rs.getInt(1);
		final long nodeId = rs.getLong(2);
		final String sourceId = rs.getString(3);
		final Long locationId = rs.getObject(4, Long.class);
		final String @Nullable [] namesI = getArray(rs, 5);
		final String @Nullable [] namesA = getArray(rs, 6);
		final String @Nullable [] namesS = getArray(rs, 7);
		final UUID uuid = encodeLocalStreamId(nodeId, streamIdDomain, streamId);
		if ( locationId != null ) {
			return new BasicObjectDatumStreamMetadata(uuid, timeZoneId, ObjectDatumKind.Location,
					locationId, sourceId, namesI, namesA, namesS);
		}
		return new BasicObjectDatumStreamMetadata(uuid, timeZoneId, ObjectDatumKind.Node, nodeId,
				sourceId, namesI, namesA, namesS);
	}

}
