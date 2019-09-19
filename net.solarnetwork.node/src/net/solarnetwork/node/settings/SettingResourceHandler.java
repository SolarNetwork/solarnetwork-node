/* ==================================================================
 * SettingResourceHandler.java - 16/09/2019 4:41:04 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings;

import java.io.IOException;
import org.springframework.core.io.Resource;

/**
 * API for something that can handle setting updates via resources, such as
 * external files.
 * 
 * <p>
 * This API can be used with {@link TextAreaSettingSpecifier} or
 * {@link FileSettingSpecifier} to provide support for configuration from
 * external resources.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.70
 */
public interface SettingResourceHandler {

	/**
	 * Get a unique, application-wide setting ID.
	 * 
	 * <p>
	 * This ID must be unique across all setting resource handlers registered
	 * within the system. Generally the implementation will also be a
	 * {@link SettingSpecifierProvider} for the same ID.
	 * </p>
	 * 
	 * @return unique ID
	 */
	String getSettingUID();

	/**
	 * Get the current setting resources for a specific key.
	 * 
	 * @param settingKey
	 *        the setting key, generally a
	 *        {@link KeyedSettingSpecifier#getKey()} value
	 * @return the resources, never {@literal null}
	 */
	Iterable<Resource> currentSettingResources(String settingKey);

	/**
	 * Apply settings for a specific key from a resource.
	 * 
	 * <p>
	 * This method returns a collection of setting values to update as a result
	 * of applying the given resources. This provides a way for the handler to
	 * generate a list of settings to be persisted elsewhere (for example via a
	 * {@link SettingsService}.
	 * </p>
	 * 
	 * @param settingKey
	 *        the setting key, generally a
	 *        {@link KeyedSettingSpecifier#getKey()} value
	 * @param resources
	 *        the resources with the settings to apply
	 * @return any setting values that should be persisted as a result of
	 *         applying the given resources (never {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	SettingsUpdates applySettingResources(String settingKey, Iterable<Resource> resources)
			throws IOException;

}
