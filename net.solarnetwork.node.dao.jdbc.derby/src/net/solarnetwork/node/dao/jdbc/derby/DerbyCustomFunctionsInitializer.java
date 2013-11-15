/* ==================================================================
 * DerbyCustomFunctionsInitializer.java - Nov 15, 2013 3:53:52 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Register custom functions in the Derby database.
 * 
 * @author matt
 * @version 1.0
 */
public class DerbyCustomFunctionsInitializer {

	private JdbcOperations jdbcOperations;

	private static final String BITWISE_AND = "BITWISE_AND";
	private static final String BITWISE_OR = "BITWISE_OR";

	// Note: I wanted to put this method in DerbyBitwiseFunctions itself, but PDE requires
	// the fragement host to include Eclipse-ExtensibleAPI: true or else the classpath fails
	private static void registerBitwiseFunctions(final Connection con, String schema)
			throws SQLException {
		DatabaseMetaData dbMeta = con.getMetaData();
		ResultSet rs = dbMeta.getFunctions(null, null, null);
		Set<String> functionNames = new HashSet<String>(Arrays.asList(BITWISE_AND, BITWISE_OR));
		while ( rs.next() ) {
			String schemaName = rs.getString(2);
			String functionName = rs.getString(3).toUpperCase();
			if ( schema.equalsIgnoreCase(schemaName) && functionNames.contains(functionName) ) {
				functionNames.remove(functionName);
			}
		}

		// at this point, functionNames contains the functions we need to create
		if ( functionNames.size() > 0 ) {
			final String sqlTemplate = "CREATE FUNCTION %s.%s( parm1 INTEGER, param2 INTEGER ) "
					+ "RETURNS INTEGER LANGUAGE JAVA DETERMINISTIC PARAMETER STYLE JAVA NO SQL "
					+ "EXTERNAL NAME 'net.solarnetwork.node.dao.jdbc.derby.ext.DerbyBitwiseFunctions.%s'";
			if ( functionNames.contains(BITWISE_AND) ) {
				final String sql = String.format(sqlTemplate, schema, BITWISE_AND, "bitwiseAnd");
				con.createStatement().execute(sql);
			}
			if ( functionNames.contains(BITWISE_OR) ) {
				final String sql = String.format(sqlTemplate, schema, BITWISE_OR, "bitwiseOr");
				con.createStatement().execute(sql);
			}
		}
	}

	public void init() {
		jdbcOperations.execute(new ConnectionCallback<Object>() {

			@Override
			public Object doInConnection(Connection con) throws SQLException, DataAccessException {
				registerBitwiseFunctions(con, "solarnode");
				return null;
			}
		});
	}

	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

}
