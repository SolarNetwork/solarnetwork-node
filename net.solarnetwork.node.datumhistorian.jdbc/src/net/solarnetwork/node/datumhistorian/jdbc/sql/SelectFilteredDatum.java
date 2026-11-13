/* ==================================================================
 * SelectFilteredDatum.java - 11 Nov 2026 11:02:42 am
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

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareDateRange;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareLimitOffset;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareParameter;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.limitOffset;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereDateRange;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.DatumMetaCriteria.NODE_ID_COLUMN;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.node.service.FilterableDatumHistorian.DEFAULT_SORTS;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_LOCATION;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_NODE;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_SOURCE;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_TIME;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.dao.jdbc.CommonJdbcUtils;
import net.solarnetwork.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.NodeDatumFilter;

/**
 * Select datum matching a {@link NodeDatumFilter}.
 *
 * <p>
 * The filter criteria are applied like this:
 * </p>
 *
 * <dl>
 * <dt>object IDs</dt>
 * <dd>With a {@link ObjectDatumKind#Location} object kind, the location IDs of
 * location datum. The location datum stored by all nodes are merged, keeping
 * one datum per location, source, and timestamp: the one stored by the
 * {@code nodeId} given to this class if there is one, otherwise the one stored
 * by the lowest node ID. Otherwise the node IDs of node datum. When no object
 * IDs are given, the results are limited to the streams of the {@code nodeId}
 * given to this class, and to either node or location datum when an object kind
 * is given.</dd>
 * <dt>stream IDs</dt>
 * <dd>Not taken from the filter, which has the UUID stream IDs a historian
 * gives its streams. The {@code stream_id} values of the streams to limit the
 * results to can be given to this class instead.</dd>
 * <dt>source IDs</dt>
 * <dd>Ant-style patterns, matched case-insensitively with the
 * {@code ant_patterns_to_regexps()} database function.</dd>
 * <dt>types</dt>
 * <dd>Datum class or interface names; a datum matches if its class can be
 * assigned to any of them.</dd>
 * <dt>start and end date</dt>
 * <dd>The minimum (inclusive) and maximum (exclusive) datum timestamps.</dd>
 * <dt>most recent</dt>
 * <dd>Only the most recent datum of each stream that matches the date and type
 * criteria. For merged location datum, the most recent datum of each location
 * and source.</dd>
 * <dt>sorts</dt>
 * <dd>The {@code SORT_BY_*} keys of {@code FilterableDatumHistorian}. Any
 * {@link net.solarnetwork.node.service.FilterableDatumHistorian#DEFAULT_SORTS}
 * keys not given are added, so the order is always the same. The node key is
 * ignored for merged location datum.</dd>
 * </dl>
 *
 * <p>
 * Rows have the columns of {@link SelectDatum}, followed by {@code node_id}.
 * Rows of a {@link #forStream(String, NodeDatumFilter, Long, Integer[], int)}
 * instance have only these columns, and the {@code dh_datum_type} table is not
 * joined:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code node_id}</li>
 * <li>{@code ts}</li>
 * <li>{@code data_i}</li>
 * <li>{@code data_a}</li>
 * <li>{@code data_s}</li>
 * <li>{@code data_t}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectFilteredDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The default fetch size. */
	public static final int DEFAULT_FETCH_SIZE = 100;

	/** The columns to sort by for each sort key. */
	private static final Map<String, String> SORT_KEY_COLUMNS = Map.of(SORT_BY_NODE, NODE_ID_COLUMN,
			SORT_BY_LOCATION, "m.loc_id", SORT_BY_SOURCE, "m.source_id", SORT_BY_TIME, "d.ts");

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final @Nullable Long nodeId;
	private final DatumMetaCriteria metaCriteria;
	private final boolean streamColumns;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_FETCH_SIZE} will be used.
	 * </p>
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, and whose location datum to prefer when {@code filter}
	 *        has location IDs; or {@code null} for all nodes
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public SelectFilteredDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId) {
		this(schemaName, filter, nodeId, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, and whose location datum to prefer when {@code filter}
	 *        has location IDs; or {@code null} for all nodes
	 * @param fetchSize
	 *        the fetch size, or {@literal 0} for the JDBC driver default
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public SelectFilteredDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			int fetchSize) {
		this(schemaName, filter, nodeId, null, fetchSize);
	}

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, and whose location datum to prefer when {@code filter}
	 *        has location IDs; or {@code null} for all nodes
	 * @param streamIds
	 *        the {@code stream_id} values of the streams to limit the results
	 *        to, or {@code null} or empty for all streams
	 * @param fetchSize
	 *        the fetch size, or {@literal 0} for the JDBC driver default
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public SelectFilteredDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds, int fetchSize) {
		this(schemaName, filter, nodeId, streamIds, false, fetchSize);
	}

	private SelectFilteredDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds, boolean streamColumns, int fetchSize) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.filter = requireNonNullArgument(filter, "filter");
		this.nodeId = nodeId;
		this.metaCriteria = new DatumMetaCriteria(this.schemaName, this.filter, nodeId, streamIds);
		this.streamColumns = streamColumns;
		this.fetchSize = fetchSize;
	}

	/**
	 * Select the datum matching a filter, with only the stream ID, timestamp,
	 * and property value columns.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, and whose location datum to prefer when {@code filter}
	 *        has location IDs; or {@code null} for all nodes
	 * @param streamIds
	 *        the {@code stream_id} values of the streams to limit the results
	 *        to, or {@code null} or empty for all streams
	 * @param fetchSize
	 *        the fetch size, or {@literal 0} for the JDBC driver default
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public static SelectFilteredDatum forStream(String schemaName, NodeDatumFilter filter,
			@Nullable Long nodeId, Integer @Nullable [] streamIds, int fetchSize) {
		return new SelectFilteredDatum(schemaName, filter, nodeId, streamIds, true, fetchSize);
	}

	/**
	 * Test if the location datum stored by all nodes are merged.
	 *
	 * @return {@code true} if the filter has location IDs
	 */
	private boolean isMergedLocations() {
		return metaCriteria.hasObjectIds() && metaCriteria.isLocationKind();
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(1024);
		if ( streamColumns ) {
			buf.append("""
					SELECT m.stream_id, m.node_id, d.ts
						, d.data_i, d.data_a, d.data_s, d.data_t
					""");
		} else {
			buf.append("""
					SELECT m.stream_id, m.source_id, m.loc_id, d.ts, t.class_name, t.type_names
						, m.names_i, m.names_a, m.names_s
						, d.data_i, d.data_a, d.data_s, d.data_t
						, m.node_id
					""");
		}
		if ( isMergedLocations() ) {
			sqlMergedLocationsFrom(buf);
			buf.append("INNER JOIN ").append(schemaName)
					.append(".dh_datum_meta m ON m.stream_id = k.stream_id\n");
			buf.append("INNER JOIN ").append(schemaName)
					.append(".dh_datum d ON d.stream_id = k.stream_id AND d.ts = k.ts\n");
			sqlTypeJoin(buf);
		} else {
			sqlFromWhere(buf);
		}
		sqlOrderBy(buf);
		limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlFromWhere(StringBuilder buf) {
		sqlStreamFrom(buf, false);
		sqlTypeJoin(buf);
		sqlWhere(buf);
	}

	private void sqlTypeJoin(StringBuilder buf) {
		if ( streamColumns ) {
			// no type columns are selected, and the type criteria do not use this join
			return;
		}
		buf.append("INNER JOIN ").append(schemaName)
				.append(".dh_datum_type t ON t.type_id = d.type_id\n");
	}

	/**
	 * Generate a {@code FROM} clause of the stream and datum keys of merged
	 * location datum, as a {@code k} table.
	 *
	 * <p>
	 * One datum is kept per location, source, and timestamp (or just per
	 * location and source for the most recent datum), preferring the datum of
	 * {@code nodeId} and then the lowest node ID.
	 * </p>
	 *
	 * @param buf
	 *        the buffer to append to
	 */
	private void sqlMergedLocationsFrom(StringBuilder buf) {
		final boolean mostRecent = filter.isMostRecent();
		final StringBuilder sub = new StringBuilder(512);
		sub.append("SELECT DISTINCT ON (m.loc_id, m.source_id");
		if ( !mostRecent ) {
			sub.append(", d.ts");
		}
		sub.append(") m.stream_id, d.ts\n");
		sqlStreamFrom(sub, true);
		sqlWhere(sub);
		sub.append("ORDER BY m.loc_id, m.source_id, d.ts");
		if ( mostRecent ) {
			sub.append(" DESC");
		}
		if ( nodeId != null ) {
			sub.append(", CASE WHEN m.node_id = ? THEN 0 ELSE 1 END");
		}
		sub.append(", m.node_id\n");

		buf.append("FROM (\n");
		appendIndented(buf, sub);
		buf.append(") k\n");
	}

	private static void appendIndented(StringBuilder buf, CharSequence lines) {
		lines.toString().lines().forEach(l -> buf.append('\t').append(l).append('\n'));
	}

	/**
	 * Generate a {@code FROM} clause of the stream meta and datum tables, as
	 * {@code m} and {@code d} tables.
	 *
	 * @param buf
	 *        the buffer to append to
	 * @param timestampOnly
	 *        {@code true} to include only the timestamp column of the most
	 *        recent datum
	 */
	private void sqlStreamFrom(StringBuilder buf, boolean timestampOnly) {
		buf.append("FROM ").append(schemaName).append(".dh_datum_meta m\n");
		if ( filter.isMostRecent() ) {
			buf.append("INNER JOIN LATERAL (\n");
			buf.append(timestampOnly ? "\tSELECT d.ts\n"
					: "\tSELECT d.ts, d.type_id, d.data_i, d.data_a, d.data_s, d.data_t\n");
			buf.append("\tFROM ").append(schemaName).append(".dh_datum d\n");
			buf.append("\tWHERE d.stream_id = m.stream_id\n");
			final StringBuilder where = new StringBuilder();
			sqlDatumWhere(where);
			appendIndented(buf, where);
			buf.append("\tORDER BY d.ts DESC\n");
			buf.append("\tLIMIT 1\n");
			buf.append(") d ON TRUE\n");
		} else {
			buf.append("INNER JOIN ").append(schemaName)
					.append(".dh_datum d ON d.stream_id = m.stream_id\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		final StringBuilder where = new StringBuilder();
		metaCriteria.appendSql(where);
		if ( !filter.isMostRecent() ) {
			sqlDatumWhere(where);
		}
		DatumMetaCriteria.appendWhere(buf, where);
	}

	private void sqlDatumWhere(StringBuilder buf) {
		whereDateRange(filter, "d.ts", buf);
		if ( filter.hasTypeCriteria() ) {
			buf.append("\tAND d.type_id = ANY(");
			metaCriteria.appendTypeIdsSql(buf);
			buf.append(")\n");
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		final Set<String> columns = new HashSet<>(8);
		if ( isMergedLocations() ) {
			// merged location datum are not grouped by node, so skip the node column
			columns.add(NODE_ID_COLUMN);
		}
		final StringBuilder order = new StringBuilder(64);
		final List<SortDescriptor> sorts = filter.getSorts();
		if ( sorts != null ) {
			for ( SortDescriptor sort : sorts ) {
				final String col = sortColumn(sort);
				if ( col != null && columns.add(col) ) {
					order.append(", ").append(col);
					if ( sort.isDescending() ) {
						order.append(" DESC");
					}
				}
			}
		}
		// add the remaining default sorts so the order is always the same
		for ( SortDescriptor sort : DEFAULT_SORTS ) {
			final String col = sortColumn(sort);
			if ( col != null && columns.add(col) ) {
				order.append(", ").append(col);
			}
		}
		buf.append("ORDER BY ").append(order, 2, order.length());
	}

	private static @Nullable String sortColumn(@Nullable SortDescriptor sort) {
		final String key = (sort != null ? sort.getSortKey() : null);
		return (key != null ? SORT_KEY_COLUMNS.get(key) : null);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = CommonJdbcUtils.createPreparedStatement(con, getSql(), false);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		int p = prepareFromWhere(con, stmt, 0);
		prepareLimitOffset(filter, stmt, p);
		return stmt;
	}

	private int prepareFromWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.isMostRecent() ) {
			p = prepareDatumWhere(con, stmt, p);
			p = metaCriteria.prepare(con, stmt, p);
		} else {
			p = metaCriteria.prepare(con, stmt, p);
			p = prepareDatumWhere(con, stmt, p);
		}
		if ( isMergedLocations() ) {
			p = prepareParameter(stmt, p, nodeId);
		}
		return p;
	}

	private int prepareDatumWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareDateRange(filter, stmt, p);
		if ( filter.hasTypeCriteria() ) {
			p = metaCriteria.prepareTypes(con, stmt, p);
		}
		return p;
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	/**
	 * Count the datum matching the filter, ignoring pagination.
	 */
	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			final StringBuilder buf = new StringBuilder(512);
			buf.append("SELECT COUNT(*)\n");
			if ( isMergedLocations() ) {
				sqlMergedLocationsFrom(buf);
			} else {
				sqlFromWhere(buf);
			}
			return buf.toString();
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			final PreparedStatement stmt = con.prepareStatement(getSql());
			prepareFromWhere(con, stmt, 0);
			return stmt;
		}

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
