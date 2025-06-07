/* ==================================================================
 * PostgresDatabaseSystemService.java - 5/06/2025 10:52:07 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.postgres;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import net.solarnetwork.node.dao.jdbc.DatabaseSystemService;

/**
 * Postgres implementation of {@link DatabaseSystemService}.
 *
 * @author matt
 * @version 1.0
 */
public class PostgresDatabaseSystemService implements DatabaseSystemService {

	private static final String SQL_TABLE_DISK_SIZE = "SELECT pg_total_relation_size(?)";

	private static final String SQL_TABLE_VACUUM = "VACUUM %s.%s";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Collection<DataSource> dataSources;

	/**
	 * Constructor.
	 *
	 * @param dataSources
	 *        the list of data sources to support
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public PostgresDatabaseSystemService(Collection<DataSource> dataSources) {
		super();
		this.dataSources = requireNonNullArgument(dataSources, "dataSources");
	}

	@Override
	public File[] getFileSystemRoots() {
		return new File[0];
	}

	@Override
	public long tableFileSystemSize(String schemaName, String tableName) {
		final Set<String> urls = new HashSet<>();
		long result = 0;
		for ( DataSource dataSource : dataSources ) {
			final JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
			try {
				result += jdbcOps.execute((ConnectionCallback<Long>) conn -> {
					String url = conn.getMetaData().getURL();
					if ( urls.contains(url) ) {
						return 0L;
					}
					urls.add(url);
					try (PreparedStatement stmt = conn.prepareStatement(SQL_TABLE_DISK_SIZE)) {
						stmt.setString(1, String.format("%s.%s", schemaName, tableName));
						try (ResultSet rs = stmt.executeQuery()) {
							if ( rs.next() ) {
								long size = rs.getLong(1);
								log.debug("Found table {}.{} has disk size {}", schemaName, tableName,
										size);
								return size;
							}
						}
					}

					return 0L;
				});
			} catch ( DataAccessException e ) {
				log.debug("Ignoring DB exception calculating disk size of database table {}.{}: {}",
						schemaName, tableName, e.toString());
			}
		}
		return result;
	}

	@Override
	public void vacuumTable(String schemaName, String tableName) {
		final Set<String> urls = new HashSet<>();
		for ( DataSource dataSource : dataSources ) {
			final JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
			try {
				jdbcOps.execute((ConnectionCallback<Void>) conn -> {
					String url = conn.getMetaData().getURL();
					if ( urls.contains(url) ) {
						return null;
					}
					urls.add(url);
					try (PreparedStatement stmt = conn
							.prepareStatement(String.format(SQL_TABLE_VACUUM, schemaName, tableName))) {
						stmt.execute();
						log.debug("Vacuumed table {}.{}", schemaName, tableName);
					}

					return null;
				});
			} catch ( DataAccessException e ) {
				log.warn("Ignoring DB exception vacuuming database table {}.{}: {}", schemaName,
						tableName, e.toString());
			}
		}
	}

}
