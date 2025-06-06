/* ==================================================================
 * PreparedStatementCsvReaderTests.java - 6/10/2016 1:13:32 PM
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

package net.solarnetwork.node.dao.jdbc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.node.dao.jdbc.ColumnCsvMetaData;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.JdbcUtils;
import net.solarnetwork.node.dao.jdbc.PreparedStatementCsvReader;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test cases for the {@link PreparedStatementCsvReader} class.
 *
 * @author matt
 * @version 1.1
 */
public class PreparedStatementCsvReaderTests extends AbstractNodeTransactionalTest {

	@BeforeTransaction
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		executeSqlScript("net/solarnetwork/node/dao/jdbc/test/init-csv-data.sql", false);
	}

	private void importData(final String tableName) {
		final Map<String, ColumnCsvMetaData> columnMetaData = new LinkedHashMap<String, ColumnCsvMetaData>(
				8);
		jdbcTemplate.execute(new ConnectionCallback<Object>() {

			@Override
			public Object doInConnection(Connection con) throws SQLException, DataAccessException {
				columnMetaData.putAll(
						JdbcUtils.columnCsvMetaDataForDatabaseMetaData(con.getMetaData(), tableName));
				String sql = JdbcUtils.insertSqlForColumnCsvMetaData(tableName, columnMetaData);
				PreparedStatement ps = con.prepareStatement(sql);

				Reader in;
				PreparedStatementCsvReader reader = null;
				try {
					in = new InputStreamReader(getClass().getResourceAsStream("csv-data-01.csv"),
							"UTF-8");
					reader = new PreparedStatementCsvReader(in, CsvPreference.STANDARD_PREFERENCE);
					String[] header = reader.getHeader(true);
					Map<String, Integer> csvColumns = JdbcUtils.csvColumnIndexMapping(header);
					CellProcessor[] cellProcessors = JdbcUtils.parsingCellProcessorsForCsvColumns(header,
							columnMetaData);
					while ( reader.read(ps, csvColumns, cellProcessors, columnMetaData) ) {
						Savepoint sp = con.setSavepoint();
						try {
							ps.executeUpdate();
						} catch ( SQLException e ) {

							DataAccessException dae = jdbcTemplate.getExceptionTranslator()
									.translate("Load CSV", sql, e);
							if ( dae instanceof DataIntegrityViolationException ) {
								con.rollback(sp);
							} else {
								throw e;
							}
						}
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
				return null;
			}

		});
	}

	@Test
	public void importTable() throws Exception {
		final String tableName = "SOLARNODE.TEST_CSV_IO";
		importData(tableName);
		final AtomicInteger row = new AtomicInteger(0);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		jdbcTemplate.query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				// TODO Auto-generated method stub
				return con.prepareStatement(
						"select PK,STR,INUM,DNUM,TS from solarnode.test_csv_io order by pk");
			}
		}, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				row.incrementAndGet();
				final int i = row.intValue();
				assertEquals("PK " + i, i, rs.getLong(1));
				if ( i == 2 ) {
					assertNull("STR " + i, rs.getString(2));
				} else {
					assertEquals("STR " + i, "s0" + i, rs.getString(2));
				}
				if ( i == 3 ) {
					assertNull("INUM " + i, rs.getObject(3));
				} else {
					assertEquals("INUM " + i, i, rs.getInt(3));
				}
				if ( i == 4 ) {
					assertNull("DNUM " + i, rs.getObject(4));
				} else {
					assertEquals("DNUM " + i, i, rs.getDouble(4), 0.01);
				}
				if ( i == 5 ) {
					assertNull("TS " + i, rs.getObject(5));
				} else {
					Timestamp ts = rs.getTimestamp(5, utcCalendar);
					try {
						assertEquals("TS " + i, sdf.parse("2016-10-0" + i + "T12:01:02.345Z"), ts);
					} catch ( ParseException e ) {
						// should not get here
					}
				}
			}
		});
		assertEquals("Imported count", 5, row.intValue());
	}

	@Test
	public void updateTable() throws Exception {
		final String tableName = "SOLARNODE.TEST_CSV_IO";
		importData(tableName);

		txTemplate.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// verify the savepoint logic works to ignore inserts on data that already exists
				importData(tableName);
			}
		});
	}

}
