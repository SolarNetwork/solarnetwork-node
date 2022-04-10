/* ==================================================================
 * TransientSettingDao.java - 10/04/2022 5:54:06 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao;

import java.util.concurrent.ConcurrentMap;

/**
 * API for a transient setting DAO.
 * 
 * <p>
 * This API is designed for simple, small, and fast access to transient runtime
 * data shared across SolarNode. Values added to the maps returned from
 * {@link #settings(String)} are <b>not</b> persisted across application
 * restarts. The API is arranged as a map of maps, both with string keys,
 * similar to how the {@link SettingDao} uses a primary key composed of two
 * strings to group related settings together.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.5
 */
public interface TransientSettingDao {

	/**
	 * Get the settings map for a given key.
	 * 
	 * <p>
	 * This method will always return the same map instance for a given key.
	 * </p>
	 * 
	 * @param <V>
	 *        the value type to cast to; note this is only a generic casting
	 *        operation for convenience and the actual map is expected to
	 *        actually be declared as {@code ConcurrentMap&ltString, Object&gt;}
	 * @param key
	 *        the settings key to get
	 * @return the settings for the given key, never {@literal null}
	 */
	<V> ConcurrentMap<String, V> settings(String key);

}
