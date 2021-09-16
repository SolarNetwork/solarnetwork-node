/* ==================================================================
 * DerbyDatabaseSystemService.java - Jul 30, 2017 3:54:28 PM
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.dao.jdbc.DatabaseSystemService;

/**
 * Embedded Derby implementation of {@link DatabaseSystemService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DerbyDatabaseSystemService implements DatabaseSystemService {

	private static final String EMBEDDED_DERBY_JDBC_PREFIX = "jdbc:derby:";

	private JdbcOperations jdbcOperations;
	private TablesMaintenanceService compressTablesService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the file system the Derby database is located on.
	 * 
	 * <p>
	 * This method extracts the path provided on the configured JDBC
	 * connection's URL. The URL is assumed to take the form
	 * <code>jdbc:derby:<em>path</em></code> where <em>path</em> is the path to
	 * the database files. The {@literal derby.system.home} system property is
	 * supported, and if defined serves as a path prefix for the URL
	 * <em>path</em>.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public File[] getFileSystemRoots() {
		File root = jdbcOperations.execute(new ConnectionCallback<File>() {

			@Override
			public File doInConnection(Connection conn) throws SQLException, DataAccessException {
				return dbDir(conn);
			}
		});
		return (root == null ? new File[0] : new File[] { root });
	}

	private File dbDir(Connection conn) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		String url = meta.getURL();
		assert url.startsWith(EMBEDDED_DERBY_JDBC_PREFIX);
		url = url.substring(EMBEDDED_DERBY_JDBC_PREFIX.length(), url.length());

		// look for system.home prop
		String dbHome = System.getProperty("derby.system.home");
		if ( dbHome != null ) {
			url = dbHome + File.separatorChar + url;
		}

		File dir = new File(url);
		return dir;
	}

	/**
	 * Get the SQL query that returns the size of a single database table on
	 * disk.
	 * 
	 * <p>
	 * This method returns the contents of the
	 * {@literal find-table-disk-size.sql} resource, which is expected to accept
	 * schema and table name parameters, returning a integer column that
	 * contains the size that table occupies on disk, in bytes.
	 * </p>
	 * 
	 * @return the SQL
	 */
	private String tableFileSystemSizeQuery() {
		try {
			String sql = FileCopyUtils.copyToString(new InputStreamReader(
					getClass().getResourceAsStream("find-table-disk-size.sql"), "UTF-8"));
			return sql;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long tableFileSystemSize(final String schemaName, final String tableName) {
		return jdbcOperations.query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
				String sql = tableFileSystemSizeQuery();
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setString(1, schemaName);
				stmt.setString(2, tableName);
				return stmt;
			}
		}, new ResultSetExtractor<Long>() {

			@Override
			public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
				long result = 0;
				if ( rs.next() ) {
					result = rs.getLong(1);
					log.debug("Found table {}.{} has disk size {}", schemaName, tableName, result);
				}
				return result;
			}
		});
	}

	/**
	 * Compress Derby database tables via a configured
	 * {@link TablesMaintenanceService}.
	 * 
	 * <p>
	 * This method simply calls
	 * {@link TablesMaintenanceService#processTables(String)} on the configured
	 * {@code compressTablesService}. It assumes that service will examine the
	 * tables and compress those that need it.
	 * 
	 * {@inheritDoc}
	 * 
	 * @see DerbyFullCompressTablesService as an example service that can be
	 *      configured
	 */
	@Override
	public void vacuumTable(String schemaName, String tableName) {
		// we ignore the schema / table name and simply try to compress all potential tables
		TablesMaintenanceService service = compressTablesService;
		if ( service == null ) {
			return;
		}
		service.processTables(null);
	}

	/**
	 * Set the JDBC operations to use for accessing the Derby database.
	 * 
	 * <p>
	 * This operations must be configured with a connection to the Derby
	 * database that is to be managed.
	 * </p>
	 * 
	 * @param jdbcOperations
	 *        the JDBC operations to use
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	/**
	 * Set a service to use to help reclaim disk space from Derby.
	 * 
	 * <p>
	 * This service is used by the {@link #vacuumTable(String, String)} method,
	 * and is assumed to compress database tables in the same database as
	 * configured via {@link #setJdbcOperations(JdbcOperations)}.
	 * </p>
	 * 
	 * @param compressTablesService
	 *        the service to use
	 * @see DerbyFullCompressTablesService as an example service that can be
	 *      configured
	 */
	public void setCompressTablesService(TablesMaintenanceService compressTablesService) {
		this.compressTablesService = compressTablesService;
	}

}
