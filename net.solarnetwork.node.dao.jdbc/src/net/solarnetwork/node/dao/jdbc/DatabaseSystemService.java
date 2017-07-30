/* ==================================================================
 * DatabaseSystemService.java - Jul 30, 2017 7:54:19 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import java.io.File;

/**
 * An API to expose system administration functions to the database.
 * 
 * The implementation of these methods are most often database vendor specific,
 * so this API provides a way for the SolarNode to access the information in a
 * generic way.
 * 
 * @author matt
 * @version 1.0
 * @since 1.19
 */
public interface DatabaseSystemService {

	/**
	 * Get a set of "root" directories the database stores files on.
	 * 
	 * This method aims to support discovering how much space is available for
	 * the database system on the file system(s) it uses.
	 * 
	 * @return a list of directories
	 */
	File[] getFileSystemRoots();

	/**
	 * Get the size, in bytes, a specific database table consumes on disk.
	 * 
	 * @param schemaName
	 *        the schema of the table
	 * @param tableName
	 *        the table name
	 * @return the size, in bytes, the table consumes on disk
	 */
	long tableFileSystemSize(String schemaName, String tableName);

}
