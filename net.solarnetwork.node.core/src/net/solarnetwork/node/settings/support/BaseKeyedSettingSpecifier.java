/* ==================================================================
 * BaseKeyedSettingSpecifier.java - Mar 12, 2012 10:03:17 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.settings.support;

import net.solarnetwork.node.settings.KeyedSettingSpecifier;

/**
 * Base implementation of {@link KeyedSettingSpecifier}.
 * 
 * @author matt
 * @version $Revision$
 */
public abstract class BaseKeyedSettingSpecifier<T> extends BaseSettingSpecifier implements
		KeyedSettingSpecifier<T> {

	private String key;
	private T defaultValue;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the default value
	 */
	public BaseKeyedSettingSpecifier(String key, T defaultValue) {
		super();
		setKey(key);
		setDefaultValue(defaultValue);
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public T getDefaultValue() {
		return this.defaultValue;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setDefaultValue(T defaultValue) {
		this.defaultValue = defaultValue;
	}

}
