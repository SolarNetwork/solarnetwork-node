/* ==================================================================
 * TimeBasedTableDiskSizeManagerTests.java - Jul 30, 2017 3:25:40 PM
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

package net.solarnetwork.node.dao.jdbc.test;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.node.dao.jdbc.DatabaseSystemService;
import net.solarnetwork.node.dao.jdbc.TimeBasedTableDiskSizeManager;
import net.solarnetwork.node.dao.jdbc.derby.DerbyDatabaseSystemService;
import net.solarnetwork.node.dao.jdbc.derby.DerbyFullCompressTablesService;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Tests for the {@link TimeBasedTableDiskSizeManager} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
public class TimeBasedTableDiskSizeManagerTests extends AbstractNodeTransactionalTest {

	private static int ROW_COUNT = 60 * 24 * 30;
	private static int TRIM_MINUTES = 60 * 24;

	@Resource(name = "timeBasedDataSource")
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;
	private DerbyDatabaseSystemService dbService;
	private TimeBasedTableDiskSizeManager manager;
	private File dbDir;

	private int serialNum = 0;

	@Before
	public void setup() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		manager = new TimeBasedTableDiskSizeManager(jdbcTemplate);
		dbService = new DerbyDatabaseSystemService();
		dbService.setJdbcOperations(jdbcTemplate);

		DerbyFullCompressTablesService compressService = new DerbyFullCompressTablesService();
		compressService.setJdbcOperations(jdbcTemplate);
		dbService.setCompressTablesService(compressService);

		manager.setDbSystemService(new StaticOptionalService<DatabaseSystemService>(dbService));
		manager.setMinTableSizeThreshold(1024);
		manager.setSchemaName("SOLARNODE");
		manager.setTableName("TEST_TIME_BASED");
		manager.setTrimMinutes(TRIM_MINUTES);

		dbDir = jdbcTemplate.execute(new ConnectionCallback<File>() {

			@Override
			public File doInConnection(Connection conn) throws SQLException, DataAccessException {
				setupTestTable(conn);
				return dbDir(conn);
			}
		});
		long usableSpace = dbDir.getUsableSpace();
		long totalSpace = dbDir.getTotalSpace();
		manager.setMaxFileSystemUseThreshold(
				(float) (100 * (1.0 - (usableSpace / (double) totalSpace))));
	}

	private File dbDir(Connection conn) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		String url = meta.getURL();
		assert url.startsWith("jdbc:derby:");
		url = url.substring(11, url.length());
		File dir = new File(url);
		return dir;
	}

	private void setupTestTable(Connection conn) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables(null, "SOLARNODE", "TEST_TIME_BASED", null);
		if ( rs.next() ) {
			dropTestTable(conn);
		}
		conn.setAutoCommit(true);
		conn.createStatement().execute(
				"CREATE TABLE SOLARNODE.TEST_TIME_BASED(ID INT, CREATED TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP, STUFF VARCHAR(255), PRIMARY KEY(id))");
		conn.createStatement().execute(
				"CREATE INDEX TEST_TIME_BASED_CREATED_IDX ON SOLARNODE.TEST_TIME_BASED(CREATED)");
	}

	private void dropTestTable(Connection conn) throws SQLException {
		conn.setAutoCommit(true);
		conn.createStatement().execute("DROP TABLE SOLARNODE.TEST_TIME_BASED");
	}

	private void insertRows(Connection conn, Calendar cal, int count) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(
					"INSERT INTO SOLARNODE.TEST_TIME_BASED (id, created, stuff) VALUES (?, ?, ?)");
			while ( count-- > 0 ) {
				stmt.setInt(1, ++serialNum);
				stmt.setTimestamp(2, new Timestamp(cal.getTimeInMillis()), cal);
				stmt.setString(3, "This is a a string of important stuff.");
				stmt.execute();
				cal.add(Calendar.MINUTE, 1);
			}
		} finally {
			if ( stmt != null ) {
				stmt.close();
			}
		}
	}

	private static long totalDiskSize(File file) {
		long fileSize = 0;
		if ( file.isDirectory() ) {
			for ( File f : file.listFiles() ) {
				fileSize += totalDiskSize(f);
			}
		} else {
			fileSize += file.length();
		}
		return fileSize;
	}

	@Test
	public void test() throws Exception {
		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);

		jdbcTemplate.execute(new ConnectionCallback<Object>() {

			@Override
			public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
				insertRows(conn, cal, ROW_COUNT);
				return null;
			}
		});

		long diskSizeBefore = totalDiskSize(dbDir);

		manager.executeJobService();

		int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SOLARNODE.TEST_TIME_BASED",
				Integer.class);
		assertEquals(ROW_COUNT - TRIM_MINUTES, count);

		long diskSizeAfter = totalDiskSize(dbDir);

		log.debug("Disk size diff: {}", diskSizeAfter - diskSizeBefore);
	}

}
