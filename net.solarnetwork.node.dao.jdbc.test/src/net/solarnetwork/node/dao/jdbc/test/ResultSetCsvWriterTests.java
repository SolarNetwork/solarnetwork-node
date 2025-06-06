/* ==================================================================
 * ResultSetCsvWriterTests.java - 6/10/2016 12:28:13 PM
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.FileCopyUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.JdbcUtils;
import net.solarnetwork.node.dao.jdbc.ResultSetCsvWriter;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test cases for the {@link ResultSetCsvWriter} class.
 *
 * @author matt
 * @version 1.1
 */
public class ResultSetCsvWriterTests extends AbstractNodeTransactionalTest {

	@BeforeTransaction
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		executeSqlScript("net/solarnetwork/node/dao/jdbc/test/init-csv-data.sql", false);
	}

	private void populateTestData() {
		final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		jdbcTemplate.batchUpdate(
				"INSERT INTO solarnode.test_csv_io (pk, str, inum, dnum, ts) VALUES (?,?,?,?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						int pos = i + 1;
						ps.setLong(1, pos);
						if ( pos == 2 ) {
							ps.setNull(2, Types.VARCHAR);
						} else {
							ps.setString(2, "s0" + pos);
						}
						if ( pos == 3 ) {
							ps.setNull(3, Types.INTEGER);
						} else {
							ps.setInt(3, pos);
						}
						if ( pos == 4 ) {
							ps.setNull(4, Types.DOUBLE);
						} else {
							ps.setDouble(4, pos);
						}
						if ( pos == 5 ) {
							ps.setNull(5, Types.TIMESTAMP);
						} else {
							try {
								Date d = sdf.parse("2016-10-0" + pos + "T12:01:02.345Z");
								ps.setTimestamp(5, new java.sql.Timestamp(d.getTime()), utcCalendar);
							} catch ( ParseException e ) {
								// should not get here
							}
						}
					}

					@Override
					public int getBatchSize() {
						return 5;
					}
				});
	}

	@Test
	public void exportTable() throws Exception {
		populateTestData();
		String result = jdbcTemplate.query("select * from solarnode.test_csv_io order by pk",
				new ResultSetExtractor<String>() {

					@Override
					public String extractData(ResultSet rs) throws SQLException, DataAccessException {
						CellProcessor[] processors = JdbcUtils
								.formattingProcessorsForResultSetMetaData(rs.getMetaData());
						StringWriter out = new StringWriter();
						ResultSetCsvWriter writer = new ResultSetCsvWriter(out,
								CsvPreference.STANDARD_PREFERENCE);
						try {
							try {
								writer.write(rs, processors);
							} catch ( IOException e ) {
								throw new DataAccessResourceFailureException("IO exception", e);
							}
						} finally {
							try {
								writer.flush();
								writer.close();
							} catch ( IOException e ) {
								// ignore
							}
						}
						return out.toString();
					}

				});

		Assert.assertNotNull(result);
		Assert.assertEquals("CSV output", FileCopyUtils.copyToString(
				new InputStreamReader(getClass().getResourceAsStream("csv-data-01.csv"), "UTF-8")),
				result);
	}

}
