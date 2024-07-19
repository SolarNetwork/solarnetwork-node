/* ==================================================================
 * TestDbUtils.java - 11/04/2022 4:37:28 PM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.test;

import static java.util.stream.Collectors.joining;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Test helper methods for DB functions.
 *
 * @author matt
 * @version 1.1
 */
public class TestDbUtils {

	/**
	 * Setup Derby system functions used in SolarNode.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 */
	public static void setupDerbyFunctions(JdbcOperations jdbcOps) {
		jdbcOps.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				final String sqlTemplate = "CREATE FUNCTION %s.%s( parm1 INTEGER, param2 INTEGER ) "
						+ "RETURNS INTEGER LANGUAGE JAVA DETERMINISTIC PARAMETER STYLE JAVA NO SQL "
						+ "EXTERNAL NAME 'net.solarnetwork.node.test.DerbyBitwiseFunctions.%s'";
				String sql = String.format(sqlTemplate, "SOLARNODE", "BITWISE_AND", "bitwiseAnd");
				con.createStatement().execute(sql);
				sql = String.format(sqlTemplate, "SOLARNODE", "BITWISE_OR", "bitwiseOr");
				con.createStatement().execute(sql);
				return null;
			}
		});

	}

	/**
	 * Get the raw content of a table.
	 *
	 * @param log
	 *        the logger to log to
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param table
	 *        the name of the table
	 * @param order
	 *        the order column expression
	 * @return the rows
	 * @since 1.1
	 */
	public static List<Map<String, Object>> allTableData(Logger log, JdbcOperations jdbcOps,
			String table, String order) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList(String.format("SELECT * FROM %s ORDER BY %s", table, order));
		log.debug(String.format("%s table has {} items: [{}]", table), data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
