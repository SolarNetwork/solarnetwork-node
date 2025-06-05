/* ==================================================================
 * JdbcTableBackupResourceProvider.java - 6/10/2016 7:11:28 AM
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

package net.solarnetwork.node.dao.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceInfo;
import net.solarnetwork.node.backup.BackupResourceProvider;
import net.solarnetwork.node.backup.BackupResourceProviderInfo;
import net.solarnetwork.node.backup.SimpleBackupResourceInfo;
import net.solarnetwork.node.backup.SimpleBackupResourceProviderInfo;

/**
 * Backup support for JDBC tables.
 *
 * @author matt
 * @version 1.6
 * @since 1.17
 */
public class JdbcTableBackupResourceProvider implements BackupResourceProvider {

	private final String key;
	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;
	private final TaskExecutor taskExecutor;

	private MessageSource messageSource;
	private String[] tableNames;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * <p>
	 * This will default to a backup provider key of
	 * {@literal net.solarnetwork.node.dao.jdbc.JdbcTableBackupResourceProvider}.
	 * </p>
	 *
	 * @param jdbcTemplate
	 *        The JDBC template to use.
	 * @param transactionTemplate
	 *        A transaction template to use, for supporting savepoints.
	 * @param taskExecutor
	 *        A task executor to use.
	 */
	public JdbcTableBackupResourceProvider(JdbcTemplate jdbcTemplate,
			TransactionTemplate transactionTemplate, TaskExecutor taskExecutor) {
		this("net.solarnetwork.node.dao.jdbc.JdbcTableBackupResourceProvider", jdbcTemplate,
				transactionTemplate, taskExecutor);
	}

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the backup provider key to use
	 * @param jdbcTemplate
	 *        The JDBC template to use.
	 * @param transactionTemplate
	 *        A transaction template to use, for supporting savepoints.
	 * @param taskExecutor
	 *        A task executor to use.
	 * @since 1.3
	 */
	public JdbcTableBackupResourceProvider(String key, JdbcTemplate jdbcTemplate,
			TransactionTemplate transactionTemplate, TaskExecutor taskExecutor) {
		super();
		this.key = key;
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
		this.taskExecutor = taskExecutor;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Iterable<BackupResource> getBackupResources() {
		List<BackupResource> result = new ArrayList<BackupResource>(tableNames.length);
		for ( String tableName : tableNames ) {
			result.add(new JdbcTableBackupResource(tableName, CsvPreference.STANDARD_PREFERENCE));
		}
		return result;
	}

	private final class JdbcTableBackupResource implements BackupResource {

		private final long modTime;
		private final String tableName;
		private final CsvPreference preference;

		private JdbcTableBackupResource(String tableName, CsvPreference preference) {
			super();
			this.tableName = tableName;
			this.modTime = System.currentTimeMillis();
			this.preference = preference;
		}

		@Override
		public String getProviderKey() {
			return getKey();
		}

		@Override
		public String getBackupPath() {
			return tableName + ".csv";
		}

		@Override
		public long getModificationDate() {
			return modTime;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			PipedOutputStream sink = new PipedOutputStream();
			Writer writer = new OutputStreamWriter(sink, "UTF-8");
			PipedInputStream result = new PipedInputStream(sink);
			taskExecutor.execute(new JdbcTableCsvExporter(tableName, writer, preference));
			return result;
		}

		@Override
		public String getSha256Digest() {
			return null;
		}

	}

	private final class JdbcTableCsvExporter implements Runnable {

		private final String sqlQuery;
		private final Writer out;
		private final CsvPreference preference;

		private JdbcTableCsvExporter(String tableName, Writer out, CsvPreference preference) {
			super();
			this.sqlQuery = "SELECT * FROM " + tableName;
			this.out = out;
			this.preference = preference;
		}

		@Override
		public void run() {
			jdbcTemplate.execute(new ConnectionCallback<Object>() {

				@Override
				public Object doInConnection(Connection con) throws SQLException, DataAccessException {
					try {
						exportTable(con);
					} catch ( IOException e ) {
						log.debug("IOException exporting table to CSV", e);
					}
					return null;
				}
			});

		}

		private void exportTable(Connection con) throws SQLException, IOException {
			// query
			Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY);
			try {
				ResultSet rs = stmt.executeQuery(sqlQuery);
				CellProcessor[] cellProcessors = JdbcUtils
						.formattingProcessorsForResultSetMetaData(rs.getMetaData());
				JdbcResultSetCsvWriter writer = new ResultSetCsvWriter(out, preference);
				try {
					writer.write(rs, cellProcessors);
				} finally {
					if ( writer != null ) {
						try {
							writer.flush();
							writer.close();
						} catch ( IOException e ) {
							// ignore
						}
					}
					if ( rs != null ) {
						try {
							rs.close();
						} catch ( SQLException e ) {
							// ignore
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
		}
	}

	@Override
	public boolean restoreBackupResource(final BackupResource resource) {
		if ( resource == null ) {
			return false;
		}
		final String tableName = StringUtils.stripFilenameExtension(resource.getBackupPath());
		if ( tableName == null || tableName.length() < 1 ) {
			return false;
		}
		if ( tableNames != null ) {
			boolean tableSupported = false;
			for ( String supportedTableName : tableNames ) {
				if ( supportedTableName.equalsIgnoreCase(tableName) ) {
					tableSupported = true;
					break;
				}
			}
			if ( !tableSupported ) {
				return false;
			}
		}
		return transactionTemplate.execute(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus status) {
				return jdbcTemplate.execute(new ConnectionCallback<Boolean>() {

					@Override
					public Boolean doInConnection(Connection con)
							throws SQLException, DataAccessException {
						return restoreWithConnection(resource, con, tableName);
					}
				});
			}
		});
	}

	@Override
	public BackupResourceProviderInfo providerInfo(Locale locale) {
		String name = "Database Table Backup Provider";
		String desc = "Backs up the SolarNode database tables.";
		MessageSource ms = messageSource;
		if ( ms != null ) {
			name = ms.getMessage("title", null, name, locale);
			desc = ms.getMessage("desc", null, desc, locale);
		}
		return new SimpleBackupResourceProviderInfo(getKey(), name, desc);
	}

	@Override
	public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
		return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(), null);
	}

	private boolean restoreWithConnection(final BackupResource resource, final Connection con,
			final String tableName) throws SQLException {
		final Map<String, ColumnCsvMetaData> columnMetaData = JdbcUtils
				.columnCsvMetaDataForDatabaseMetaData(con.getMetaData(), tableName);
		final String sql = JdbcUtils.insertSqlForColumnCsvMetaData(tableName, columnMetaData);
		Reader in;
		PreparedStatementCsvReader reader = null;
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			in = new InputStreamReader(resource.getInputStream());
			reader = new PreparedStatementCsvReader(in, CsvPreference.STANDARD_PREFERENCE);
			String[] header = reader.getHeader(true);
			if ( header == null ) {
				// no data; skip
				log.info("No data provided in backup resource for table {}", tableName);
				return true;
			}
			Map<String, Integer> csvColumns = JdbcUtils.csvColumnIndexMapping(header);
			CellProcessor[] cellProcessors = JdbcUtils.parsingCellProcessorsForCsvColumns(header,
					columnMetaData);
			while ( reader.read(ps, csvColumns, cellProcessors, columnMetaData) ) {
				Savepoint sp = con.setSavepoint();
				try {
					ps.executeUpdate();
				} catch ( SQLException e ) {
					DataAccessException dae = jdbcTemplate.getExceptionTranslator().translate("Load CSV",
							sql, e);
					if ( dae instanceof DataIntegrityViolationException ) {
						log.debug("Ignoring {} CSV duplicate import row {}", tableName,
								reader.getRowNumber());
						con.rollback(sp);
					} else {
						throw e;
					}
				}
			}
			return true;
		} catch ( SQLException e ) {
			log.error("SQL error restoring resource [{}] to table [{}]: {}", resource.getBackupPath(),
					tableName, e.getMessage());
		} catch ( IOException e ) {
			log.error("CSV encoding error restoring resource [{}] to table [{}]: {}",
					resource.getBackupPath(), tableName, e.getMessage());
		} finally {
			if ( reader != null ) {
				try {
					reader.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
		}
		return false;
	}

	/**
	 * Set the list of table names to back up. The names should be
	 * fully-qualified like {@code schema.table}.
	 *
	 * @param tableNames
	 *        The tables to back up.
	 */
	public void setTableNames(String[] tableNames) {
		this.tableNames = tableNames;
	}

	/**
	 * Set a {@link MessageSource} to use for resolving backup info messages.
	 *
	 * @param messageSource
	 *        The message source to use.
	 * @since 1.2
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
