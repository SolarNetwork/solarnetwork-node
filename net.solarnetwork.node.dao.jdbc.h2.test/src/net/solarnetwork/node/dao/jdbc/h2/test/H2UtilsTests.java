/* ==================================================================
 * H2UtilsTests.java - 20/04/2022 10:31:35 AM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import net.solarnetwork.node.dao.jdbc.h2.H2Utils;

/**
 * Test cases for the {@link H2Utils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class H2UtilsTests {

	@Test
	public void dbPath_relative() {
		// WHEN
		String path = H2Utils.h2DatabasePath("jdbc:h2:./var/solarnode-h2");

		// THEN
		assertThat("Relative path extracted", path, is("./var/solarnode-h2"));
	}

	@Test
	public void dbName_relative() {
		// WHEN
		String path = H2Utils.h2DatabaseName("jdbc:h2:./var/solarnode-h2");

		// THEN
		assertThat("Relative path extracted", path, is("solarnode-h2"));
	}

	@Test
	public void dbPath_absolute() {
		// WHEN
		String path = H2Utils.h2DatabasePath("jdbc:h2:/run/solarnode/db/solarnode");

		// THEN
		assertThat("Absolute path extracted", path, is("/run/solarnode/db/solarnode"));
	}

	@Test
	public void dbName_absolute() {
		// WHEN
		String path = H2Utils.h2DatabaseName("jdbc:h2:/run/solarnode/db/solarnode");

		// THEN
		assertThat("Absolute path extracted", path, is("solarnode"));
	}

	@Test
	public void dbPath_absolute_split() {
		// WHEN
		String path = H2Utils.h2DatabasePath("jdbc:h2:split:25:/run/solarnode/db/solarnode");

		// THEN
		assertThat("Absolute path extracted", path, is("/run/solarnode/db/solarnode"));
	}

	@Test
	public void dbName_absolute_split() {
		// WHEN
		String path = H2Utils.h2DatabaseName("jdbc:h2:split:25:/run/solarnode/db/solarnode");

		// THEN
		assertThat("Absolute path extracted", path, is("solarnode"));
	}

}
