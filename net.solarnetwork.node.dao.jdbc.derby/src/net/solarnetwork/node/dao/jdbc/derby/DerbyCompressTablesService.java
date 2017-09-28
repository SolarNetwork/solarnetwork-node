/* ==================================================================
 * DerbyCompressTablesService.java - 30/09/2016 6:16:34 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

/**
 * A service to inspect available tables and perform a Derby in-place compress
 * on them to free up disk space.
 * 
 * @author matt
 * @version 1.1
 * @see DerbyFullCompressTablesService for a more agressive alternative to this
 *      service
 */
public class DerbyCompressTablesService implements TablesMaintenanceService {

	private static final String COMPRESS_CALL = "CALL SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE(?, ?, ?, ?, ?)";

	private JdbcOperations jdbcOperations;
	private Set<String> schemas = Collections.singleton("solarnode");
	private boolean purgeRows = true;
	private boolean defragmentRows = true;
	private boolean truncateEnd = true;
	private int maxSeconds = 120;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Inspect and compress appropriate tables, optionally starting after a
	 * specific table key.
	 * 
	 * This method is designed to exit early if compressing tables is taking
	 * more than {@code maxSeconds}. If not all tables can be compressed within
	 * that time, it will return a key value that can later be passed as the
	 * {@code startAfterKey} argument to this method, to essentially pick up
	 * where the previous invocation left off.
	 * 
	 * @param startAfterKey
	 *        A {@code key} returned from a previous execution of this method,
	 *        to start compressing tables from, or {@code null} to start at the
	 *        first available table.
	 * @return A {@code key} for the last table processed, or {@code null} if
	 *         all tables were processed.
	 */
	@Override
	public String processTables(final String startAfterKey) {
		return jdbcOperations.execute(new ConnectionCallback<String>() {

			@Override
			public String doInConnection(Connection con) throws SQLException, DataAccessException {
				Map<String, Set<String>> candidateMap = candidateTableMap(con);

				if ( log.isTraceEnabled() ) {
					log.trace("Preparing Derby compress table call [" + COMPRESS_CALL + ']');
				}
				CallableStatement stmt = con.prepareCall(COMPRESS_CALL);

				final long expireTime = (maxSeconds > 0
						? System.currentTimeMillis() + (maxSeconds * 1000) : 0);

				String lastKey = null;
				try {
					while ( !candidateMap.isEmpty() && !pastTime(expireTime) ) {
						for ( Iterator<Map.Entry<String, Set<String>>> entryItr = candidateMap.entrySet()
								.iterator(); entryItr.hasNext() && !pastTime(expireTime); ) {
							Map.Entry<String, Set<String>> me = entryItr.next();
							String schema = me.getKey();
							Set<String> tables = me.getValue();
							for ( Iterator<String> tableItr = tables.iterator(); tableItr.hasNext()
									&& !pastTime(expireTime); ) {
								String table = tableItr.next();
								String key = keyForTable(schema, table);
								if ( startAfterKey != null && lastKey == null ) {
									if ( startAfterKey.equals(key) ) {
										lastKey = key;
									}
									continue;
								}
								log.debug("Compressing table {}", key);
								compressTable(stmt, schema, table);
								tableItr.remove();
								lastKey = key;
							}
							if ( tables.isEmpty() ) {
								// finished all tables in this schema
								entryItr.remove();
							}
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

				return (candidateMap.isEmpty() ? null : lastKey);
			}
		});
	}

	private boolean pastTime(long expireTime) {
		return (expireTime != 0 && System.currentTimeMillis() > expireTime);
	}

	private void compressTable(CallableStatement stmt, String schema, String table) throws SQLException {
		int idx = 1;
		stmt.setString(idx++, schema);
		stmt.setString(idx++, table);
		stmt.setShort(idx++, purgeRows ? (short) 1 : (short) 0);
		stmt.setShort(idx++, defragmentRows ? (short) 1 : (short) 0);
		stmt.setShort(idx++, truncateEnd ? (short) 1 : (short) 0);
		stmt.execute();
	}

	private String keyForTable(String schema, String table) {
		return schema + "." + table;
	}

	private Map<String, Set<String>> candidateTableMap(Connection con) throws SQLException {
		Map<String, Set<String>> candidateMap = new LinkedHashMap<String, Set<String>>(16);
		DatabaseMetaData dbMeta = con.getMetaData();
		ResultSet rs = null;
		try {
			rs = dbMeta.getTables(null, null, null, null);
			while ( rs.next() ) {
				String schema = rs.getString(2);
				if ( schemas != null && (schema == null || !schemas.contains(schema.toLowerCase())) ) {
					continue;
				}
				String table = rs.getString(3);
				String type = rs.getString(4);
				if ( !"TABLE".equalsIgnoreCase(type) ) {
					continue;
				}
				if ( schema == null ) {
					schema = "";
				}
				log.debug("Found table compress candidate {}.{}", schema, table);
				Set<String> tables = candidateMap.get(schema);
				if ( tables == null ) {
					tables = new LinkedHashSet<String>(16);
					candidateMap.put(schema, tables);
				}
				tables.add(table);
			}
			return candidateMap;
		} finally {
			if ( rs != null ) {
				try {
					rs.close();
				} catch ( SQLException e ) {
					// ignore this
				}
			}
		}
	}

	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	public void setPurgeRows(boolean purgeRows) {
		this.purgeRows = purgeRows;
	}

	public void setDefragmentRows(boolean defragmentRows) {
		this.defragmentRows = defragmentRows;
	}

	public void setTruncateEnd(boolean truncateEnd) {
		this.truncateEnd = truncateEnd;
	}

	public void setSchemas(Set<String> schemas) {
		if ( schemas != null && !schemas.isEmpty() ) {
			Set<String> lcSchemas = new LinkedHashSet<String>(schemas.size());
			for ( String s : schemas ) {
				lcSchemas.add(s.toLowerCase());
			}
			schemas = Collections.unmodifiableSet(lcSchemas);
		}
		this.schemas = schemas;
	}

	public void setMaxSeconds(int maxSeconds) {
		this.maxSeconds = maxSeconds;
	}

}
