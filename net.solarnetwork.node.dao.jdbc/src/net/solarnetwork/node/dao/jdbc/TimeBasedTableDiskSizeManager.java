/* ==================================================================
 * TimeBasedTableDiskSizeManager.java - Jul 30, 2017 7:59:53 AM
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

package net.solarnetwork.node.dao.jdbc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.util.OptionalService;

/**
 * Service that deletes rows from a database table when disk space is running
 * low.
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.19
 */
public class TimeBasedTableDiskSizeManager {

	private JdbcOperations jdbcOperations;
	private OptionalService<DatabaseSystemService> dbSystemService;
	private String schemaName = "SOLARNODE";
	private String tableName = "SN_GENERAL_NODE_DATUM";
	private String dateColumnName = "CREATED";
	private float maxFilesystemUseThreshold = 90.0f;
	private long minTableSizeThreshold = (1024 * 1024); // 1MB
	private int trimMinutes = 90;

	private static final String OLDEST_DATE_QUERY_TEMPLATE = "SELECT MIN(%s) FROM %s";
	private static final String DELETE_BY_DATE_QUERY_TEMPLATE = "DELETE FROM %s WHERE %s < ?";

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Examine the configured database to see if disk space is low and if so
	 * delete a set of "oldest" rows based on a date column in the configured
	 * table.
	 */
	public void performMaintenance() {
		final DatabaseSystemService dbService = (dbSystemService != null ? dbSystemService.service()
				: null);
		if ( dbService == null ) {
			log.debug("No DatabaseSystemService available");
			return;
		}

		// get usable disk space for db system
		File[] dbRoots = dbService.getFileSystemRoots();
		URI rootURI;
		try {
			rootURI = new URI("file:///");
		} catch ( URISyntaxException e ) {
			throw new RuntimeException(e);
		}
		Path rootPath = Paths.get(rootURI);

		for ( File f : dbRoots ) {
			try {
				Path dirPath = rootPath.resolve(f.getAbsolutePath());
				FileStore store = Files.getFileStore(dirPath);

				long totalSpace = store.getTotalSpace();
				long usableSpace = store.getUsableSpace();
				float percentFull = (float) (100 * (1.0 - (usableSpace / (double) totalSpace)));
				log.debug("Database filesystem {} {}% capacity ({} available out of {})", store.name(),
						percentFull, usableSpace, totalSpace);
				if ( percentFull >= maxFilesystemUseThreshold ) {
					deleteDataIfPossible(dbService);
					return; // only execute once per table, assuming all file systems will be impacted
				}

			} catch ( IOException e ) {
				log.error("Error examining disk use for database root {}", f, e);
			}
		}
	}

	private void deleteDataIfPossible(DatabaseSystemService dbService) {
		long diskSize = dbService.tableFileSystemSize(schemaName, tableName);
		log.debug("Database table {}.{} consumes {} on disk", schemaName, tableName, diskSize);
		if ( diskSize < minTableSizeThreshold ) {
			return;
		}
		jdbcOperations.execute(new ConnectionCallback<Object>() {

			@Override
			public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
				DatabaseMetaData meta = conn.getMetaData();

				ResultSet dateColRs = null;
				try {
					dateColRs = meta.getColumns(null, schemaName, tableName, dateColumnName);
					if ( !dateColRs.next() ) {
						log.error("Date column {} not found on table {}.{}; cannot trim data",
								dateColumnName, schemaName, tableName);
						return null;
					}
				} finally {
					if ( dateColRs != null ) {
						dateColRs.close();
					}
				}

				final String fullTableName = (schemaName == null ? tableName
						: schemaName + '.' + tableName);

				// get oldest available date from table
				Timestamp oldestDate = findOldestDate(conn, fullTableName);
				if ( oldestDate == null ) {
					log.debug("Oldest date not found on table {}.{}; cannot trim data", schemaName,
							tableName);
					return null;
				}

				Timestamp deleteDate = new Timestamp(
						oldestDate.getTime() + TimeUnit.MINUTES.toMillis(trimMinutes));
				int deleted = deleteOlderThan(conn, fullTableName, deleteDate);
				log.info("Trimmed {} rows from {} older than {} to free space", deleted, fullTableName,
						deleteDate);
				return null;
			}

		});

		// now that we've deleted data, perform a vacuum to reclaim space if possible
		dbService.vacuumTable(schemaName, tableName);
	}

	private Timestamp findOldestDate(final Connection conn, final String fullTableName)
			throws SQLException {
		String oldestDateSql = String.format(OLDEST_DATE_QUERY_TEMPLATE, dateColumnName, fullTableName);

		Timestamp oldestDate = null;
		PreparedStatement oldestDateStmt = null;
		ResultSet oldestDateRs = null;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		try {
			oldestDateStmt = conn.prepareStatement(oldestDateSql);
			oldestDateRs = oldestDateStmt.executeQuery();
			if ( oldestDateRs.next() ) {
				oldestDate = oldestDateRs.getTimestamp(1, cal);
			}
		} finally {
			if ( oldestDateRs != null ) {
				oldestDateRs.close();
			}
			if ( oldestDateStmt != null ) {
				oldestDateStmt.close();
			}
		}
		return oldestDate;
	}

	private int deleteOlderThan(final Connection conn, final String fullTableName, final Timestamp date)
			throws SQLException {
		log.debug("Trimming rows from {} older than {} to free space", fullTableName, date);
		String oldestDateSql = String.format(DELETE_BY_DATE_QUERY_TEMPLATE, fullTableName,
				dateColumnName);

		PreparedStatement deleteStmt = null;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		try {
			deleteStmt = conn.prepareStatement(oldestDateSql);
			deleteStmt.setTimestamp(1, date, cal);
			int count = deleteStmt.executeUpdate();
			return count;
		} finally {
			if ( deleteStmt != null ) {
				deleteStmt.close();
			}
		}
	}

	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	public void setDbSystemService(OptionalService<DatabaseSystemService> dbSystemService) {
		this.dbSystemService = dbSystemService;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setDateColumnName(String dateColumnName) {
		this.dateColumnName = dateColumnName;
	}

	public void setMaxFilesystemUseThreshold(float maxFilesystemUseThreshold) {
		this.maxFilesystemUseThreshold = maxFilesystemUseThreshold;
	}

	public void setMinTableSizeThreshold(long minTableSizeThreshold) {
		this.minTableSizeThreshold = minTableSizeThreshold;
	}

	public void setTrimMinutes(int trimMinutes) {
		this.trimMinutes = trimMinutes;
	}

}
