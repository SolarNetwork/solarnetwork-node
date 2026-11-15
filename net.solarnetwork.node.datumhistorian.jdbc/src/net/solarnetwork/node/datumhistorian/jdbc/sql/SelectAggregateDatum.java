/* ==================================================================
 * SelectAggregateDatum.java - 15 Nov 2026 4:12:08 pm
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
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.limitOffset;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.whereDateRange;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.STREAM_TIME_ZONE_SQL;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.appendNodeTimeZoneJoin;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.prepareLocalDateRange;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.whereLocalDateRange;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.dao.jdbc.CommonJdbcUtils;
import net.solarnetwork.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.node.dao.NodeDatumFilter;

/**
 * Select the stored aggregate datum matching a {@link NodeDatumFilter}.
 *
 * <p>
 * This reads the {@code dh_agg_hourly}, {@code dh_agg_daily}, or
 * {@code dh_agg_monthly} table of the aggregate setup, for the
 * {@link Aggregation#Hour}, {@link Aggregation#Day}, or
 * {@link Aggregation#Month} aggregation of the filter. It is ported from the
 * SolarNet {@code SelectDatum} class, for those aggregations. The same rows
 * serve both list and reading results: the list results use the clock
 * accumulation in {@code data_a}, and the reading results the reading
 * difference, start, and end in {@code read_a}.
 * </p>
 *
 * <p>
 * The filter criteria are applied as described in {@link SelectFilteredDatum},
 * except:
 * </p>
 *
 * <dl>
 * <dt>object IDs</dt>
 * <dd>With a {@link net.solarnetwork.domain.datum.ObjectDatumKind#Location}
 * object kind, the location IDs of the location streams of any node. The rows
 * of each stream are returned, so location datum stored by more than one node
 * are not merged.</dd>
 * <dt>types</dt>
 * <dd>Only the streams that have stored a datum of a matching type. An
 * aggregate covers all the datum of its period, whatever their type, so the
 * rows themselves are not limited by type.</dd>
 * <dt>start and end date</dt>
 * <dd>The minimum (inclusive) and maximum (exclusive) start of the aggregate
 * periods.</dd>
 * <dt>local start and end date</dt>
 * <dd>The minimum (inclusive) and maximum (exclusive) local start of the
 * aggregate periods, in the time zone of the node of each stream, as described
 * in {@link SelectFilteredDatum}. The days and months of a stream start at
 * midnight in the same zone, so a local date range of whole days or months
 * selects exactly those periods.</dd>
 * <dt>most recent</dt>
 * <dd>Only the most recent aggregate of each stream that matches the date or
 * local date criteria, as SolarNet does for these aggregations.</dd>
 * <dt>sorts</dt>
 * <dd>As described in {@link SelectFilteredDatum}, with the time key sorting by
 * the period start. The node key is never ignored.</dd>
 * </dl>
 *
 * <p>
 * Rows have these columns:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code node_id}</li>
 * <li>{@code ts_start}</li>
 * <li>{@code data_i}</li>
 * <li>{@code data_a}</li>
 * <li>{@code data_s} (a JSON array)</li>
 * <li>{@code data_t}</li>
 * <li>{@code stat_i}</li>
 * <li>{@code read_a}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectAggregateDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final String tableName;
	private final DatumMetaCriteria metaCriteria;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter, whose aggregation must be supported
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, or {@code null} for all nodes
	 * @param streamIds
	 *        the {@code stream_id} values of the streams to limit the results
	 *        to, or {@code null} or empty for all streams
	 * @param fetchSize
	 *        the fetch size, or {@literal 0} for the JDBC driver default
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid, {@code filter} is
	 *         {@code null}, or the filter aggregation is not supported
	 * @see #isSupportedAggregation(Aggregation)
	 */
	public SelectAggregateDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds, int fetchSize) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.filter = requireNonNullArgument(filter, "filter");
		final String table = tableName(filter.getAggregation());
		if ( table == null ) {
			throw new IllegalArgumentException(
					"The %s aggregation is not supported.".formatted(filter.getAggregation()));
		}
		this.tableName = table;
		this.metaCriteria = new DatumMetaCriteria(this.schemaName, this.filter, nodeId, streamIds);
		this.fetchSize = fetchSize;
	}

	/**
	 * Test if an aggregation is supported.
	 *
	 * @param aggregation
	 *        the aggregation to test
	 * @return {@code true} if {@code aggregation} is {@link Aggregation#Hour},
	 *         {@link Aggregation#Day}, or {@link Aggregation#Month}
	 */
	public static boolean isSupportedAggregation(@Nullable Aggregation aggregation) {
		return (tableName(aggregation) != null);
	}

	private static @Nullable String tableName(@Nullable Aggregation aggregation) {
		if ( aggregation == null ) {
			return null;
		}
		return switch (aggregation) {
			case Hour -> "dh_agg_hourly";
			case Day -> "dh_agg_daily";
			case Month -> "dh_agg_monthly";
			default -> null;
		};
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(1024);
		buf.append("""
				SELECT m.stream_id, m.node_id, d.ts_start
					, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
				""");
		sqlFromWhere(buf);
		SelectFilteredDatum.appendOrderBy(buf, filter.getSorts(), "d.ts_start", false);
		limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlFromWhere(StringBuilder buf) {
		buf.append("FROM ").append(schemaName).append(".dh_datum_meta m\n");
		if ( filter.hasLocalDate() ) {
			appendNodeTimeZoneJoin(schemaName, buf);
		}
		if ( filter.isMostRecent() ) {
			buf.append("INNER JOIN LATERAL (\n");
			buf.append(
					"\tSELECT d.ts_start, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a\n");
			buf.append("\tFROM ").append(schemaName).append('.').append(tableName).append(" d\n");
			buf.append("\tWHERE d.stream_id = m.stream_id\n");
			final StringBuilder where = new StringBuilder();
			sqlDateWhere(where);
			SelectFilteredDatum.appendIndented(buf, where);
			buf.append("\tORDER BY d.ts_start DESC\n");
			buf.append("\tLIMIT 1\n");
			buf.append(") d ON TRUE\n");
		} else {
			buf.append("INNER JOIN ").append(schemaName).append('.').append(tableName)
					.append(" d ON d.stream_id = m.stream_id\n");
		}
		final StringBuilder where = new StringBuilder();
		metaCriteria.appendSql(where);
		if ( !filter.isMostRecent() ) {
			sqlDateWhere(where);
		}
		DatumMetaCriteria.appendWhere(buf, where);
	}

	private void sqlDateWhere(StringBuilder buf) {
		if ( filter.hasLocalDate() ) {
			whereLocalDateRange(filter, "d.ts_start", STREAM_TIME_ZONE_SQL, buf);
		} else {
			whereDateRange(filter, "d.ts_start", buf);
		}
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
			p = prepareDates(stmt, p);
			p = metaCriteria.prepare(con, stmt, p);
		} else {
			p = metaCriteria.prepare(con, stmt, p);
			p = prepareDates(stmt, p);
		}
		return p;
	}

	private int prepareDates(PreparedStatement stmt, int p) throws SQLException {
		return (filter.hasLocalDate() ? prepareLocalDateRange(filter, stmt, p)
				: prepareDateRange(filter, stmt, p));
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	/**
	 * Count the aggregate datum matching the filter, ignoring pagination.
	 */
	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			final StringBuilder buf = new StringBuilder(512);
			buf.append("SELECT COUNT(*)\n");
			sqlFromWhere(buf);
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

	/**
	 * Get the name of the aggregate table read.
	 *
	 * @return the table name, without the schema
	 */
	public String getTableName() {
		return tableName;
	}

}
