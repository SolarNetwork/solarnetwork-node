/* ==================================================================
 * Plugin.java - Apr 21, 2014 2:14:24 PM
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

import java.util.Locale;

/**
 * API for a system "plugin" that can be manipulated by the application at
 * runtime.
 * 
 * @author matt
 * @version 1.0
 */
public interface Plugin {

	/**
	 * Get a unique identifier for this service. This should be meaningful to
	 * the service implementation.
	 * 
	 * @return unique identifier (should never be <em>null</em>)
	 */
	String getUID();

	/**
	 * Get the plugin version.
	 * 
	 * @return the version
	 */
	PluginVersion getVersion();

	/**
	 * Get the plugin information.
	 * 
	 * @return the info
	 */
	PluginInfo getInfo();

	/**
	 * Get the plugin information as a localized resource.
	 * 
	 * @param locale
	 *        the locale
	 * @return the info
	 */
	PluginInfo getLocalizedInfo(Locale locale);

	/**
	 * Return "core feature" flag. Core features are those plugins that are
	 * central to SolarNode functionality, and should not be removed by users.
	 * They can be upgraded, however.
	 * 
	 * @return core feature flag
	 */
	boolean isCoreFeature();

}
