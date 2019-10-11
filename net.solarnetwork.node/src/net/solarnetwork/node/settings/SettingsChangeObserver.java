/* ==================================================================
 * SettingsChangeObserver.java - 25/09/2019 11:32:06 am
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

import java.util.Map;

/**
 * API for something that observes changes to settings.
 * 
 * @author matt
 * @version 1.0
 * @since 1.70
 */
public interface SettingsChangeObserver {

	/**
	 * Callback invoked with settings have changed.
	 * 
	 * <p>
	 * Each key in the {@code properties} Map represents a unique setting key,
	 * such as {@link KeyedSettingSpecifier#getKey()}, and the associated Map
	 * value is the current setting value. The {@code properties} Map may
	 * contain settings whose values have not changed since the last invocation
	 * of this method; that is to say a complete set of settings may be provided
	 * at each invocation of this method.
	 * </p>
	 * 
	 * @param properties
	 *        the current settings
	 */
	void configurationChanged(Map<String, Object> properties);
}
