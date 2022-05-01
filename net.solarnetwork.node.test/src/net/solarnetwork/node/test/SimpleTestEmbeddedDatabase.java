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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Simple implementation of {@link TestEmbeddedDatabase}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.14
 */
public class SimpleTestEmbeddedDatabase implements TestEmbeddedDatabase {

	private final EmbeddedDatabase db;
	private final EmbeddedDatabaseType dbType;

	/**
	 * Constructor.
	 * 
	 * @param db
	 *        the database to delegate to
	 * @param dbType
	 *        the database type
	 */
	public SimpleTestEmbeddedDatabase(EmbeddedDatabase db, EmbeddedDatabaseType dbType) {
		super();
		this.db = db;
		this.dbType = dbType;
	}

	@Override
	public EmbeddedDatabaseType getDatabaseType() {
		return dbType;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return db.getLogWriter();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return db.unwrap(iface);
	}

	@Override
	public void shutdown() {
		db.shutdown();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		db.setLogWriter(out);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return db.isWrapperFor(iface);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return db.getConnection();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		db.setLoginTimeout(seconds);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return db.getConnection(username, password);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return db.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return db.getParentLogger();
	}

}
