/* ==================================================================
 * ConfigurableOwmClientService.java - 14/09/2018 4:50:56 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.owm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Support class for configurable {@link OwmClient} based data sources.
 * 
 * @author matt
 * @version 2.1
 */
public abstract class ConfigurableOwmClientService extends DatumDataSourceSupport {

	/** A key to use for the "last datum" in the local cache. */
	protected static final String LAST_DATUM_CACHE_KEY = "last";

	/** The default value for the {@code timeZoneId} property. */
	public static final String DEFAULT_TIME_ZONE_ID = "UTC";

	private String locationIdentifier;
	private String timeZoneId = DEFAULT_TIME_ZONE_ID;
	private OwmClient client;
	private SetupResourceProvider locationSettingResourceProvider;

	/** A map to be used for caching datum data. */
	protected final ConcurrentMap<String, NodeDatum> datumCache;

	/**
	 * Constructor.
	 */
	public ConfigurableOwmClientService() {
		super();
		datumCache = new ConcurrentHashMap<>(4);
	}

	/**
	 * Get a list of setting specifiers suitable for configuring this class via
	 * the {@code SettingSpecifierProvider} API.
	 * 
	 * @return List of setting specifiers.
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.addAll(baseIdentifiableSettings(null));
		results.add(new BasicTextFieldSettingSpecifier("client.apiKey", null));
		results.add(new BasicTextFieldSettingSpecifier("timeZoneId", DEFAULT_TIME_ZONE_ID));
		results.add(new BasicTextFieldSettingSpecifier("locationIdentifier", null));
		if ( locationSettingResourceProvider != null ) {
			Map<String, Object> setupProps = new HashMap<String, Object>();
			setupProps.put("lid", getLocationIdentifier());
			if ( client != null ) {
				setupProps.put("apikey", client.getApiKey());
			}
			results.add(
					new BasicSetupResourceSettingSpecifier(locationSettingResourceProvider, setupProps));
		}
		return results;
	}

	/**
	 * Get the configured client.
	 * 
	 * @return the client
	 */
	public OwmClient getClient() {
		return client;
	}

	/**
	 * Set the client to use for accessing the OWM API.
	 * 
	 * @param client
	 *        the client to use
	 */
	public void setClient(OwmClient client) {
		this.client = client;
	}

	/**
	 * Get the OWM location identifier to use.
	 * 
	 * @return the location identifier
	 */
	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	/**
	 * Set the OWM location identifier to use.
	 * 
	 * @param locationIdentifier
	 *        The location identifier to use.
	 */
	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	/**
	 * Get the setup resource provider to use for providing a UI for finding OWM
	 * location identifiers.
	 * 
	 * @return the setup resource provider to use, or {@literal null}
	 */
	public SetupResourceProvider getLocationSettingResourceProvider() {
		return locationSettingResourceProvider;
	}

	/**
	 * Set a setup resource provider to use for providing a UI for finding OWM
	 * location identifiers.
	 * 
	 * @param locationSettingResourceProvider
	 *        the setup resource provider to use
	 */
	public void setLocationSettingResourceProvider(
			SetupResourceProvider locationSettingResourceProvider) {
		this.locationSettingResourceProvider = locationSettingResourceProvider;
	}

	/**
	 * Get the time zone ID to apply to local date/time values.
	 * 
	 * @return the time zone ID to apply
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID to apply to local date/time values.
	 * 
	 * @param timeZoneId
	 *        the time zone ID to apply; {@literal null} will be treated as
	 *        {@literal UTC}
	 */
	public void setTimeZoneId(String timeZoneId) {
		if ( timeZoneId == null ) {
			timeZoneId = "UTC";
		}
		this.timeZoneId = timeZoneId;
	}

}
