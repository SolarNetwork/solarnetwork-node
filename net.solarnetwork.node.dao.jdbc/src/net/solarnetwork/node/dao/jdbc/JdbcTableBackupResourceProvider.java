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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.util.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceProvider;

/**
 * Backup support for JDBC tables.
 * 
 * @author matt
 * @version 1.0
 * @since 1.17
 */
public class JdbcTableBackupResourceProvider implements BackupResourceProvider {

	private final JdbcTemplate jdbcTemplate;
	private final TaskExecutor taskExecutor;
	private String[] tableNames;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param jdbcTemplate
	 *        The JDBC template to use.
	 * @param taskExecutor
	 *        A task executor to use.
	 */
	public JdbcTableBackupResourceProvider(JdbcTemplate jdbcTemplate, TaskExecutor taskExecutor) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.taskExecutor = taskExecutor;
	}

	@Override
	public String getKey() {
		return getClass().getName();
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
					while ( rs.next() ) {
						writer.write(rs, cellProcessors);
					}
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
	public boolean restoreBackupResource(BackupResource resource) {
		String tableName = StringUtils.stripFilenameExtension(resource.getBackupPath());
		final Map<String, ColumnCsvMetaData> columnMetaData = new LinkedHashMap<String, ColumnCsvMetaData>(
				8);
		return jdbcTemplate.execute(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				columnMetaData.putAll(
						JdbcUtils.columnCsvMetaDataForDatabaseMetaData(con.getMetaData(), tableName));
				String sql = JdbcUtils.insertSqlForColumnCsvMetaData(tableName, columnMetaData);
				return con.prepareStatement(sql);
			}
		}, new PreparedStatementCallback<Boolean>() {

			@Override
			public Boolean doInPreparedStatement(PreparedStatement ps)
					throws SQLException, DataAccessException {
				Reader in;
				PreparedStatementCsvReader reader = null;
				try {
					in = new InputStreamReader(resource.getInputStream());
					reader = new PreparedStatementCsvReader(in, CsvPreference.STANDARD_PREFERENCE);
					String[] header = reader.getHeader(true);
					Map<String, Integer> csvColumns = JdbcUtils.csvColumnIndexMapping(header);
					CellProcessor[] cellProcessors = JdbcUtils.parsingCellProcessorsForCsvColumns(header,
							columnMetaData);
					while ( reader.read(ps, csvColumns, cellProcessors, columnMetaData) ) {
						ps.executeUpdate();
					}
				} catch ( IOException e ) {
					throw new DataAccessResourceFailureException("CSV encoding error", e);
				} finally {
					if ( reader != null ) {
						try {
							reader.close();
						} catch ( IOException e ) {
							// ignore
						}
					}
				}
				return true;
			}
		});
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

}
