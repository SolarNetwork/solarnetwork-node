/* ==================================================================
 * SelectReadingDifference.java - 15 Nov 2026 4:12:08 pm
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
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.dao.jdbc.CommonJdbcUtils;
import net.solarnetwork.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.node.dao.NodeDatumFilter;

/**
 * Select the {@link net.solarnetwork.domain.datum.DatumReadingType#Difference}
 * reading of each datum stream matching a {@link NodeDatumFilter}, from raw
 * datum.
 *
 * <p>
 * This is ported from the SolarNet {@code SelectReadingDifference} class, for
 * the {@code Difference} reading type: the {@code dh_find_datum_diff_rows()}
 * function finds the readings at each end of the date range, looking up to a
 * year before or after it, and the {@code dh_diff_datum()} aggregate subtracts
 * them. With the aggregate setup, the function falls back to the readings of
 * the hourly aggregates where raw datum have been dropped. A stream without
 * readings has no row.
 * </p>
 *
 * <p>
 * The filter criteria are applied as described in {@link SelectAggregateDatum},
 * except:
 * </p>
 *
 * <dl>
 * <dt>start and end date</dt>
 * <dd>The times to find the starting and ending readings at. A missing date is
 * taken to be the current time, as SolarNet does.</dd>
 * <dt>most recent</dt>
 * <dd>Ignored, as SolarNet ignores it for reading results: there is at most one
 * row per stream.</dd>
 * <dt>sorts</dt>
 * <dd>As described in {@link SelectFilteredDatum}, with the time key sorting by
 * the time of the starting reading. The node key is never ignored.</dd>
 * </dl>
 *
 * <p>
 * Rows have the columns of {@link SelectAggregateDatum}, followed by the time
 * of the ending reading. The {@code ts_start} column is the time of the
 * starting reading. Only the accumulating values are set: {@code data_a} and
 * {@code read_a} hold the same difference, and {@code read_a} also holds the
 * starting and ending readings.
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
 * <li>{@code ts_end}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectReadingDifference
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final String schemaName;
	private final NodeDatumFilter filter;
	private final DatumMetaCriteria metaCriteria;
	private final Instant startDate;
	private final Instant endDate;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * <p>
	 * A missing start or end date of {@code filter} is taken to be the time
	 * this instance is created.
	 * </p>
	 *
	 * @param schemaName
	 *        the schema name
	 * @param filter
	 *        the filter
	 * @param nodeId
	 *        the node ID to limit the results to when {@code filter} has no
	 *        object IDs, or {@code null} for all nodes
	 * @param streamIds
	 *        the {@code stream_id} values of the streams to limit the results
	 *        to, or {@code null} or empty for all streams
	 * @param fetchSize
	 *        the fetch size, or {@literal 0} for the JDBC driver default
	 * @throws IllegalArgumentException
	 *         if {@code schemaName} is not valid or {@code filter} is
	 *         {@code null}
	 */
	public SelectReadingDifference(String schemaName, NodeDatumFilter filter, @Nullable Long nodeId,
			Integer @Nullable [] streamIds, int fetchSize) {
		super();
		this.schemaName = requireValidSchemaName(schemaName);
		this.filter = requireNonNullArgument(filter, "filter");
		this.metaCriteria = new DatumMetaCriteria(this.schemaName, this.filter, nodeId, streamIds);
		final Instant now = Instant.now();
		final Instant start = filter.getStartDate();
		final Instant end = filter.getEndDate();
		this.startDate = (start != null ? start : now);
		this.endDate = (end != null ? end : now);
		this.fetchSize = fetchSize;
	}

	@Override
	public String getSql() {
		final StringBuilder buf = new StringBuilder(1024);
		buf.append("""
				SELECT m.stream_id, m.node_id, d.ts_start
					, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a, d.ts_end
				""");
		sqlFromWhere(buf);
		SelectFilteredDatum.appendOrderBy(buf, filter.getSorts(), "d.ts_start", false);
		limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlFromWhere(StringBuilder buf) {
		buf.append("FROM ").append(schemaName).append(".dh_datum_meta m\n");
		buf.append("INNER JOIN LATERAL (\n");
		buf.append("\tSELECT (").append(schemaName).append(".dh_diff_datum(r ORDER BY r.ts)).*\n");
		buf.append("\tFROM ").append(schemaName)
				.append(".dh_find_datum_diff_rows(m.stream_id, ?, ?) r\n");
		// no row for a stream without readings, rather than one of NULL values
		buf.append("\tHAVING COUNT(*) > 0\n");
		buf.append(") d ON TRUE\n");
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
		stmt.setTimestamp(++p, Timestamp.from(startDate));
		stmt.setTimestamp(++p, Timestamp.from(endDate));
		return metaCriteria.prepare(con, stmt, p);
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	/**
	 * Count the streams with readings, ignoring pagination.
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
	 * Get the time the starting readings are found at.
	 *
	 * @return the filter start date, or the time this instance was created if
	 *         the filter has none
	 */
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Get the time the ending readings are found at.
	 *
	 * @return the filter end date, or the time this instance was created if
	 *         the filter has none
	 */
	public Instant getEndDate() {
		return endDate;
	}

}
