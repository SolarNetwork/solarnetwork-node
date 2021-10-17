/* ==================================================================
 * ProviderHelper.java - 10/05/2021 11:55:55 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings.ca;

import java.util.Map;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.util.MapPathMatcher;
import net.solarnetwork.util.SearchFilter;

/**
 * Helper class for a provider and its associated properties.
 * 
 * @author matt
 * @version 2.0
 * @since 1.13
 */
public final class ProviderHelper {

	private final SettingSpecifierProvider provider;
	private final Map<String, ?> properties;

	/**
	 * Constructor.
	 * 
	 * @param provider
	 *        the provider
	 * @param properties
	 *        the properties
	 */
	public ProviderHelper(SettingSpecifierProvider provider, Map<String, ?> properties) {
		super();
		this.provider = provider;
		this.properties = properties;
	}

	/**
	 * Test if a filter matches the provider service properties.
	 * 
	 * @param filter
	 *        the filter
	 * @return {@literal true} if the filter matches
	 */
	public boolean matches(SearchFilter filter) {
		if ( properties == null ) {
			return false;
		}
		return MapPathMatcher.matches(properties, filter);
	}

	/**
	 * Get the provider.
	 * 
	 * @return the provider
	 */
	public SettingSpecifierProvider getProvider() {
		return provider;
	}

	/**
	 * Get the properties.
	 * 
	 * @return the properties
	 */
	public Map<String, ?> getProperties() {
		return properties;
	}

}
