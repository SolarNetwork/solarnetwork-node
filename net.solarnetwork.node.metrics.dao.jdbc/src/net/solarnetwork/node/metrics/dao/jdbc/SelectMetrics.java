/* ==================================================================
 * SelectMetrics.java - 14/07/2024 2:22:50 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.dao.jdbc;

import static java.lang.String.format;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.dao.MetricFilter;
import net.solarnetwork.node.metrics.domain.MetricAggregate;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate {@code SELECT} SQL for metric values based on a filter.
 *
 * @author matt
 * @version 1.1
 */
public class SelectMetrics implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 100;

	private final MetricFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectMetrics(MetricFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @param fetchSize
	 *        the fetch size, or {@code 0} to not specify
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectMetrics(MetricFilter filter, int fetchSize) {
		super();
		this.filter = ObjectUtils.requireNonNullArgument(filter, "filter");
		this.fetchSize = fetchSize;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		if ( filter.hasAggregateCriteria() ) {
			sqlAgg(buf);
		} else {
			sqlRaw(buf);
		}
		sqlOrderBy(buf);
		sqlPagination(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		setValues(stmt);
		return stmt;
	}

	@Override
	public void setValues(PreparedStatement stmt) throws SQLException {
		int p = prepareWhere(stmt, 0);
		preparePagination(stmt, p);
	}

	private void sqlRaw(StringBuilder buf) {
		if ( filter.isMostRecent() ) {
			buf.append("SELECT ts, mtype, mname, val FROM (\n");
		}
		buf.append("SELECT ");
		if ( filter.isMostRecent() ) {
			buf.append("DISTINCT ON (mtype, mname) ");
		}
		buf.append("ts, mtype, mname, val\n");
		buf.append("FROM solarnode.mtr_metric\n");
		sqlWhere(buf);
		if ( filter.isMostRecent() ) {
			buf.append("ORDER BY ts DESC, mtype, mname\n");
			buf.append(") m\n");
		}
	}

	private void sqlAgg(StringBuilder buf) {
		buf.append("WITH m AS (\n");
		buf.append("\tSELECT mname\n");
		int idx = 0;
		for ( MetricAggregate agg : filter.getAggregates() ) {
			buf.append("\t\t, ");
			switch (agg.getType()) {
				case MetricAggregate.METRIC_TYPE_AVERAGE:
					buf.append("avg(val)");
					break;
				case MetricAggregate.METRIC_TYPE_MAXIMUM:
					buf.append("max(val)");
					break;
				case MetricAggregate.METRIC_TYPE_MINIMUM:
					buf.append("min(val)");
					break;
				case MetricAggregate.METRIC_TYPE_QUANTILE:
					buf.append("percentile_cont(?) WITHIN GROUP (ORDER BY val)");
					break;
				default:
					throw new IllegalArgumentException(
							format("MetricAggregate type [%s] not supported.", agg.getType()));
			}
			buf.append(" AS m").append(idx++).append("\n");
		}
		buf.append("\tFROM solarnode.mtr_metric\n");
		sqlWhere(buf);
		buf.append("\tGROUP BY mname\n");
		buf.append(")\n");
		buf.append(", d AS (");
		idx = 0;
		for ( MetricAggregate agg : filter.getAggregates() ) {
			if ( idx > 0 ) {
				buf.append("\tUNION ALL\n");
			}
			buf.append("\tSELECT '").append(agg.key()).append("' AS mtype\n");
			buf.append("\t\t, mname\n");
			buf.append("\t\t, m").append(idx++).append(" AS val\n");
			buf.append("\tFROM m\n");
		}
		buf.append(")\n");
		buf.append("SELECT CURRENT_TIMESTAMP AS ts\n");
		buf.append("\t, mtype\n");
		buf.append("\t, mname\n");
		buf.append("\t, val\n");
		buf.append("FROM d\n");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();

		if ( filter.hasStartDate() ) {
			where.append("\tAND ts >= ?\n");
		}
		if ( filter.hasEndDate() ) {
			where.append("\tAND ts < ?\n");
		}
		if ( filter.hasTypeCriteria() ) {
			where.append("\tAND mtype = ANY(?)\n");
		}
		if ( filter.hasNameCriteria() ) {
			where.append("\tAND mname = ANY(?)\n");
		}

		if ( where.length() > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		List<SortDescriptor> sorts = filter.getSorts();
		int count = 0;
		if ( sorts != null ) {
			for ( SortDescriptor s : sorts ) {
				if ( s.getSortKey() == null ) {
					continue;
				}
				String colName = null;
				switch (s.getSortKey()) {
					case MetricDao.SORT_BY_DATE:
						colName = "ts";
						break;

					case MetricDao.SORT_BY_TYPE:
						colName = "mtype";
						break;

					case MetricDao.SORT_BY_NAME:
						colName = "mname";
						break;

					case MetricDao.SORT_BY_VALUE:
						colName = "val";
						break;

					default:
						// ignore
				}
				if ( colName != null ) {
					if ( count++ == 0 ) {
						buf.append("ORDER BY ");
					} else {
						buf.append(", ");
					}
					buf.append(colName);
					if ( s.isDescending() ) {
						buf.append(" DESC");
					}
				}
			}
		}
		if ( count < 1 ) {
			if ( filter.hasAggregateCriteria() ) {
				buf.append("ORDER BY mname, mtype");
			} else {
				buf.append("ORDER BY ts, mtype, mname");
			}
		}
		buf.append("\n");
	}

	private void sqlPagination(StringBuilder buf) {
		if ( filter.getOffset() != null ) {
			buf.append("OFFSET ? ROWS\n");
		}
		if ( filter.getMax() != null ) {
			buf.append("FETCH FIRST ? ROWS ONLY\n");
		}
	}

	private int prepareWhere(PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasAggregateCriteria() ) {
			for ( MetricAggregate agg : filter.getAggregates() ) {
				if ( MetricAggregate.METRIC_TYPE_QUANTILE.equals(agg.getType()) ) {
					stmt.setObject(++p, agg.numberParameter(0));
				}
			}
		}
		if ( filter.hasStartDate() ) {
			stmt.setObject(++p, filter.getStartDate());
		}
		if ( filter.hasEndDate() ) {
			stmt.setObject(++p, filter.getEndDate());
		}
		if ( filter.hasTypeCriteria() ) {
			Array a = stmt.getConnection().createArrayOf("VARCHAR", filter.getTypes());
			stmt.setArray(++p, a);
			a.free();
		}
		if ( filter.hasNameCriteria() ) {
			Array a = stmt.getConnection().createArrayOf("VARCHAR", filter.getNames());
			stmt.setArray(++p, a);
			a.free();
		}
		return p;
	}

	private int preparePagination(PreparedStatement stmt, int p) throws SQLException {
		if ( filter.getOffset() != null ) {
			stmt.setInt(++p, filter.getOffset());
		}
		if ( filter.getMax() != null ) {
			stmt.setInt(++p, filter.getMax());
		}
		return p;
	}

	/**
	 * Get SQL to execute a {@code COUNT} style query based on the configured
	 * filter.
	 *
	 * @return the SQL creator
	 */
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			StringBuilder buf = new StringBuilder();
			buf.append("SELECT COUNT(*) FROM (");
			if ( filter.hasAggregateCriteria() ) {
				sqlAgg(buf);
			} else {
				sqlRaw(buf);
			}
			buf.append(") AS q");
			return buf.toString();
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement stmt = con.prepareStatement(getSql());
			prepareWhere(stmt, 0);
			return stmt;
		}

	}
}
