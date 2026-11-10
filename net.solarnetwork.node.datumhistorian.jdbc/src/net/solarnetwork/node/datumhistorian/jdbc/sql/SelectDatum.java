/* ==================================================================
 * SelectDatum.java - 10 Nov 2026 8:34:05 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.sql;

import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select datum offset from the most recent datum of each source.
 *
 * <p>
 * The history of a source is made up of all the datum streams with that source
 * ID, ordered by timestamp, newest first. For each matching source this
 * statement returns up to {@code count} datum starting at {@code offset}
 * within that history, optionally limited to datum with timestamps at or
 * before a given timestamp. Rows are ordered by source ID and then newest
 * first, with these columns:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code source_id}</li>
 * <li>{@code loc_id}</li>
 * <li>{@code ts}</li>
 * <li>{@code class_name}</li>
 * <li>{@code type_names}</li>
 * <li>{@code names_i}</li>
 * <li>{@code names_a}</li>
 * <li>{@code names_s}</li>
 * <li>{@code data_i} (as {@code TEXT[]} so {@code NaN} values can be read)</li>
 * <li>{@code data_a} (as {@code TEXT[]})</li>
 * <li>{@code data_s}</li>
 * <li>{@code data_t}</li>
 * </ol>
 *
 * <p>
 * When a type name is given, sources that have never stored a datum of that
 * type are skipped, using the {@code type_ids} of their streams. Datum of
 * other types are still returned for the remaining sources, so the caller can
 * apply the type filter to the datum found at the offset.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectDatum implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider {

	/** The default fetch size. */
	public static final int DEFAULT_FETCH_SIZE = 100;

	private final String schemaName;
	private final long nodeId;
	private final @Nullable String sourceId;
	private final Integer @Nullable [] streamIds;
	private final @Nullable Instant timestamp;
	private final int offset;
	private final int count;
	private final @Nullable String typeName;

	private SelectDatum(String schemaName, long nodeId, @Nullable String sourceId,
			Integer @Nullable [] streamIds, @Nullable Instant timestamp, int offset, int count,
			@Nullable String typeName) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.nodeId = nodeId;
		this.sourceId = sourceId;
		this.streamIds = streamIds;
		this.timestamp = timestamp;
		if ( offset < 0 ) {
			throw new IllegalArgumentException("The offset must not be negative.");
		}
		this.offset = offset;
		if ( count < 1 ) {
			throw new IllegalArgumentException("The count must be at least 1.");
		}
		this.count = count;
		this.typeName = typeName;
	}

	/**
	 * Select datum for a single source.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        an optional maximum timestamp (inclusive)
	 * @param offset
	 *        the offset from the most recent datum, {@literal 0} being the most
	 *        recent
	 * @param count
	 *        the maximum number of datum to return
	 * @param typeName
	 *        an optional datum type name; sources that never stored a datum of
	 *        this type are skipped
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid, {@code sourceId} is
	 *         {@code null}, {@code offset} is negative, or {@code count} is less
	 *         than {@literal 1}
	 */
	public static SelectDatum forSource(String schemaName, long nodeId, String sourceId,
			@Nullable Instant timestamp, int offset, int count, @Nullable String typeName) {
		return new SelectDatum(schemaName, nodeId, requireNonNullArgument(sourceId, "sourceId"), null,
				timestamp, offset, count, typeName);
	}

	/**
	 * Select one datum for each source of a set of streams.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param nodeId
	 *        the node ID
	 * @param streamIds
	 *        the stream IDs to limit the results to, or {@code null} for all
	 *        streams of the node
	 * @param timestamp
	 *        an optional maximum timestamp (inclusive)
	 * @param offset
	 *        the offset from the most recent datum, {@literal 0} being the most
	 *        recent
	 * @param typeName
	 *        an optional datum type name; sources that never stored a datum of
	 *        this type are skipped
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code offset} is negative
	 */
	public static SelectDatum forStreams(String schemaName, long nodeId,
			@Nullable Collection<Integer> streamIds, @Nullable Instant timestamp, int offset,
			@Nullable String typeName) {
		return new SelectDatum(schemaName, nodeId, null,
				(streamIds != null ? streamIds.toArray(Integer[]::new) : null), timestamp, offset, 1,
				typeName);
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(1536);
		buf.append("SELECT d.stream_id, d.source_id, d.loc_id, d.ts, t.class_name, t.type_names\n");
		buf.append("\t, d.names_i, d.names_a, d.names_s\n");
		buf.append("\t, d.data_i::TEXT[] AS data_i, d.data_a::TEXT[] AS data_a, d.data_s, d.data_t\n");
		buf.append("FROM (\n");
		buf.append("\tSELECT m.stream_id, m.source_id, m.loc_id, m.names_i, m.names_a, m.names_s\n");
		buf.append("\t\t, d.ts, d.type_id, d.data_i, d.data_a, d.data_s, d.data_t\n");
		buf.append("\t\t, row_number() OVER (PARTITION BY m.source_id")
				.append(" ORDER BY d.ts DESC, m.stream_id DESC) AS rn\n");
		buf.append("\tFROM ").append(schemaName).append(".dh_datum_meta m\n");
		buf.append("\tCROSS JOIN LATERAL (\n");
		buf.append("\t\tSELECT d.ts, d.type_id, d.data_i, d.data_a, d.data_s, d.data_t\n");
		buf.append("\t\tFROM ").append(schemaName).append(".dh_datum d\n");
		buf.append("\t\tWHERE d.stream_id = m.stream_id\n");
		if ( timestamp != null ) {
			buf.append("\t\t\tAND d.ts <= ?\n");
		}
		buf.append("\t\tORDER BY d.ts DESC\n");
		buf.append("\t\tLIMIT ?\n");
		buf.append("\t) d\n");
		buf.append("\tWHERE m.node_id = ?\n");
		if ( sourceId != null ) {
			buf.append("\t\tAND m.source_id = ?\n");
		} else if ( streamIds != null ) {
			buf.append("\t\tAND m.stream_id = ANY(?)\n");
		}
		if ( typeName != null ) {
			buf.append("\t\tAND m.source_id IN (\n");
			buf.append("\t\t\tSELECT tm.source_id\n");
			buf.append("\t\t\tFROM ").append(schemaName).append(".dh_datum_meta tm\n");
			buf.append("\t\t\tWHERE tm.node_id = ?\n");
			buf.append("\t\t\t\tAND tm.type_ids && ARRAY(\n");
			buf.append("\t\t\t\t\tSELECT tt.type_id\n");
			buf.append("\t\t\t\t\tFROM ").append(schemaName).append(".dh_datum_type tt\n");
			buf.append("\t\t\t\t\tWHERE tt.type_names @> ARRAY[?]::TEXT[]\n");
			buf.append("\t\t\t\t)\n");
			buf.append("\t\t)\n");
		}
		buf.append(") d\n");
		buf.append("INNER JOIN ").append(schemaName).append(".dh_datum_type t ON t.type_id = d.type_id\n");
		buf.append("WHERE d.rn BETWEEN ? AND ?\n");
		buf.append("ORDER BY d.source_id, d.rn");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setFetchSize(DEFAULT_FETCH_SIZE);
		setValues(stmt);
		return stmt;
	}

	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		int p = 0;
		final Instant timestamp = this.timestamp;
		if ( timestamp != null ) {
			ps.setObject(++p, timestamp.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		}

		// each stream needs at most (offset + count) rows to find the rows of its source
		final long maxRow = (long) offset + count;
		ps.setLong(++p, maxRow);

		ps.setLong(++p, nodeId);

		final String sourceId = this.sourceId;
		final Integer[] streamIds = this.streamIds;
		if ( sourceId != null ) {
			ps.setString(++p, sourceId);
		} else if ( streamIds != null ) {
			ps.setArray(++p, ps.getConnection().createArrayOf("integer", streamIds));
		}

		final String typeName = this.typeName;
		if ( typeName != null ) {
			ps.setLong(++p, nodeId);
			ps.setString(++p, typeName);
		}

		ps.setLong(++p, offset + 1L);
		ps.setLong(++p, maxRow);
	}

	/**
	 * Get the schema name.
	 *
	 * @return the schema name
	 */
	public String getSchemaName() {
		return schemaName;
	}

}
