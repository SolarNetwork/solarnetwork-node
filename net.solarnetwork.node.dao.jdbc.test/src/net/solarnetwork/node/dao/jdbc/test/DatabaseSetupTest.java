/* ==================================================================
 * DatabaseSetupTest.java - Feb 25, 2013 4:03:36 PM
 *
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import static org.junit.Assert.assertNotNull;
import java.util.Map;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Unit tests for the {@link DatabaseSetup} class.
 *
 * @author matt
 * @version 1.2
 */
public class DatabaseSetupTest extends AbstractNodeTransactionalTest {

	@Test
	public void createDatabaseSetup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.setInitSqlResource(new ClassPathResource("settings-init.sql", DatabaseSetup.class));
		setup.init();

		JdbcOperations jdbcOps = this.jdbcTemplate;
		Map<String, ?> results = jdbcOps.queryForMap(
				"SELECT * FROM solarnode.sn_settings WHERE skey = ?", "solarnode.sn_settings.version");
		log.debug("Got sn_settings.version record {}", results);
		assertNotNull(results);
		assertEquals("Should have key, value, type, flags, modified, and note values", 6,
				results.size());
		assertEquals("7", results.get("svalue"));
	}
}
