/* ==================================================================
 * SelectThirtyMinuteAggregateDatum.java - 25 Nov 2026 2:14:37 pm
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
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.STREAM_TIME_ZONE_SQL;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.appendNodeTimeZoneJoin;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.prepareLocalDateRange;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
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
 * Select the {@link Aggregation#ThirtyMinute} aggregate datum matching a
 * {@link NodeDatumFilter}, combined from the stored 15 minute aggregates.
 *
 * <p>
 * This reads the {@code dh_agg_15m} table of the aggregate setup, and combines
 * the 15 minute rows of each half hour into one row. The half hours are those
 * of UTC time, starting on the hour and half hour, so each holds two 15 minute
 * slots of the same UTC hour. A half hour has a row when either of its slots
 * has one. The rows are combined by the rules the
 * {@code dh_rollup_agg_data_for_time_span()} function of the aggregate setup
 * rolls hours up into days with:
 * </p>
 *
 * <ul>
 * <li>Clock accumulations and reading differences are added.</li>
 * <li>The starting reading of each accumulating property is that of the first
 * slot that has one, and the ending reading that of the last, taking slots
 * whose accumulation is not {@code NULL} first.</li>
 * <li>Counts are added, and the least minimum and greatest maximum taken.</li>
 * <li>Means are weighted by count.</li>
 * <li>The status of each status property is the more frequent of the two
 * values. When they differ it is the one that sorts first, as {@code mode()}
 * gives it.</li>
 * <li>The tags are merged, each given once.</li>
 * <li>A property that only one slot has, or has a value for, takes the values
 * of that slot.</li>
 * </ul>
 *
 * <p>
 * The 15 minute rows of an hour add up to the hour, so the two half hours of an
 * hour, rolled up by the same rules, add up to it too, apart from the status
 * values: the more frequent of two values per half hour need not be the more
 * frequent value of the hour.
 * </p>
 *
 * <p>
 * Within an hour, the half hours keep the 15 minute rows' placement of the
 * accumulation. A slot without datum has no row of its own, so its share of the
 * hour's accumulation is in the row of the slot before it in the hour, or, when
 * it comes before the hour's first datum, in the hour's first row. So when the
 * {@code :30} slot has no datum and the {@code :45} slot has, the {@code :30}
 * slot's share is in the first half hour, where splitting the accumulation
 * between the half hours by time would give it to the second.
 * </p>
 *
 * <p>
 * The filter criteria are applied as described in {@link SelectAggregateDatum},
 * except:
 * </p>
 *
 * <dl>
 * <dt>start and end date</dt>
 * <dd>The minimum (inclusive) and maximum (exclusive) start of the half hours,
 * so a date that is not on a half hour selects the half hours from the next
 * one. Only the 15 minute rows of the selected half hours are read, so a half
 * hour is never combined from one of its slots because the range starts or ends
 * between them.</dd>
 * <dt>local start and end date</dt>
 * <dd>The minimum (inclusive) and maximum (exclusive) local start of the half
 * hours, in the time zone of the node of each stream, as described in
 * {@link SelectFilteredDatum}, and taken to the next half hour in the same
 * way.</dd>
 * <dt>most recent</dt>
 * <dd>Not supported, as SolarNet does not support it for this aggregation.</dd>
 * <dt>sorts</dt>
 * <dd>As described in {@link SelectFilteredDatum}, with the time key sorting by
 * the start of the half hour. The node key is never ignored.</dd>
 * </dl>
 *
 * <p>
 * Rows have the columns of {@link SelectAggregateDatum}. The count query counts
 * the half hours.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectThirtyMinuteAggregateDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/**
	 * The origin of the half hours: the origin of the 15 minute slots of the
	 * aggregate setup, a UTC midnight.
	 */
	private static final String SLOT_ORIGIN = "TIMESTAMPTZ '2000-01-03 00:00:00+00'";

	/**
	 * The SQL of the start of the half hour of a 15 minute row of the
	 * {@code dh_agg_15m} table, named {@code d}.
	 */
	private static final String HALF_HOUR_SQL = "date_bin(INTERVAL 'PT30M', d.ts_start, " + SLOT_ORIGIN
			+ ")";

	/**
	 * The select list of the half hours, named {@code m}, each combined from
	 * the rows of its two slots, {@code a} and {@code b}, by
	 * {@link #COMBINE_SQL}, and the start of the {@code FROM} clause, which the
	 * query of the half hours follows.
	 *
	 * <p>
	 * The status of each status property is the lesser of the two slots' values
	 * that are not JSON {@code null}, which is what {@code mode()} gives of two
	 * values, and the tags are those of both slots, each given once. Each is
	 * computed only when a slot has statuses or tags.
	 * </p>
	 */
	private static final String SELECT_SQL = """
			SELECT m.stream_id, m.node_id, m.ts_start
				, ci.data_i, ca.data_a
				, CASE WHEN a.data_s IS NOT NULL OR b.data_s IS NOT NULL THEN (
					SELECT jsonb_agg(LEAST(NULLIF(x.sa, 'null'::JSONB), NULLIF(x.sb, 'null'::JSONB)) ORDER BY x.idx)
					FROM ROWS FROM (jsonb_array_elements(a.data_s), jsonb_array_elements(b.data_s))
						WITH ORDINALITY AS x(sa, sb, idx)
					) END AS data_s
				, CASE WHEN a.data_t IS NOT NULL OR b.data_t IS NOT NULL THEN (
					SELECT array_agg(DISTINCT t.val) FROM unnest(a.data_t || b.data_t) AS t(val)
					) END AS data_t
				, ci.stat_i, ca.read_a
			FROM (
			""";

	/**
	 * The SQL that combines the rows of the two slots of each half hour,
	 * {@code a} and {@code b}, position by position, by the rules of the
	 * {@code dh_rollup_agg_data_for_time_span()} function: the instantaneous
	 * properties as {@code ci} and the accumulating properties as {@code ca}.
	 *
	 * <p>
	 * {@code unnest()} of the two slots' arrays pads the shorter with
	 * {@code NULL}, and {@code COALESCE()} of the sum and of each part ignores
	 * {@code NULL}, as {@code sum()} does. The minimum and maximum take the
	 * second slot's value first, so that of equal values written differently
	 * they give the one {@code min()} and {@code max()} give over the slots in
	 * time order.
	 * </p>
	 *
	 * <p>
	 * The {@code WHERE} clause of each query is always true. It makes the query
	 * depend on the start of the half hour, for which the planner has no
	 * statistics, so the planner takes every call to differ and does not cache
	 * the results by the slots' arrays, which seldom repeat: such a cache costs
	 * about a fifth of a long query without a page.
	 * </p>
	 */
	private static final String COMBINE_SQL = """
			CROSS JOIN LATERAL (
				SELECT vec_trim_scale(array_agg(COALESCE(x.ai * a.stat_i[x.idx][1] + x.bi * b.stat_i[x.idx][1], x.ai * a.stat_i[x.idx][1], x.bi * b.stat_i[x.idx][1])
						/ COALESCE(a.stat_i[x.idx][1] + b.stat_i[x.idx][1], a.stat_i[x.idx][1], b.stat_i[x.idx][1]) ORDER BY x.idx)) AS data_i
					, array_agg(ARRAY[COALESCE(a.stat_i[x.idx][1] + b.stat_i[x.idx][1], a.stat_i[x.idx][1], b.stat_i[x.idx][1])
						, LEAST(b.stat_i[x.idx][2], a.stat_i[x.idx][2]), GREATEST(b.stat_i[x.idx][3], a.stat_i[x.idx][3])] ORDER BY x.idx) AS stat_i
				FROM unnest(a.data_i, b.data_i) WITH ORDINALITY AS x(ai, bi, idx)
				WHERE m.ts_start IS NOT NULL
			) ci
			CROSS JOIN LATERAL (
				SELECT vec_trim_scale(array_agg(COALESCE(x.aa + x.ba, x.aa, x.ba) ORDER BY x.idx)) AS data_a
					, array_agg(ARRAY[COALESCE(a.read_a[x.idx][1] + b.read_a[x.idx][1], a.read_a[x.idx][1], b.read_a[x.idx][1])
						, CASE WHEN x.aa IS NULL AND x.ba IS NOT NULL THEN COALESCE(b.read_a[x.idx][2], a.read_a[x.idx][2]) ELSE COALESCE(a.read_a[x.idx][2], b.read_a[x.idx][2]) END
						, CASE WHEN x.ba IS NULL AND x.aa IS NOT NULL THEN COALESCE(a.read_a[x.idx][3], b.read_a[x.idx][3]) ELSE COALESCE(b.read_a[x.idx][3], a.read_a[x.idx][3]) END
						] ORDER BY x.idx) AS read_a
				FROM unnest(a.data_a, b.data_a) WITH ORDINALITY AS x(aa, ba, idx)
				WHERE m.ts_start IS NOT NULL
			) ca
			""";

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final DatumMetaCriteria metaCriteria;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter, whose aggregation must be
	 *        {@link Aggregation#ThirtyMinute}
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
	 *         {@code null}, the filter aggregation is not
	 *         {@link Aggregation#ThirtyMinute}, or the filter has the most
	 *         recent criteria
	 */
	public SelectThirtyMinuteAggregateDatum(String schemaName, NodeDatumFilter filter,
			@Nullable Long nodeId, Integer @Nullable [] streamIds, int fetchSize) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.filter = requireNonNullArgument(filter, "filter");
		final Aggregation agg = filter.getAggregation();
		if ( agg != Aggregation.ThirtyMinute ) {
			throw new IllegalArgumentException("The %s aggregation is not supported.".formatted(agg));
		}
		if ( filter.isMostRecent() ) {
			// the same message as the SolarNet SelectDatum class
			throw new IllegalArgumentException(
					"The mostRecent flag cannot be used with aggregation %s.".formatted(agg));
		}
		this.metaCriteria = new DatumMetaCriteria(this.schemaName, this.filter, nodeId, streamIds);
		this.fetchSize = fetchSize;
	}

	@Override
	public String getSql() {
		// The half hours are found, sorted and paged first, and then only those
		// of the page are combined, each from its two 15 minute rows. So a page
		// costs the finding of the date range's half hours, which reads only the
		// primary key of the 15 minute rows, and the combining of its own. The
		// nested loops over the half hours keep their order, which the outer
		// ORDER BY repeats so that the order does not depend on the plan.
		final StringBuilder buf = new StringBuilder(4096);
		buf.append(SELECT_SQL);
		final StringBuilder halfHours = new StringBuilder(1024);
		sqlHalfHours(halfHours);
		SelectFilteredDatum.appendIndented(buf, halfHours);
		buf.append(") m\n");
		appendSlot(buf, "a", "m.ts_start");
		appendSlot(buf, "b", "m.ts_start + INTERVAL 'PT15M'");
		buf.append(COMBINE_SQL);
		SelectFilteredDatum.appendOrderBy(buf, filter.getSorts(), "m.ts_start", false);
		return buf.toString();
	}

	/**
	 * Append the query of the half hours matching the filter, sorted and
	 * limited to the page: the stream, node, location and source IDs, and the
	 * start, of each half hour.
	 *
	 * <p>
	 * The half hours are found from the primary key of the 15 minute rows
	 * alone. A row starts its half hour when the stream's previous row in the
	 * date range is in an earlier half hour, or there is none, so each half
	 * hour is found once, whether it has one slot or two. The half hours are
	 * named {@code m}, as the sort columns are named for the meta table, whose
	 * columns they carry. Without a page the query ends with {@code OFFSET 0},
	 * which keeps the planner from merging it into the outer query, where it
	 * would sort the combined rows in place of the half hours.
	 * </p>
	 *
	 * @param buf
	 *        the buffer to append to
	 */
	private void sqlHalfHours(StringBuilder buf) {
		buf.append("SELECT m.stream_id, m.node_id, m.loc_id, m.source_id, m.ts_start\n");
		buf.append("FROM (\n");
		buf.append("\tSELECT d.stream_id, m.node_id, m.loc_id, m.source_id, ").append(HALF_HOUR_SQL)
				.append(" AS ts_start\n");
		buf.append("\t\t, lag(d.ts_start) OVER (PARTITION BY d.stream_id ORDER BY d.ts_start)")
				.append(" AS ts_prev\n");
		final StringBuilder from = new StringBuilder(512);
		sqlFromWhere(from);
		SelectFilteredDatum.appendIndented(buf, from);
		buf.append(") m\n");
		buf.append("WHERE m.ts_prev IS NULL OR m.ts_prev < m.ts_start\n");
		SelectFilteredDatum.appendOrderBy(buf, filter.getSorts(), "m.ts_start", false);
		if ( limitOffset(filter, buf) == 0 ) {
			buf.append("\nOFFSET 0");
		}
	}

	/**
	 * Append the {@code LATERAL} query of the 15 minute row of one slot of the
	 * half hour {@code m}, read by primary key.
	 *
	 * <p>
	 * The {@code LIMIT 1}, which the primary key allows, keeps the query a
	 * probe of the index for each half hour, which the planner cannot turn into
	 * a join over every row of the table.
	 * </p>
	 *
	 * @param buf
	 *        the buffer to append to
	 * @param alias
	 *        the name of the query
	 * @param slotStart
	 *        the SQL of the start of the slot
	 */
	private void appendSlot(StringBuilder buf, String alias, String slotStart) {
		buf.append("LEFT OUTER JOIN LATERAL (\n");
		buf.append("\tSELECT d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a\n");
		buf.append("\tFROM ").append(schemaName).append(".dh_agg_15m d\n");
		buf.append("\tWHERE d.stream_id = m.stream_id AND d.ts_start = ").append(slotStart)
				.append('\n');
		buf.append("\tLIMIT 1\n");
		buf.append(") ").append(alias).append(" ON TRUE\n");
	}

	/**
	 * Append the {@code FROM} and {@code WHERE} clauses that select the 15
	 * minute rows, as {@code d}, of the half hours matching the filter.
	 *
	 * @param buf
	 *        the buffer to append to
	 */
	private void sqlFromWhere(StringBuilder buf) {
		buf.append("FROM ").append(schemaName).append(".dh_datum_meta m\n");
		if ( filter.hasLocalDate() ) {
			appendNodeTimeZoneJoin(schemaName, buf);
		}
		buf.append("INNER JOIN ").append(schemaName)
				.append(".dh_agg_15m d ON d.stream_id = m.stream_id\n");
		final StringBuilder where = new StringBuilder();
		metaCriteria.appendSql(where);
		sqlDateWhere(where);
		DatumMetaCriteria.appendWhere(buf, where);
	}

	/**
	 * Append the date or local date criteria, as lines that each start with a
	 * tab and {@code AND}.
	 *
	 * <p>
	 * Each date is taken to the first half hour that starts at or after it, so
	 * comparing the start of each 15 minute row with it selects the rows of the
	 * half hours that start within the range, and only those.
	 * </p>
	 *
	 * @param buf
	 *        the buffer to append to
	 */
	private void sqlDateWhere(StringBuilder buf) {
		final boolean local = filter.hasLocalDate();
		final String date = (local ? "? AT TIME ZONE " + STREAM_TIME_ZONE_SQL : "?::TIMESTAMPTZ");
		if ( local ? filter.getLocalStartDate() != null : filter.getStartDate() != null ) {
			buf.append("\tAND d.ts_start >= ").append(halfHourAtOrAfter(date)).append('\n');
		}
		if ( local ? filter.getLocalEndDate() != null : filter.getEndDate() != null ) {
			buf.append("\tAND d.ts_start < ").append(halfHourAtOrAfter(date)).append('\n');
		}
	}

	/**
	 * Get the SQL of the start of the first half hour at or after a time.
	 *
	 * <p>
	 * This is the start of the half hour of the time one microsecond earlier,
	 * the precision of a Postgres timestamp, plus half an hour.
	 * </p>
	 *
	 * @param ts
	 *        the SQL of the time
	 * @return the SQL
	 */
	private static String halfHourAtOrAfter(String ts) {
		return "date_bin(INTERVAL 'PT30M', (" + ts + ") - INTERVAL 'PT0.000001S', " + SLOT_ORIGIN
				+ ") + INTERVAL 'PT30M'";
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
		p = metaCriteria.prepare(con, stmt, p);
		return (filter.hasLocalDate() ? prepareLocalDateRange(filter, stmt, p)
				: prepareDateRange(filter, stmt, p));
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	/**
	 * Count the half hours matching the filter, ignoring pagination.
	 */
	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			final StringBuilder buf = new StringBuilder(512);
			buf.append("SELECT COUNT(*)\n");
			buf.append("FROM (\n");
			buf.append("\tSELECT DISTINCT d.stream_id, ").append(HALF_HOUR_SQL).append('\n');
			final StringBuilder from = new StringBuilder(512);
			sqlFromWhere(from);
			SelectFilteredDatum.appendIndented(buf, from);
			buf.append(") k");
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
