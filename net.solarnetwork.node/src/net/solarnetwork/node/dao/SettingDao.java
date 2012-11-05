/* ===================================================================
 * SettingDao.java
 * 
 * Created Dec 1, 2009 10:23:41 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.dao;

import java.util.List;

import net.solarnetwork.node.Setting;
import net.solarnetwork.node.support.KeyValuePair;

/**
 * Data access object API for setting key/value pairs.
 * 
 * <p>This DAO is for very simple key/value based settings and other
 * perstitable runtime data.</p>
 * 
 * <p>It also supports key+type/value pairs, where key and type are separate
 * values. This can be useful for grouping sets of keys together, or for adding
 * and namespace to prevent key collisions across different packages.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public interface SettingDao extends BatchableDao<Setting> {
	
	/**
	 * Persist a new key/value pair, or update an existing key.
	 * 
	 * <p>The type key will be set to a default value.</p>
	 * 
	 * @param key the setting key
	 * @param value the setting value
	 */
	void storeSetting(String key, String value);

	/**
	 * Persist a new key+type/value pair, or update an existing key+type.
	 * 
	 * @param key the setting key
	 * @param type the type key
	 * @param value the setting value
	 */
	void storeSetting(String key, String type, String value);

	/**
	 * Get the first value for a key.
	 * 
	 * @param key the key to get the first value for
	 * @return the first associated value, or <em>null</em> if key not found
	 */
	String getSetting(String key);
	
	/**
	 * Get all settings for a specific key.
	 * 
	 * @param key the key to get the settings for
	 * @return list of {@link KeyValuePair} objects, where the {@code key} will
	 * be set to the {@code type} value
	 */
	List<KeyValuePair> getSettings(String key);
	
	/**
	 * Get the value for a key+type.
	 * 
	 * @param key the key to get the value for
	 * @param type the type to get the value for
	 * @return the associated value, or <em>null</em> if key not found
	 */
	String getSetting(String key, String type);
	
	/**
	 * Delete a setting key/value pair.
	 * 
	 * <p>This method will not fail if the key does not exist.</p>
	 * 
	 * @param key the key to delete
	 * @return true if the key existed and was deleted
	 */
	boolean deleteSetting(String key);
	
	/**
	 * Delete a setting key+type/value pair.
	 * 
	 * <p>This method will not fail if the key does not exist.</p>
	 * 
	 * @param key the key to delete
	 * @param type the type to delete
	 * @return true if the key existed and was deleted
	 */
	boolean deleteSetting(String key, String type);
	
}
