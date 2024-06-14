/* ===================================================================
 * JdbcDaoConstants.java
 *
 * Created Jul 24, 2009 2:38:46 PM
 *
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc;

/**
 * Constant definitions for JDBC DAO implementations to use.
 *
 * @author matt
 * @version 1.0
 */
public final class JdbcDaoConstants {

	/** The database schema name to use. */
	public static final String SCHEMA_NAME = "solarnode";

	/** The table name for settings. */
	public static final String TABLE_SETTINGS = "sn_settings";

	private JdbcDaoConstants() {
		// not available
	}

}
