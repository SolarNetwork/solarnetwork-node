/* ==================================================================
 * ConfigurableWeatherUndergroundClientService.java - Apr 9, 2017 4:17:45 PM
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

package net.solarnetwork.node.weather.wu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.Identifiable;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SetupResourceSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.setup.SetupResourceProvider;

/**
 * Support class for configurable Weather Underground based services.
 * 
 * @param <T>
 *        the {@link Datum} type used for this service
 * @author matt
 * @version 1.0
 */
public abstract class ConfigurableWeatherUndergroundClientService<T extends Datum>
		implements Identifiable {

	/** A key to use for the "last datum" in the local cache. */
	protected static final String LAST_DATUM_CACHE_KEY = "last";

	private String uid;
	private String groupUID;
	private String locationIdentifier;
	private WeatherUndergroundClient client;
	private MessageSource messageSource;
	private SetupResourceProvider setupResourceProvider;

	/** A map to be used for caching datum data. */
	protected final ConcurrentMap<String, T> datumCache;

	public ConfigurableWeatherUndergroundClientService() {
		super();
		datumCache = new ConcurrentHashMap<String, T>(4);
		setClient(new BasicWeatherUndergoundClient());
	}

	/**
	 * Get a list of setting specifiers suitable for configuring this class via
	 * the {@code SettingSpecifierProvider} API.
	 * 
	 * @return List of setting specifiers.
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", null));
		results.add(new BasicTextFieldSettingSpecifier("locationIdentifier", null));
		results.add(new BasicTextFieldSettingSpecifier("client.apiKey", null));
		if ( setupResourceProvider != null ) {
			Map<String, Object> setupProps = Collections.singletonMap("uid", (Object) getUID());
			results.add(new BasicSetupResourceSettingSpecifier(setupResourceProvider, setupProps));
		}
		return results;
	}

	@Override
	public String getUID() {
		return getUid();
	}

	/**
	 * Alias for {@link @getUID()}.
	 * 
	 * @return the UID
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * Set the UID.
	 * 
	 * @param uid
	 *        the UID to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	/**
	 * Set the group UID.
	 * 
	 * @param groupUID
	 *        the group UID to set
	 */
	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	/**
	 * Get the configured client.
	 * 
	 * @return the client
	 */
	public WeatherUndergroundClient getClient() {
		return client;
	}

	/**
	 * Set the client to use for accessing the Weather Underground API.
	 * 
	 * @param client
	 *        the client to use
	 */
	public void setClient(WeatherUndergroundClient client) {
		this.client = client;
	}

	/**
	 * Get a {@link MessageSource} for supporting message resolution.
	 * 
	 * @return the message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a {@link MessageSource} to support message resolution.
	 * 
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the Weather Underground location identifier to use.
	 * 
	 * @return the location identifier
	 */
	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	/**
	 * Set the Weather Underground location identifier to use.
	 * 
	 * @param locationIdentifier
	 *        The location identifier to use.
	 */
	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	/**
	 * Get the setup resource provider.
	 * 
	 * @return the setup resource provider, or {@code null}
	 */
	public SetupResourceProvider getSetupResourceProvider() {
		return setupResourceProvider;
	}

	/**
	 * Set a setup resource provider.
	 * 
	 * If configured, a {@link SetupResourceSettingSpecifier} will be included
	 * in the setting specifier returned by {@link #getSettingSpecifiers()}.
	 * 
	 * @param setupResourceProvider
	 *        The setup resource provider to use.
	 */
	public void setSetupResourceProvider(SetupResourceProvider setupResourceProvider) {
		this.setupResourceProvider = setupResourceProvider;
	}

}
