/* ==================================================================
 * PostgresDatabaseSystemServiceTests.java - 5/06/2025 12:04:15 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.postgres.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import java.util.Collections;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.postgres.PostgresDatabaseSystemService;
import net.solarnetwork.node.test.NodeTestUtils;

/**
 * Test cases for the {@link PostgresDatabaseSystemService} class.
 *
 * @author matt
 * @version 1.0
 */
public class PostgresDatabaseSystemServiceTests {

	private static Properties env;

	private HikariDataSource dataSource;
	private PostgresDatabaseSystemService service;

	@BeforeClass
	public static final void setupClass() {
		env = NodeTestUtils.loadEnvironmentProperties();
	}

	@Before
	public void setup() {
		HikariConfig config = new HikariConfig();
		config.setPoolName("SolarNode-Test");
		config.setConnectionTestQuery("SELECT CURRENT_DATE");
		config.setJdbcUrl(env.getProperty("postgres.url"));
		config.setUsername(env.getProperty("postgres.username"));
		config.setPassword(env.getProperty("postgres.password"));
		dataSource = new HikariDataSource(config);

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.setInitSqlResource(new ClassPathResource("settings-init.sql", DatabaseSetup.class));
		setup.init();

		service = new PostgresDatabaseSystemService(Collections.singleton(dataSource));
	}

	@After
	public void teardown() {
		if ( dataSource != null ) {
			dataSource.close();
		}
	}

	@Test
	public void vacuum() {
		service.vacuumTable("solarnode", "sn_settings");
	}

	@Test
	public void tableSize() {
		long result = service.tableFileSystemSize("solarnode", "sn_settings");
		assertThat("Table has non-zero size", result, is(greaterThan(0L)));
	}

}
