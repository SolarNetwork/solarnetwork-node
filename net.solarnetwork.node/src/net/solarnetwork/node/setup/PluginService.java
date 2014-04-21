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

import java.util.List;

/**
 * Service for managing dynamic "plugins" within the application.
 * 
 * @author matt
 * @version 1.0
 */
public interface PluginService {

	/**
	 * Get a list of all available plugins.
	 * 
	 * @param filter
	 *        an optional filter to apply to limit the returned results by. Pass
	 *        <em>null</em> to request all available Plugin instances
	 * @return list of available plugins, or an empty list if none available
	 */
	List<Plugin> availablePlugins(String filter);

}
