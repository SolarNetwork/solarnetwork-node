/* ==================================================================
 * H2DatabaseSystemService.java - 20/04/2022 10:16:18 AM
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

package net.solarnetwork.node.dao.jdbc.h2;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import net.solarnetwork.node.dao.jdbc.DatabaseSystemService;

/**
 * H2 implementation of {@link DatabaseSystemService}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class H2DatabaseSystemService implements DatabaseSystemService {

	private static final String SQL_TABLE_DISK_SIZE = "CALL DISK_SPACE_USED(?)";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Collection<DataSource> dataSources;

	/**
	 * Constructor.
	 * 
	 * @param dataSources
	 *        the list of data sources to support
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public H2DatabaseSystemService(Collection<DataSource> dataSources) {
		super();
		this.dataSources = requireNonNullArgument(dataSources, "dataSources");
	}

	@Override
	public File[] getFileSystemRoots() {
		Set<File> roots = new LinkedHashSet<>(2);
		for ( DataSource dataSource : dataSources ) {
			final JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
			File root = jdbcOps.execute(new ConnectionCallback<File>() {

				@Override
				public File doInConnection(Connection conn) throws SQLException, DataAccessException {
					return dbDir(conn);
				}
			});
			if ( root != null ) {
				roots.add(root);
			}
		}
		return roots.toArray(new File[roots.size()]);
	}

	private File dbDir(Connection conn) throws SQLException {
		final DatabaseMetaData meta = conn.getMetaData();
		final String url = meta.getURL();
		final String dbPath = H2Utils.h2DatabasePath(url);
		if ( dbPath == null || dbPath.indexOf(File.separatorChar) < 0 ) {
			return null;
		}

		return Paths.get(dbPath).getParent().toFile();
	}

	@Override
	public long tableFileSystemSize(String schemaName, String tableName) {
		long result = 0;
		for ( DataSource dataSource : dataSources ) {
			final JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
			try {
				long size = jdbcOps.query(new PreparedStatementCreator() {

					@Override
					public PreparedStatement createPreparedStatement(Connection conn)
							throws SQLException {
						PreparedStatement stmt = conn.prepareStatement(SQL_TABLE_DISK_SIZE);
						stmt.setString(1, String.format("%s.%s", schemaName, tableName));
						return stmt;
					}
				}, new ResultSetExtractor<Long>() {

					@Override
					public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
						long result = 0;
						if ( rs.next() ) {
							result = rs.getLong(1);
							log.debug("Found table {}.{} has disk size {}", schemaName, tableName,
									result);
						}
						return result;
					}
				});
				result += size;
			} catch ( DataAccessException e ) {
				log.debug("Ignoring DB exception calculating disk size of database {}.{}: {}",
						schemaName, tableName, e.toString());
			}
		}
		return result;
	}

	@Override
	public void vacuumTable(String schemaName, String tableName) {
		// no-op
	}

}
