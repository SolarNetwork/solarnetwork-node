/* ==================================================================
 * SettingsFilter.java - 8/06/2024 8:40:41 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

/**
 * API for a filter on settings.
 *
 * <p>
 * For any non-null property, the filter implies only settings that match that
 * property should be included in the filtered result set.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 3.9
 */
public interface SettingsFilter {

	/**
	 * Get the provider key.
	 *
	 * @return the provider key
	 */
	String getProviderKey();

	/**
	 * Get the instance key.
	 *
	 * @return the instance key
	 */
	String getInstanceKey();

}
