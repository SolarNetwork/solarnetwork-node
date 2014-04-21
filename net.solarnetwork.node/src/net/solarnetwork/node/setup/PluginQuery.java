/* ==================================================================
 * PluginQuery.java - Apr 22, 2014 7:39:29 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup;

/**
 * API for querying and filtering available plugins.
 * 
 * @author matt
 * @version 1.0
 */
public interface PluginQuery {

	/**
	 * If <em>true</em> then only return the latest (highest) version available
	 * for any given plugin.
	 * 
	 * @return boolean
	 */
	boolean isLatestVersionOnly();

	/**
	 * Get a simple substring-matching query. This is the query as provided by a
	 * user. The application can apply this query in whatever method is most
	 * appropriate to return appropriate results.
	 * 
	 * @return a simple query string
	 */
	String getSimpleQuery();

}
