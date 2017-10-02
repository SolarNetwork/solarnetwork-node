/* ==================================================================
 * NodeAppConfiguration.java - 2/10/2017 11:30:55 AM
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

package net.solarnetwork.node.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bean for immutable node application configuration.
 * 
 * @author matt
 * @version 1.0
 * @since 1.53
 */
public class NodeAppConfiguration {

	private final long created;
	private final Map<String, String> networkServiceUrls;

	/**
	 * Default constructor.
	 */
	public NodeAppConfiguration() {
		super();
		this.created = 0; // make expired
		this.networkServiceUrls = Collections.emptyMap();
	}

	/**
	 * Constructor.
	 * 
	 * @param networkServiceUrls
	 *        the newtork service URLs
	 */
	public NodeAppConfiguration(Map<String, String> networkServiceUrls) {
		super();
		this.created = System.currentTimeMillis();
		this.networkServiceUrls = (networkServiceUrls == null || networkServiceUrls.isEmpty()
				? Collections.<String, String> emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(networkServiceUrls)));
	}

	/**
	 * Get the creation date.
	 * 
	 * @return the created date, as milliseconds since the epoch
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * Get the network service URL mappings.
	 * 
	 * @return mapping of network service names to associated URLs
	 */
	public Map<String, String> getNetworkServiceUrls() {
		return networkServiceUrls;
	}

}
