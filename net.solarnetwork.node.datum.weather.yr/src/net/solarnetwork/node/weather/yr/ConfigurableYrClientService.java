/* ==================================================================
 * ConfigurableYrClientService.java - 21/05/2017 4:30:39 PM
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

package net.solarnetwork.node.weather.yr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Support class for configurable {@link YrClient} based data sources.
 * 
 * @author matt
 * @version 2.0
 */
public abstract class ConfigurableYrClientService extends DatumDataSourceSupport {

	/** A key to use for the "last datum" in the local cache. */
	protected static final String LAST_DATUM_CACHE_KEY = "last";

	private String locationIdentifier;
	private YrClient client;

	/** A map to be used for caching datum data. */
	protected final ConcurrentMap<String, NodeDatum> datumCache;

	public ConfigurableYrClientService() {
		super();
		datumCache = new ConcurrentHashMap<String, NodeDatum>(4);
	}

	/**
	 * Get a list of setting specifiers suitable for configuring this class via
	 * the {@code SettingSpecifierProvider} API.
	 * 
	 * @return List of setting specifiers.
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("locationIdentifier", null));
		return results;
	}

	/**
	 * Get the configured client.
	 * 
	 * @return the client
	 */
	public YrClient getClient() {
		return client;
	}

	/**
	 * Set the client to use for accessing the Yr API.
	 * 
	 * @param client
	 *        the client to use
	 */
	public void setClient(YrClient client) {
		this.client = client;
	}

	/**
	 * Get the Yr location identifier to use.
	 * 
	 * @return the location identifier
	 */
	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	/**
	 * Set the Yr location identifier to use.
	 * 
	 * @param locationIdentifier
	 *        The location identifier to use.
	 */
	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

}
