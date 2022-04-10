/* ==================================================================
 * DefaultTransientSettingDao.java - 10/04/2022 5:56:35 PM
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link TransientSettingDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.5
 */
public class DefaultTransientSettingDao extends ConcurrentHashMap<String, ConcurrentMap<String, Object>>
		implements TransientSettingDao {

	private static final long serialVersionUID = -2763340940916793785L;

	/** The {@code settingsInitialCapacity} property default value. */
	public static final int DEFAULT_SETTINGS_INITIAL_CAPACITY = 4;

	/** The {@code settingsLoadFactor} property default value. */
	public static final float DEFAULT_SETTINGS_LOAD_FACTOR = 0.9f;

	/** The {@code settingsConcurrencyLevel} property default value. */
	public static final int DEFAULT_SETTINGS_CONCURRENCY_LEVEL = 2;

	private int settingsInitialCapacity = DEFAULT_SETTINGS_INITIAL_CAPACITY;
	private float settingsLoadFactor = DEFAULT_SETTINGS_LOAD_FACTOR;
	private int settingsConcurrencyLevel = DEFAULT_SETTINGS_CONCURRENCY_LEVEL;

	/**
	 * Default constructor.
	 */
	public DefaultTransientSettingDao() {
		this(64, 0.8f, Runtime.getRuntime().availableProcessors() + 1);
	}

	/**
	 * Constructor.
	 * 
	 * @param initialCapacity
	 *        the initial capacity hint
	 */
	public DefaultTransientSettingDao(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructor.
	 * 
	 * @param m
	 *        another map to copy values from
	 */
	public DefaultTransientSettingDao(Map<? extends String, ? extends ConcurrentMap<String, Object>> m) {
		super(m);
	}

	/**
	 * Constructor.
	 * 
	 * @param initialCapacity
	 *        the initial capacity hint
	 * @param loadFactor
	 *        a load factor hint
	 */
	public DefaultTransientSettingDao(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Constructor.
	 * 
	 * @param initialCapacity
	 *        the initial capacity hint
	 * @param loadFactor
	 *        a load factor hint
	 * @param concurrencyLevel
	 *        a concurrency level hint
	 */
	public DefaultTransientSettingDao(int initialCapacity, float loadFactor, int concurrencyLevel) {
		super(initialCapacity, loadFactor, concurrencyLevel);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <V> ConcurrentMap<String, V> settings(String key) {
		ConcurrentMap<String, Object> result = this.computeIfAbsent(key,
				k -> new ConcurrentHashMap<>(settingsInitialCapacity, settingsLoadFactor,
						settingsConcurrencyLevel));
		return (ConcurrentMap) result;
	}

	/**
	 * Get the initial capacity used for settings maps.
	 * 
	 * @return the initial capacity; defaults to
	 *         {@link #DEFAULT_SETTINGS_INITIAL_CAPACITY}
	 */
	public int getSettingsInitialCapacity() {
		return settingsInitialCapacity;
	}

	/**
	 * Set the initial capacity used for settings maps.
	 * 
	 * @param settingsInitialCapacity
	 *        the capacity to set
	 */
	public void setSettingsInitialCapacity(int settingsInitialCapacity) {
		this.settingsInitialCapacity = settingsInitialCapacity;
	}

	/**
	 * Get the load factory used for settings maps.
	 * 
	 * @return the load factor; defaults to
	 *         {@link #DEFAULT_SETTINGS_LOAD_FACTOR}
	 */
	public float getSettingsLoadFactor() {
		return settingsLoadFactor;
	}

	/**
	 * Set the load factory used for settings maps.
	 * 
	 * @param settingsLoadFactor
	 *        the load factor to set
	 */
	public void setSettingsLoadFactor(float settingsLoadFactor) {
		this.settingsLoadFactor = settingsLoadFactor;
	}

	/**
	 * Get the concurrency level used for settings maps.
	 * 
	 * @return the concurrency level; defaults to
	 *         {@link #DEFAULT_SETTINGS_CONCURRENCY_LEVEL}
	 */
	public int getSettingsConcurrencyLevel() {
		return settingsConcurrencyLevel;
	}

	/**
	 * Set the concurrency level used for settings maps.
	 * 
	 * @param settingsConcurrencyLevel
	 *        the concurrency level to set
	 */
	public void setSettingsConcurrencyLevel(int settingsConcurrencyLevel) {
		this.settingsConcurrencyLevel = settingsConcurrencyLevel;
	}

}
