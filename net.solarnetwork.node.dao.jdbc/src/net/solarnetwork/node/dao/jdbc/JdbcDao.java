/* ===================================================================
 * JdbcDao.java
 * 
 * Created Dec 6, 2009 12:42:41 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * $Revision$
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc;

/**
 * API for JDBC-based DAO implemtnations.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public interface JdbcDao {

	/**
	 * Get the database schema name this DAO is working with.
	 * 
	 * @return database schema name
	 */
	String getSchemaName();
	
	/**
	 * Get the primary database table name this DAO is working with.
	 * 
	 * @return primary database table name
	 */
	String getTableName();
	
	/**
	 * Get the database table names this DAO is working with.
	 * 
	 * <p>If a DAO manages more than one table, this should return all
	 * the table names.</p>
	 * 
	 * @return database table name
	 */
	String[] getTableNames();
	
}
