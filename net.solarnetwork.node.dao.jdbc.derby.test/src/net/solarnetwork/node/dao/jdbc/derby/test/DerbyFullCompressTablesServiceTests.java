/* ==================================================================
 * DerbyFullCompressTablesServiceTests.java - Jul 29, 2017 3:37:45 PM
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

package net.solarnetwork.node.dao.jdbc.derby.test;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import net.solarnetwork.node.dao.jdbc.derby.DerbyFullCompressTablesService;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Tests for the {@link DerbyFullCompressTablesService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DerbyFullCompressTablesServiceTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;
	private DerbyFullCompressTablesService service;

	@Before
	public void setup() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		service = new DerbyFullCompressTablesService();
		service.setJdbcOperations(jdbcTemplate);
		try {
			setupTestTable(dataSource.getConnection());
		} catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void teardown() {
		try {
			dropTestTable(dataSource.getConnection());
		} catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	private void setupTestTable(Connection conn) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables(null, "SOLARNODE", "TEST_COMPRESS", null);
		if ( rs.next() ) {
			dropTestTable(conn);
		}
		conn.setAutoCommit(true);
		conn.createStatement().execute(
				"CREATE TABLE SOLARNODE.TEST_COMPRESS(id INT, created TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP, stuff VARCHAR(255), PRIMARY KEY(id))");
		conn.createStatement()
				.execute("CREATE INDEX TEST_COMPRESS_CREATED_IDX ON SOLARNODE.TEST_COMPRESS(CREATED)");
	}

	private void dropTestTable(Connection conn) throws SQLException {
		conn.setAutoCommit(true);
		conn.createStatement().execute("DROP TABLE SOLARNODE.TEST_COMPRESS");
	}

	private int serialNum = 0;

	private void insertRows(Connection conn, int count) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn
					.prepareStatement("INSERT INTO SOLARNODE.TEST_COMPRESS (id, stuff) VALUES (?, ?)");
			while ( count-- > 0 ) {
				stmt.setInt(1, ++serialNum);
				stmt.setString(2, "This is a a string of important stuff.");
				stmt.execute();
			}
		} finally {
			if ( stmt != null ) {
				stmt.close();
			}
		}
	}

	private void deleteRowsLessThan(Connection conn, int max) throws SQLException {
		int deleted = jdbcTemplate.update("DELETE FROM SOLARNODE.TEST_COMPRESS WHERE id <= ?", max);
		log.debug("Deleted {} rows", deleted);
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
	public void testCompress() {
		File dir = jdbcTemplate.execute(new ConnectionCallback<File>() {

			@Override
			public File doInConnection(Connection conn) throws SQLException, DataAccessException {
				DatabaseMetaData meta = conn.getMetaData();
				String url = meta.getURL();
				assert url.startsWith("jdbc:derby:");
				url = url.substring(11, url.length());
				File dir = new File(url);
				conn.setAutoCommit(true);
				long fsSizeBefore = totalDiskSize(dir);
				log.debug("Disk size before insert: {}", fsSizeBefore);
				insertRows(conn, 50000);
				long fsSizeAfterInsert = totalDiskSize(dir);
				log.debug("Disk size after insert: {}", fsSizeAfterInsert);
				deleteRowsLessThan(conn, 40000);
				return dir;
			}
		});
		long fsSizeBeforeCompress = totalDiskSize(dir);
		log.debug("Disk size before compress: {}", fsSizeBeforeCompress);
		service.processTables(null);
		long fsSizeAfterCompress = totalDiskSize(dir);
		log.debug("Disk size after compress: {} (diff {})", fsSizeAfterCompress,
				(fsSizeAfterCompress - fsSizeBeforeCompress));
		assertTrue("Disk space reduced", fsSizeAfterCompress < fsSizeBeforeCompress);
	}

}
