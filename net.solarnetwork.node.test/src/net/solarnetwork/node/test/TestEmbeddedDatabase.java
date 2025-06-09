/* ==================================================================
 * TestEmbeddedDatabase.java - 11/04/2022 2:35:26 PM
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

package net.solarnetwork.node.test;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

/**
 * Extension of {@link EmbeddedDatabase} to help with testing.
 *
 * @author matt
 * @version 2.0
 * @since 1.14
 */
public interface TestEmbeddedDatabase {

	/** The Derby database type. */
	String DERBY_TYPE = "derby";

	/** The H2 database type. */
	String H2_TYPE = "h2";

	/** The Postgres database type. */
	String POSTGRES_TYPE = "postgres";

	/**
	 * Get the database.
	 *
	 * @return the database
	 */
	EmbeddedDatabase getDatabase();

	/**
	 * Get the type of embedded database.
	 *
	 * @return the type
	 */
	String getDatabaseType();

}
