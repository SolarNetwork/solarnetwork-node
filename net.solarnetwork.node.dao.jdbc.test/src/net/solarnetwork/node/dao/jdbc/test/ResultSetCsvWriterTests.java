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
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
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
 * @version 1.0
 */
public class ResultSetCsvWriterTests extends AbstractNodeTransactionalTest {

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
	public void exportTable() throws Exception {
		executeSqlScript("net/solarnetwork/node/dao/jdbc/test/csv-data-01.sql", false);
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
