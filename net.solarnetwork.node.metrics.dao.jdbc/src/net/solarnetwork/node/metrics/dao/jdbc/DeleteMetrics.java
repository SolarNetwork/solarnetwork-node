/* ==================================================================
 * DeleteMetrics.java - 15/07/2024 4:36:03 pm
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

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.node.metrics.dao.MetricFilter;

/**
 * Generate {@code DELETE} SQL for metric values based on a filter.
 *
 * @author matt
 * @version 1.2
 */
public class DeleteMetrics implements PreparedStatementCreator, SqlProvider {

	private final MetricFilter filter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DeleteMetrics(MetricFilter filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("DELETE FROM solarnode.mtr_metric\n");
		sqlWhere(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		prepareWhere(con, stmt, 0);
		return stmt;
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

	private int prepareWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasStartDate() ) {
			stmt.setObject(++p, filter.startDate().atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		}
		if ( filter.hasEndDate() ) {
			stmt.setObject(++p, filter.endDate().atOffset(UTC), Types.TIMESTAMP_WITH_TIMEZONE);
		}
		if ( filter.hasTypeCriteria() ) {
			Array a = con.createArrayOf("VARCHAR", filter.types());
			stmt.setArray(++p, a);
			a.free();
		}
		if ( filter.hasNameCriteria() ) {
			Array a = con.createArrayOf("VARCHAR", filter.names());
			stmt.setArray(++p, a);
			a.free();
		}
		return p;
	}

}
