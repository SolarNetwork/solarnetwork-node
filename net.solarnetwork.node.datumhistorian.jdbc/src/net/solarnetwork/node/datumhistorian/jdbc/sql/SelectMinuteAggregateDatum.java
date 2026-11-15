/* ==================================================================
 * SelectMinuteAggregateDatum.java - 15 Nov 2026 7:48:21 pm
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

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.prepareLimitOffset;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.limitOffset;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.STREAM_TIME_ZONE_SQL;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.appendNodeTimeZoneJoin;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.dao.jdbc.CommonJdbcUtils;
import net.solarnetwork.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.node.dao.NodeDatumFilter;

/**
 * Select the sub-hour aggregate datum matching a {@link NodeDatumFilter},
 * computed from raw datum.
 *
 * <p>
 * This calls the {@code dh_rollup_datum_for_time_span_slots()} function of the
 * aggregate setup for each matching stream, for the
 * {@link Aggregation#FiveMinute}, {@link Aggregation#TenMinute},
 * {@link Aggregation#FifteenMinute}, or {@link Aggregation#ThirtyMinute}
 * aggregation of the filter: one row for each slot of that length that has
 * datum, with no reading statistics. Nothing is stored. It is ported from the
 * SolarNet {@code SelectDatum} class, for those aggregations, including its
 * rules:
 * </p>
 *
 * <ul>
 * <li>{@link Aggregation#Minute} is not supported, as SolarNet does not support
 * it.</li>
 * <li>A date range is required: either both local dates, or both the start and
 * end date.</li>
 * <li>The most recent criteria can not be used.</li>
 * </ul>
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
 * <dd>Only the streams that have stored a datum of a matching type. The
 * aggregates cover all the datum of each slot, whatever their type.</dd>
 * <dt>start and end date</dt>
 * <dd>The time span to compute the slots of, the start inclusive and the end
 * exclusive. Used when the filter does not have both local dates.</dd>
 * <dt>local start and end date</dt>
 * <dd>The time span, in the time zone of the node of each stream, as described
 * in {@link SelectFilteredDatum}. Used only when both are given, as SolarNet
 * does; otherwise the start and end date are used.</dd>
 * <dt>sorts</dt>
 * <dd>As described in {@link SelectFilteredDatum}, with the time key sorting by
 * the slot start. The node key is never ignored.</dd>
 * </dl>
 *
 * <p>
 * Rows have the columns of {@link SelectAggregateDatum}, with a {@code NULL}
 * {@code read_a}. The count query counts the slots with the faster
 * {@code dh_count_datum_time_span_slots()} function, as SolarNet does.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectMinuteAggregateDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final Aggregation aggregation;
	private final boolean localDates;
	private final DatumMetaCriteria metaCriteria;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter, whose aggregation must be a minute aggregation
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
	 *         {@code null}, the filter aggregation is not a minute aggregation
	 *         or is {@link Aggregation#Minute}, the filter has the most recent
	 *         criteria, or the filter has no date range
	 * @see #isMinuteAggregation(Aggregation)
	 */
	public SelectMinuteAggregateDatum(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds, int fetchSize) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.filter = requireNonNullArgument(filter, "filter");
		final Aggregation agg = filter.getAggregation();
		if ( agg == null || !isMinuteAggregation(agg) ) {
			throw new IllegalArgumentException(
					"The %s aggregation is not a minute aggregation.".formatted(agg));
		}
		// the same rules and messages as the SolarNet SelectDatum class
		if ( agg == Aggregation.Minute ) {
			throw new IllegalArgumentException("The Minute aggregation is not supported; please "
					+ "query the data directly by omitting the aggregation criteria or using None.");
		}
		if ( filter.isMostRecent() ) {
			throw new IllegalArgumentException(
					"The mostRecent flag cannot be used with aggregation %s.".formatted(agg));
		}
		this.localDates = filter.hasLocalDateRange();
		if ( !(localDates || filter.hasDateRange()) ) {
			throw new IllegalArgumentException(
					"A date range must be specified for aggregation %s.".formatted(agg));
		}
		this.aggregation = agg;
		this.metaCriteria = new DatumMetaCriteria(this.schemaName, this.filter, nodeId, streamIds);
		this.fetchSize = fetchSize;
	}

	/**
	 * Test if an aggregation is a minute aggregation.
	 *
	 * <p>
	 * This is any aggregation finer than {@link Aggregation#Hour}, other than
	 * {@link Aggregation#None}, as SolarNet defines it. Only
	 * {@link Aggregation#Minute} of these is not supported.
	 * </p>
	 *
	 * @param aggregation
	 *        the aggregation to test
	 * @return {@code true} if {@code aggregation} is a minute aggregation
	 */
	public static boolean isMinuteAggregation(@Nullable Aggregation aggregation) {
		return (aggregation != null && aggregation != Aggregation.None
				&& aggregation.compareLevel(Aggregation.Hour) < 0);
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(1024);
		buf.append("""
				SELECT m.stream_id, m.node_id, d.ts_start
					, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a
				""");
		sqlFromWhere(buf, "dh_rollup_datum_for_time_span_slots", "d");
		SelectFilteredDatum.appendOrderBy(buf, filter.getSorts(), "d.ts_start", false);
		limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlFromWhere(StringBuilder buf, String function, String alias) {
		buf.append("FROM ").append(schemaName).append(".dh_datum_meta m\n");
		if ( localDates ) {
			appendNodeTimeZoneJoin(schemaName, buf);
		}
		buf.append("INNER JOIN ").append(schemaName).append('.').append(function)
				.append("(m.stream_id, ");
		if ( localDates ) {
			buf.append("? AT TIME ZONE ").append(STREAM_TIME_ZONE_SQL).append(", ? AT TIME ZONE ")
					.append(STREAM_TIME_ZONE_SQL);
		} else {
			buf.append("?, ?");
		}
		buf.append(", ?) ").append(alias).append(" ON TRUE\n");
		final StringBuilder where = new StringBuilder();
		metaCriteria.appendSql(where);
		DatumMetaCriteria.appendWhere(buf, where);
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
		if ( localDates ) {
			stmt.setObject(++p, filter.getLocalStartDate(), Types.TIMESTAMP);
			stmt.setObject(++p, filter.getLocalEndDate(), Types.TIMESTAMP);
		} else {
			stmt.setTimestamp(++p, Timestamp.from(filter.getStartDate()));
			stmt.setTimestamp(++p, Timestamp.from(filter.getEndDate()));
		}
		stmt.setInt(++p, aggregation.getLevel());
		return metaCriteria.prepare(con, stmt, p);
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	/**
	 * Count the slots with datum matching the filter, ignoring pagination.
	 */
	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			final StringBuilder buf = new StringBuilder(512);
			buf.append("SELECT COALESCE(SUM(d.dcount), 0)\n");
			sqlFromWhere(buf, "dh_count_datum_time_span_slots", "d(dcount)");
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
	 * Get the slot length.
	 *
	 * @return the slot length, in seconds
	 */
	public int getSlotSeconds() {
		return aggregation.getLevel();
	}

}
