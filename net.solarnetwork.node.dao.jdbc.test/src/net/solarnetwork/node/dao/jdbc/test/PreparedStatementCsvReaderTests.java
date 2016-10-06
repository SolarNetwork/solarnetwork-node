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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
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
 * @version 1.0
 */
public class PreparedStatementCsvReaderTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void importTable() {
		final String tableName = "SOLARNODE.TEST_CSV_IO";
		executeSqlScript("net/solarnetwork/node/dao/jdbc/test/csv-data-02.sql", false);
		final Map<String, ColumnCsvMetaData> columnMetaData = new LinkedHashMap<String, ColumnCsvMetaData>(
				8);
		jdbcTemplate.execute(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				columnMetaData.putAll(
						JdbcUtils.columnCsvMetaDataForDatabaseMetaData(con.getMetaData(), tableName));
				String sql = JdbcUtils.insertSqlForColumnCsvMetaData(tableName, columnMetaData);
				return con.prepareStatement(sql);
			}
		}, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement ps)
					throws SQLException, DataAccessException {
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
				return null;
			}

		});
		List<Map<String, Object>> results = jdbcTemplate
				.queryForList("select * from solarnode.test_csv_io order by pk");
		Assert.assertNotNull(results);
	}

}
