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
 */
public class DefaultTransientSettingDao extends ConcurrentHashMap<String, ConcurrentMap<String, Object>>
		implements TransientSettingDao {

	private static final long serialVersionUID = -6057332346755667688L;

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

}
