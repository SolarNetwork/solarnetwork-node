/* ==================================================================
 * ResetSQLExceptionHandler.java - 22/12/2022 8:36:57 am
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

package net.solarnetwork.node.dao.jdbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Reset a database by deleting and exiting the application.
 *
 * <p>
 * This class currently supports H2 databases only.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 2.5
 */
public class ResetSQLExceptionHandler extends AbstractSQLExceptionHandler {

	/**
	 * Constructor.
	 */
	public ResetSQLExceptionHandler() {
		super();
	}

	@Override
	public void handleGetConnectionException(SQLException e) {
		// require DataSource
	}

	@Override
	public void handleConnectionException(Connection conn, SQLException e) {
		// require DataSource
	}

	@Override
	public void handleConnectionException(DataSource dataSource, Connection conn, SQLException e) {
		handleGetConnectionException(dataSource, e);
	}

	@Override
	public void handleGetConnectionException(DataSource dataSource, SQLException e) {
		SQLException root = exceptionMatchingSqlStatePattern(e);
		if ( root == null ) {
			return;
		}
		try {
			// get URL from H2 DataSource
			if ( dataSource.isWrapperFor(org.h2.jdbcx.JdbcDataSource.class) ) {
				org.h2.jdbcx.JdbcDataSource h2ds = dataSource.unwrap(org.h2.jdbcx.JdbcDataSource.class);
				String url = h2ds.getUrl();
				log.info("Discovered corrupted database URL: {}", url);
				int split = url.lastIndexOf(':');
				if ( split >= 0 ) {
					String path = url.substring(split + 1);
					Path p = Paths.get(path);
					Path dir = p.getParent();
					if ( Files.isDirectory(dir) ) {
						String name = p.getFileName().toString();
						Path dest = Files.createDirectory(
								dir.resolve(name + "-reset." + System.currentTimeMillis()));
						Files.createDirectories(dest);
						Files.list(dir).filter(f -> {
							return Files.isRegularFile(f) && f.getFileName().toString().startsWith(name);
						}).forEach(f -> {
							Path to = dest.resolve(f.getFileName());
							try {
								Files.move(f, to);
							} catch ( IOException e1 ) {
								log.warn("Error moving {} -> {}: {}", f, to, e1);
							}
						});
					}
				}
			}
		} catch ( Throwable e2 ) {
			// bail
			log.warn("Unable to reset database from exception [{}]", e.toString(), e2);
		}
		shutdown(root.getMessage());
	}

	private void shutdown(String msg) {
		log.error("Shutting down now due to database corruption error: {}", msg);
		// graceful would be bundleContext.getBundle(0).stop();, but we don't need to wait for that here
		System.exit(1);
	}

}
