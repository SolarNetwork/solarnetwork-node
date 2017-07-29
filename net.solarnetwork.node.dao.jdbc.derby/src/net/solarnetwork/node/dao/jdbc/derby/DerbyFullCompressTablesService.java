/* ==================================================================
 * DerbyFullCompressTablesService.java - Jul 29, 2017 2:08:08 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.FileCopyUtils;

/**
 * A service to inspect available tables and perform a Derby compress on them to
 * free up disk space.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
public class DerbyFullCompressTablesService implements TablesMaintenanceService {

	private static final String COMPRESS_CALL = "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)";

	private JdbcOperations jdbcOperations;
	private Set<String> schemas = Collections.singleton("SOLARNODE");
	private boolean sequential = true;
	private int minEstimatedSpaceSaving = 0;
	private int maxSeconds = 120;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public String processTables(final String startAfterKey) {
		return jdbcOperations.execute(new ConnectionCallback<String>() {

			@Override
			public String doInConnection(Connection con) throws SQLException, DataAccessException {
				con.setAutoCommit(true); // recommended by Derby

				Map<String, Set<String>> candidateMap = findCandidateTables(con);

				log.trace("Preparing Derby compress table call [{}]", COMPRESS_CALL);
				CallableStatement stmt = con.prepareCall(COMPRESS_CALL);

				final long expireTime = (maxSeconds > 0
						? System.currentTimeMillis() + (maxSeconds * 1000) : 0);

				try {
					for ( Iterator<Map.Entry<String, Set<String>>> entryItr = candidateMap.entrySet()
							.iterator(); entryItr.hasNext() && !pastTime(expireTime); ) {
						Map.Entry<String, Set<String>> me = entryItr.next();
						String schema = me.getKey();
						Set<String> tables = me.getValue();
						for ( Iterator<String> tableItr = tables.iterator(); tableItr.hasNext()
								&& !pastTime(expireTime); ) {
							String table = tableItr.next();
							compressTable(stmt, schema, table);
						}
					}
				} finally {
					if ( stmt != null ) {
						try {
							stmt.close();
						} catch ( SQLException e ) {
							// ignore
						}
					}
				}

				return null;
			}

		});
	}

	private Map<String, Set<String>> findCandidateTables(Connection con) throws SQLException {
		Map<String, Set<String>> candidateMap = new LinkedHashMap<>();

		PreparedStatement findTablesStmt = findTablesStatement(con);
		ResultSet rs = null;

		try {
			for ( String schema : schemas ) {
				findTablesStmt.setString(1, schema);
				findTablesStmt.setInt(2, minEstimatedSpaceSaving);
				rs = findTablesStmt.executeQuery();

				while ( rs.next() ) {
					String schemaName = rs.getString(1);
					String table = rs.getString(2);
					long savings = rs.getLong(3);
					log.debug("Found table {}.{} with estimated potential savings {}", schemaName, table,
							savings);
					Set<String> tables = candidateMap.get(schemaName);
					if ( tables == null ) {
						tables = new LinkedHashSet<>();
						candidateMap.put(schemaName, tables);
					}
					tables.add(table);
				}
			}
		} finally {
			if ( rs != null ) {
				rs.close();
			}
			if ( findTablesStmt != null ) {
				findTablesStmt.close();
			}
		}
		return candidateMap;
	}

	private boolean pastTime(long expireTime) {
		return (expireTime != 0 && System.currentTimeMillis() > expireTime);
	}

	private void compressTable(CallableStatement stmt, String schema, String table) throws SQLException {
		log.info("Compressing table {}.{} to free disk space", schema, table);
		int idx = 1;
		stmt.setString(idx++, schema);
		stmt.setString(idx++, table);
		stmt.setShort(idx++, sequential ? (short) 1 : (short) 0);
		stmt.execute();
	}

	private String findTablesQuery() {
		try {
			String sql = FileCopyUtils.copyToString(new InputStreamReader(
					getClass().getResourceAsStream("find-tables-spacesaving.sql"), "UTF-8"));
			return sql;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private PreparedStatement findTablesStatement(Connection con) throws SQLException {
		final String querySql = findTablesQuery();
		return con.prepareStatement(querySql);
	}

	/**
	 * Set the {@link JdbcOperations} to use for accessing the database.
	 * 
	 * @param jdbcOperations
	 *        the operations
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	/**
	 * Set the database schema names to limit compressing tables within.
	 * 
	 * This defaults to a singleton set of just {@code solarnode}.
	 * 
	 * @param schemas
	 *        the schemas to consider compressing tables within
	 */
	public void setSchemas(Set<String> schemas) {
		if ( schemas != null && !schemas.isEmpty() ) {
			Set<String> ciSchemas = new LinkedHashSet<>(schemas.size());
			for ( String s : schemas ) {
				ciSchemas.add(s.toUpperCase());
			}
			schemas = Collections.unmodifiableSet(ciSchemas);
		}
		this.schemas = schemas;
	}

	/**
	 * Set the {@code sequential} mode of the Derby
	 * {@code SYSCS_UTIL.SYSCS_COMPRESS_TABLE} procedure.
	 * 
	 * This is enabled by default, as it requires less resources overall to
	 * complete (at the expense of additional time).
	 * 
	 * @param sequential
	 *        {@literal true} to run in sequential mode
	 */
	public void setSequential(boolean sequential) {
		this.sequential = sequential;
	}

	/**
	 * Set a maximum number of seconds to allow the call to
	 * {@link #processTables(String)} to run for.
	 * 
	 * If the call to {@link #processTables(String)} takes more than this
	 * amount, it will finish compressing the table it happens to be compressing
	 * and then not attempt any more. Note that the method can thus take longer
	 * than this amount of time.
	 * 
	 * @param maxSeconds
	 *        the maximum amount of seconds to run before giving up compressing
	 *        additional tables
	 */
	public void setMaxSeconds(int maxSeconds) {
		this.maxSeconds = maxSeconds;
	}

	/**
	 * Set a minimum estimated space saving value to query for.
	 * 
	 * Only tables that have <b>more than</b> this much estimated savings will
	 * be compressed. The default value is {@code 0} so tables without any
	 * estimated savings will be skipped.
	 * 
	 * @param minEstimatedSpaceSaving
	 *        the minimum space saving estimate
	 */
	public void setMinEstimatedSpaceSaving(int minEstimatedSpaceSaving) {
		this.minEstimatedSpaceSaving = minEstimatedSpaceSaving;
	}

}
