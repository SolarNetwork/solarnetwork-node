/* ==================================================================
 * PluginInfo.java - Apr 21, 2014 2:28:53 PM
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
import org.jspecify.annotations.Nullable;

/**
 * Descriptive information about a plugin, designed to help users of the plugin.
 *
 * @author matt
 * @version 1.0
 */
public interface PluginInfo {

	/**
	 * Get a name of the plugin.
	 *
	 * @return the name
	 */
	@Nullable
	String getName();

	/**
	 * Get a description of the plugin.
	 *
	 * @return the description
	 */
	@Nullable
	String getDescription();

	/**
	 * Get a localized name of the plugin.
	 *
	 * @param locale
	 *        the desired locale, or {@code null} to use the default locale
	 * @return the name
	 */
	@Nullable
	String getLocalizedName(@Nullable Locale locale);

	/**
	 * Get a localized description of the plugin.
	 *
	 * @param locale
	 *        the desired locale, or {@code null} to use the default locale
	 * @return the description
	 */
	@Nullable
	String getLocalizedDescription(@Nullable Locale locale);

}
