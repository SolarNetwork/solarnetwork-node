/* ==================================================================
 * JdbcUtils.java - 14/07/2024 11:22:24 am
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * JDBC utility methods.
 *
 * @author matt
 * @version 1.0
 */
public final class JdbcUtils {

	private JdbcUtils() {
		// not available
	}

	/**
	 * Get an {@Link Instant} from a JDBC result set.
	 *
	 * @param rs
	 *        the result set
	 * @param col
	 *        the column number
	 * @return the instance, or {@literal null}
	 * @throws SQLException
	 *         if the column cannot be treated as an instant
	 */
	public static Instant instantColumn(ResultSet rs, int col) throws SQLException {
		try {
			return rs.getObject(col, Instant.class);
		} catch ( SQLException e ) {
			Timestamp ts = rs.getTimestamp(col);
			if ( ts != null ) {
				return ts.toInstant();
			}
		}
		return null;
	}

}
