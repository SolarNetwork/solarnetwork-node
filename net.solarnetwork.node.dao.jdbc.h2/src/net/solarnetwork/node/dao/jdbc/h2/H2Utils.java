/* ==================================================================
 * H2Utils.java - 20/04/2022 10:17:37 AM
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

package net.solarnetwork.node.dao.jdbc.h2;

/**
 * Utilities for H2.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public final class H2Utils {

	private H2Utils() {
		// don't construct me
	}

	/**
	 * Get the path to the H2 database from a JDBC URL.
	 * 
	 * @param url
	 *        the JDBC URL
	 * @return the H2 database name, or {@literal null} if the URL is not for a
	 *         H2 database or cannot be determined
	 */
	public static String h2DatabasePath(final String url) {
		if ( url == null || !url.startsWith("jdbc:h2:") ) {
			return null;
		}
		String[] components = url.split(":");
		String dbPath = components[components.length - 1];
		int optIdx = dbPath.indexOf(';');
		if ( optIdx > 0 ) {
			dbPath = dbPath.substring(0, optIdx);
		}
		return dbPath;
	}

	/**
	 * Get the name of the H2 database from a JDBC URL.
	 * 
	 * @param url
	 *        the JDBC URL
	 * @return the H2 database name, or {@literal null} if the URL is not for a
	 *         H2 database or cannot be determined
	 */
	public static String h2DatabaseName(final String url) {
		String dbName = h2DatabasePath(url);
		if ( dbName == null ) {
			return null;
		}
		int optIdx = dbName.indexOf('/');
		if ( optIdx >= 0 ) {
			String[] components = dbName.split("/");
			dbName = components[components.length - 1];
		}
		return dbName;
	}

}
