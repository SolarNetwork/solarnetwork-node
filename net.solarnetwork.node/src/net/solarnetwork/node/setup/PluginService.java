/* ==================================================================
 * PluginService.java - Apr 21, 2014 2:12:15 PM
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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Service for managing dynamic "plugins" within the application.
 * 
 * @author matt
 * @version 1.0
 */
public interface PluginService {

	/**
	 * Ask the PluginService to refresh its list of available plugins. This is
	 * designed for implementations where the available plugins might change
	 * over time.
	 */
	void refreshAvailablePlugins();

	/**
	 * Get a list of all available plugins.
	 * 
	 * @param query
	 *        an optional query to apply to limit the returned results by. Pass
	 *        {@literal null} to request all available Plugin instances
	 * @param locale
	 *        an optional locale to apply to PluginInfo
	 * @return list of available plugins, or an empty list if none available
	 */
	List<Plugin> availablePlugins(PluginQuery query, Locale locale);

	/**
	 * Get a list of all installed plugins.
	 * 
	 * @param locale
	 *        an optional locale to apply to PluginInfo
	 * @return list of installed plugins, or an empty list if none installed
	 */
	List<Plugin> installedPlugins(Locale locale);

	/**
	 * Remove one or more plugins based on their UIDs. This method might return
	 * immediately to allow the provisioning operation to complete
	 * asynchronously.
	 * 
	 * @param uids
	 *        the collection of plugins to remove
	 * @param locale
	 *        an optional locale to apply to the returned PluginProvisionStatus
	 * @return a status object
	 */
	PluginProvisionStatus removePlugins(Collection<String> uids, Locale locale);

	/**
	 * Get a "preview" status for installing a set of plugins based on their
	 * UIDs. This will return a status object synchronously that will contain
	 * details such as the list of plugins that will be installed and how many
	 * bytes must be downloaded. Note that the returned
	 * {@link PluginProvisionStatus#getProvisionID()} will not be valid for
	 * passing to {@link #statusForProvisioningOperation(String, Locale)}.
	 * 
	 * @param uids
	 *        the collection of plugins to remove
	 * @param locale
	 *        an optional locale to apply to the returned PluginProvisionStatus
	 * @return a status object
	 */
	PluginProvisionStatus previewInstallPlugins(Collection<String> uids, Locale locale);

	/**
	 * Install one or more plugins based on their UIDs. This method might return
	 * immediately to allow the provisioning operation to complete
	 * asynchronously.
	 * 
	 * @param uids
	 *        the collection of plugins to remove
	 * @param locale
	 *        an optional locale to apply to the returned PluginProvisionStatus
	 * @return a status object
	 */
	PluginProvisionStatus installPlugins(Collection<String> uids, Locale locale);

	/**
	 * Get a provisioning status based on a provisioning operation ID. The ID is
	 * one that would have been returned via a previous call to other methods
	 * like {@link #installedPlugins(Locale)}. The service will only maintain
	 * status information for a limited amount of time, and thus might return
	 * {@literal null} even for an ID previously returned.
	 * 
	 * @param provisionID
	 *        the provisioning operation ID to find the status for
	 * @param locale
	 *        an optional locale to apply to the returned PluginProvisionStatus
	 * @return the status, or {@literal null} if the status is not available
	 */
	PluginProvisionStatus statusForProvisioningOperation(String provisionID, Locale locale);

}
