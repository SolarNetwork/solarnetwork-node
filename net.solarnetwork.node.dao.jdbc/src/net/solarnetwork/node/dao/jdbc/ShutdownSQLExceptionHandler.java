/* ==================================================================
 * ShutdownSQLExceptionHandler.java - 30/09/2016 9:25:13 AM
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

package net.solarnetwork.node.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Recover from connection exceptions by shutting down.
 *
 * @author matt
 * @version 1.1
 */
public class ShutdownSQLExceptionHandler extends AbstractSQLExceptionHandler {

	/**
	 * Constructor.
	 */
	public ShutdownSQLExceptionHandler() {
		super();
	}

	@Override
	public void handleGetConnectionException(SQLException e) {
		handleConnectionException(null, e);
	}

	@Override
	public void handleConnectionException(Connection conn, SQLException e) {
		SQLException root = exceptionMatchingSqlStatePattern(e);
		if ( root == null ) {
			return;
		}
		shutdown(root.getMessage());
	}

	private void shutdown(String msg) {
		log.error("Shutting down now due to database connection error: {}", msg);
		// graceful would be bundleContext.getBundle(0).stop();, but we don't need to wait for that here
		System.exit(1);
	}

}
