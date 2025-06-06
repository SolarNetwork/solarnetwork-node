/* ==================================================================
 * SimpleTestEmbeddedDatabase.java - 11/04/2022 2:37:36 PM
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
 * Simple implementation of {@link TestEmbeddedDatabase}.
 *
 * @author matt
 * @version 2.0
 * @since 1.14
 */
public class SimpleTestEmbeddedDatabase implements TestEmbeddedDatabase {

	private final EmbeddedDatabase db;
	private final String dbType;

	/**
	 * Constructor.
	 *
	 * @param db
	 *        the database to delegate to
	 * @param dbType
	 *        the database type
	 */
	public SimpleTestEmbeddedDatabase(EmbeddedDatabase db, String dbType) {
		super();
		this.db = db;
		this.dbType = dbType;
	}

	@Override
	public EmbeddedDatabase getDatabase() {
		return db;
	}

	@Override
	public String getDatabaseType() {
		return dbType;
	}

}
