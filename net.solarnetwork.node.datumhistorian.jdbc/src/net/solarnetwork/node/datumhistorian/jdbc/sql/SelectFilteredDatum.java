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

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareArrayParameter;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareDateRange;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareLimitOffset;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareParameter;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.limitOffset;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereDateRange;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereEqual;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereOptimizedArrayContains;
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
 * given to this class, and to either node or location datum when an object
 * kind is given.</dd>
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
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectFilteredDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The default fetch size. */
	public static final int DEFAULT_FETCH_SIZE = 100;

	/** The node ID column. */
	private static final String NODE_ID_COLUMN = "m.node_id";

	/** The columns to sort by for each sort key. */
	private static final Map<String, String> SORT_KEY_COLUMNS = Map.of(SORT_BY_NODE, NODE_ID_COLUMN,
			SORT_BY_LOCATION, "m.loc_id", SORT_BY_SOURCE, "m.source_id", SORT_BY_TIME, "d.ts");

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final @Nullable Long nodeId;
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
	 *        object IDs, and whose location datum to prefer when
	 *        {@code filter} has location IDs; or {@code null} for all nodes
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
	 *        object IDs, and whose location datum to prefer when
	 *        {@code filter} has location IDs; or {@code null} for all nodes
	 * @param fetchSize
	 *        the fetch size, or {@literal 0} for the JDBC driver default
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public SelectFilteredDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			int fetchSize) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.filter = requireNonNullArgument(filter, "filter");
		this.nodeId = nodeId;
		this.fetchSize = fetchSize;
	}

	private boolean hasObjectIds() {
		final Long[] ids = filter.getObjectIds();
		return (ids != null && ids.length > 0);
	}

	private boolean isLocationKind() {
		return filter.getObjectKind() == ObjectDatumKind.Location;
	}

	/**
	 * Test if the location datum stored by all nodes are merged.
	 *
	 * @return {@code true} if the filter has location IDs
	 */
	private boolean isMergedLocations() {
		return hasObjectIds() && isLocationKind();
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(1024);
		buf.append("""
				SELECT m.stream_id, m.source_id, m.loc_id, d.ts, t.class_name, t.type_names
					, m.names_i, m.names_a, m.names_s
					, d.data_i::TEXT[] AS data_i, d.data_a::TEXT[] AS data_a, d.data_s, d.data_t
					, m.node_id
				""");
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
		sqlMetaWhere(where);
		if ( !filter.isMostRecent() ) {
			sqlDatumWhere(where);
		}
		if ( !where.isEmpty() ) {
			// replace the leading tab and AND of the first criteria
			buf.append("WHERE ").append(where, 5, where.length());
		}
	}

	private void sqlMetaWhere(StringBuilder buf) {
		if ( hasObjectIds() ) {
			if ( isLocationKind() ) {
				whereOptimizedArrayContains(filter.getObjectIds(), "m.loc_id", buf);
			} else {
				whereOptimizedArrayContains(filter.getObjectIds(), NODE_ID_COLUMN, buf);
				buf.append("\tAND m.loc_id IS NULL\n");
			}
		} else {
			whereEqual(nodeId, NODE_ID_COLUMN, buf);
			if ( filter.getObjectKind() == ObjectDatumKind.Node ) {
				buf.append("\tAND m.loc_id IS NULL\n");
			} else if ( isLocationKind() ) {
				buf.append("\tAND m.loc_id IS NOT NULL\n");
			}
		}
		if ( filter.hasSourceCriteria() ) {
			buf.append("\tAND m.source_id ~* ANY(").append(schemaName)
					.append(".ant_patterns_to_regexps(?))\n");
		}
		if ( filter.hasTypeCriteria() ) {
			// skip the streams that have never stored a matching type
			buf.append("\tAND m.type_ids && ");
			sqlTypeIds(buf);
			buf.append("\n");
		}
	}

	private void sqlDatumWhere(StringBuilder buf) {
		whereDateRange(filter, "d.ts", buf);
		if ( filter.hasTypeCriteria() ) {
			buf.append("\tAND d.type_id = ANY(");
			sqlTypeIds(buf);
			buf.append(")\n");
		}
	}

	private void sqlTypeIds(StringBuilder buf) {
		buf.append("ARRAY(SELECT tt.type_id FROM ").append(schemaName)
				.append(".dh_datum_type tt WHERE tt.type_names && ?)");
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
			p = prepareMetaWhere(con, stmt, p);
		} else {
			p = prepareMetaWhere(con, stmt, p);
			p = prepareDatumWhere(con, stmt, p);
		}
		if ( isMergedLocations() ) {
			p = prepareParameter(stmt, p, nodeId);
		}
		return p;
	}

	private int prepareMetaWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( hasObjectIds() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getObjectIds());
		} else {
			p = prepareParameter(stmt, p, nodeId);
		}
		if ( filter.hasSourceCriteria() ) {
			p = prepareArrayParameter(con, stmt, p, filter.getSourceIds());
		}
		if ( filter.hasTypeCriteria() ) {
			p = prepareArrayParameter(con, stmt, p, filter.getTypes());
		}
		return p;
	}

	private int prepareDatumWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareDateRange(filter, stmt, p);
		if ( filter.hasTypeCriteria() ) {
			p = prepareArrayParameter(con, stmt, p, filter.getTypes());
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
