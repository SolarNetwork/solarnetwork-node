/* ==================================================================
 * H2OnlineBackupServiceTests.java - 12/04/2022 6:44:51 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.h2.test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.solarnetwork.node.dao.jdbc.JdbcUtils;
import net.solarnetwork.node.dao.jdbc.h2.H2OnlineBackupService;

/**
 * Test cases for the {@link H2OnlineBackupService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class H2OnlineBackupServiceTests {

	private static final Logger log = LoggerFactory.getLogger(H2OnlineBackupServiceTests.class);

	private EmbeddedDatabase derby;
	private HikariDataSource derbyPool;
	private HikariDataSource h2Pool;

	private H2OnlineBackupService service;

	private static class Delete extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			try {
				Files.delete(file);
			} catch ( IOException e ) {
				// ignore
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			try {
				Files.delete(dir);
			} catch ( IOException e ) {
				// ignore
			}
			return FileVisitResult.CONTINUE;
		}
	}

	@Before
	public void setup() throws IOException {
		derby = new EmbeddedDatabaseBuilder().generateUniqueName(true)
				.setType(EmbeddedDatabaseType.DERBY).build();
		derbyPool = new HikariDataSource();
		derbyPool.setDataSource(derby);

		Files.list(Paths.get(".")).filter(p -> p.getFileName().toString().endsWith(".db")).forEach(p -> {
			try {
				log.info("Deleting test database file [{}]", p);
				Files.delete(p);
			} catch ( IOException e ) {
				// ignore
			}
		});

		HikariConfig h2Config = new HikariConfig();
		h2Config.setPoolName("H2 Test");
		h2Config.setDriverClassName("org.h2.Driver");
		h2Config.setJdbcUrl(String.format("jdbc:h2:./test"));
		h2Pool = new HikariDataSource(h2Config);

		setupTestData("Derby", derby);
		setupTestData("H2", h2Pool);

		service = new H2OnlineBackupService(asList(derbyPool, h2Pool));
		if ( Files.isDirectory(service.getDestinationPath()) ) {
			Files.walkFileTree(service.getDestinationPath(), new Delete());
		}
	}

	@After
	public void teardown() {
		derbyPool.close();
		derby.shutdown();

		h2Pool.close();
	}

	private void setupTestData(String name, DataSource dataSource) {
		Resource rsrc = new ClassPathResource("db-init.sql", getClass());
		log.info("Initializing database [{}] from {}", name, rsrc);
		JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
		String[] sql = JdbcUtils.getBatchSqlResource(rsrc);
		jdbcOps.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				con.setAutoCommit(true);
				for ( String s : sql ) {
					try {
						con.createStatement().execute(s);
					} catch ( SQLException e ) {
						log.warn("Error executing SQL [{}]: {}", s, e.toString());
					}
				}
				return null;
			}

		});
	}

	@Test
	public void backup() {
		// WHEN
		service.backup();

		// THEN
		assertThat("Backup directory created", Files.isDirectory(service.getDestinationPath()),
				is(true));
	}

}
