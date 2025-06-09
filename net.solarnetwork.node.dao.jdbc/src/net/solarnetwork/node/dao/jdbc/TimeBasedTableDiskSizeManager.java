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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * Service that deletes rows from a database table when disk space is running
 * low.
 *
 * <p>
 * This service is designed so that {@link #executeJobService()} can be called
 * periodically to check if rows need to be deleted, based on the thresholds
 * configured. The main threshold is the file system available capacity. When
 * the file system use exceeds {@code maxFileSystemUseThreshold} then this
 * service will attempt to delete a subset of the oldest available data from a
 * single configured table. After deleting data,
 * {@link DatabaseSystemService#vacuumTable(String, String)} will be called so
 * that the database can attempt to release file system space back to the
 * operating system.
 * </p>
 *
 * <p>
 * <b>Note</b> that the {@code dateColumnName} time stamp column is assumed to
 * store dates in the {@literal UTC} time zone.
 * </p>
 *
 * @author matt
 * @version 2.1
 * @since 1.19
 */
public class TimeBasedTableDiskSizeManager extends BaseIdentifiable implements JobService {

	private final JdbcOperations jdbcOperations;
	private OptionalService<DatabaseSystemService> dbSystemService;
	private String schemaName = "SOLARNODE";
	private String tableName = "SN_GENERAL_NODE_DATUM";
	private String dateColumnName = "CREATED";
	private float maxFileSystemUseThreshold = 90.0f;
	private long minTableSizeThreshold = (1024 * 1024); // 1MB
	private int trimMinutes = 90;

	private static final String OLDEST_DATE_QUERY_TEMPLATE = "SELECT MIN(%s) FROM %s";
	private static final String DELETE_BY_DATE_QUERY_TEMPLATE = "DELETE FROM %s WHERE %s < ?";

	/**
	 * Constructor.
	 *
	 * @param jdbcOperations
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TimeBasedTableDiskSizeManager(JdbcOperations jdbcOperations) {
		super();
		this.jdbcOperations = ObjectUtils.requireNonNullArgument(jdbcOperations, "jdbcOperations");
	}

	@Override
	public String getSettingUid() {
		String uid = getUid();
		return (uid != null && !uid.isEmpty() ? uid
				: "net.solarnetwork.node.dao.jdbc.TimeBasedTableDiskSizeManager");
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
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
				if ( percentFull >= maxFileSystemUseThreshold ) {
					long diskSize = dbService.tableFileSystemSize(schemaName, tableName);
					log.debug("Database table {}.{} consumes {} on disk", schemaName, tableName,
							diskSize);
					if ( diskSize >= minTableSizeThreshold ) {
						int deleted = deleteOldestData(dbService);
						if ( deleted > 0 ) {
							long newDiskSize = dbService.tableFileSystemSize(schemaName, tableName);
							log.info("Trimmed {} oldest rows from {}.{} to free space; size diff is {}",
									deleted, schemaName, tableName, newDiskSize - diskSize);
						}
						return; // only execute once per call, assuming all file systems will be impacted
					} else {
						log.info(
								"Database table {}.{} consumes {} on disk but not deleting data because of configured minimum size threshold {}",
								schemaName, tableName, diskSize, minTableSizeThreshold);
					}
				}

			} catch ( IOException e ) {
				log.error("Error examining disk use for database root {}", f, e);
			}
		}
	}

	private int deleteOldestData(DatabaseSystemService dbService) {
		int deleted = jdbcOperations.execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection conn) throws SQLException, DataAccessException {
				DatabaseMetaData meta = conn.getMetaData();

				// verify the configured dateColumnName actually exists on the configured table
				ResultSet dateColRs = null;
				try {
					dateColRs = meta.getColumns(null, schemaName, tableName, dateColumnName);
					if ( !dateColRs.next() ) {
						log.error("Date column {} not found on table {}.{}; cannot trim data",
								dateColumnName, schemaName, tableName);
						return 0;
					}
				} finally {
					if ( dateColRs != null ) {
						dateColRs.close();
					}
				}

				final String fullTableName = (schemaName == null ? tableName
						: schemaName + '.' + tableName);

				// get oldest available date from table
				Instant oldestDate = findOldestDate(conn, fullTableName);
				if ( oldestDate == null ) {
					log.debug("Oldest date not found on table {}.{}; cannot trim data", schemaName,
							tableName);
					return 0;
				}

				Instant deleteDate = oldestDate.plus(trimMinutes, ChronoUnit.MINUTES);
				int deleted = deleteOlderThan(conn, fullTableName, deleteDate);
				log.debug("Trimmed {} rows from {} older than {} to free space", deleted, fullTableName,
						deleteDate);
				return deleted;
			}

		});

		if ( deleted > 0 ) {
			// now that we've deleted data, perform a vacuum to reclaim space if possible
			dbService.vacuumTable(schemaName, tableName);
		}

		return deleted;
	}

	/**
	 * Find the oldest available date in the configured table.
	 *
	 * <p>
	 * This method will execute the {@link #OLDEST_DATE_QUERY_TEMPLATE} SQL
	 * template, after substituting {@code dateColumnName} and
	 * {@code fullTableName}.
	 * </p>
	 *
	 * @param conn
	 * @param fullTableName
	 * @return
	 * @throws SQLException
	 */
	private Instant findOldestDate(final Connection conn, final String fullTableName)
			throws SQLException {
		String oldestDateSql = String.format(OLDEST_DATE_QUERY_TEMPLATE, dateColumnName, fullTableName);

		Instant oldestDate = null;
		try (PreparedStatement stmt = conn.prepareStatement(oldestDateSql);
				ResultSet rs = stmt.executeQuery()) {
			if ( rs.next() ) {
				oldestDate = JdbcUtils.getUtcTimestampColumnValue(rs, 1);
			}
		}
		return oldestDate;
	}

	/**
	 * Delete rows in the configured database table older than a specific date.
	 *
	 * <p>
	 * This method will execute the {@link #DELETE_BY_DATE_QUERY_TEMPLATE} SQL
	 * template, after substituting {@code fullTableName} and
	 * {@code dateColumnName}, setting the given {@code date} parameter.
	 * </p>
	 *
	 * @param conn
	 *        the database connection
	 * @param fullTableName
	 *        the full table name (schema + name) to delete from
	 * @param date
	 *        the date to delete older than
	 * @return the number of deleted rows
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	private int deleteOlderThan(final Connection conn, final String fullTableName, final Instant date)
			throws SQLException {
		log.debug("Trimming rows from {} older than {} to free space", fullTableName, date);
		String oldestDateSql = String.format(DELETE_BY_DATE_QUERY_TEMPLATE, fullTableName,
				dateColumnName);

		try (PreparedStatement stmt = conn.prepareStatement(oldestDateSql)) {
			JdbcUtils.setUtcTimestampStatementValue(stmt, 1, date);
			int count = stmt.executeUpdate();
			return count;
		}
	}

	/**
	 * Set the database system service to use.
	 *
	 * <p>
	 * This service is assumed to be configured to manage the same database as
	 * provided by the {@link JdbcOperations} passed to the constructor.
	 * </p>
	 *
	 * @param dbSystemService
	 *        the database system service
	 */
	public void setDbSystemService(OptionalService<DatabaseSystemService> dbSystemService) {
		this.dbSystemService = dbSystemService;
	}

	/**
	 * Set the name of the schema of the database table to manage.
	 *
	 * <p>
	 * This defaults to {@literal SOLARNODE}.
	 * </p>
	 *
	 * @param schemaName
	 *        the database schema name of the table to manage
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Set the name of the database table to manage.
	 *
	 * <p>
	 * This defaults to {@literal SN_GENERAL_NODE_DATUM}.
	 * </p>
	 *
	 * @param tableName
	 *        the name of the database table to manage
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Set the name of the date column to manage.
	 *
	 * <p>
	 * This column is referenced when executing time-based queries on the
	 * managed database table, and should contain a timestamp data type. It
	 * defaults to {@literal CREATED}.
	 * </p>
	 *
	 * @param dateColumnName
	 *        the name of the date column on the database table to manage
	 */
	public void setDateColumnName(String dateColumnName) {
		this.dateColumnName = dateColumnName;
	}

	/**
	 * Set the maximum file system use percentage allowed before trimming data
	 * is permitted.
	 *
	 * <p>
	 * This value should be expressed as a percentage, for example
	 * {@literal 92.5}. Defaults to {@literal 90}.
	 * </p>
	 *
	 * @param maxFileSystemUseThreshold
	 *        the max file system use percentage
	 */
	public void setMaxFileSystemUseThreshold(float maxFileSystemUseThreshold) {
		this.maxFileSystemUseThreshold = maxFileSystemUseThreshold;
	}

	/**
	 * Set a minimum size, in bytes, for a table to consume before allowing the
	 * oldest data to be trimmed.
	 *
	 * <p>
	 * This defaults to {@literal 1048576} (1MB).
	 * </p>
	 *
	 * @param minTableSizeThreshold
	 *        the minimum table size threshold
	 */
	public void setMinTableSizeThreshold(long minTableSizeThreshold) {
		this.minTableSizeThreshold = minTableSizeThreshold;
	}

	/**
	 * Set the number of minutes of oldest data to trim.
	 *
	 * <p>
	 * When trimming data, the oldest available date plus this many minutes will
	 * be deleted. Defaults to {@literal 90}.
	 * </p>
	 *
	 * @param trimMinutes
	 *        the number of minutes to to trim
	 */
	public void setTrimMinutes(int trimMinutes) {
		this.trimMinutes = trimMinutes;
	}

}
