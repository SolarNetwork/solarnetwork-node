/* ==================================================================
 * SimplePluginQuery.java - Apr 22, 2014 7:42:47 AM
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
 * Simple implementation of {@link PluginQuery}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimplePluginQuery implements PluginQuery {

	private boolean latestVersionOnly = true;
	private String simpleQuery = null;

	/**
	 * Default constructor.
	 */
	public SimplePluginQuery() {
		super();
	}

	@Override
	public boolean isLatestVersionOnly() {
		return latestVersionOnly;
	}

	/**
	 * Set the latest version only flag.
	 * 
	 * @param latestVersionOnly
	 *        the flag to set
	 */
	public void setLatestVersionOnly(boolean latestVersionOnly) {
		this.latestVersionOnly = latestVersionOnly;
	}

	@Override
	public String getSimpleQuery() {
		return simpleQuery;
	}

	/**
	 * Set the simple query.
	 * 
	 * @param simpleQuery
	 *        the query to set
	 */
	public void setSimpleQuery(String simpleQuery) {
		this.simpleQuery = simpleQuery;
	}

}
