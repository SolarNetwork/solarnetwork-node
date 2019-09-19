/* ==================================================================
 * SettingValueBean.java - Mar 18, 2012 3:38:04 PM
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
 */

package net.solarnetwork.node.settings;

/**
 * An individual setting value.
 * 
 * @author matt
 * @version 1.2
 */
public class SettingValueBean implements SettingsUpdates.Change {

	private String providerKey;
	private String instanceKey;
	private String key;
	private String value;
	private boolean trans;
	private boolean remove;

	/**
	 * Constructor.
	 */
	public SettingValueBean() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 * @since 1.2
	 */
	public SettingValueBean(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param remove
	 *        the remove flag
	 * @since 1.2
	 */
	public SettingValueBean(String key, boolean remove) {
		super();
		this.key = key;
		this.remove = remove;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SettingValueBean{key=");
		builder.append(key);
		builder.append(",value=");
		builder.append(value);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the remove flag.
	 * 
	 * @return {@literal true} this setting should be deleted
	 * @since 1.1
	 */
	@Override
	public boolean isRemove() {
		return remove;
	}

	/**
	 * Set the remove flag.
	 * 
	 * @param remove
	 *        The flag to set.
	 * @since 1.1
	 */
	public void setRemove(boolean delete) {
		this.remove = delete;
	}

	@Override
	public boolean isTransient() {
		return trans;
	}

	public void setTransient(boolean value) {
		this.trans = value;
	}

	public String getProviderKey() {
		return providerKey;
	}

	public void setProviderKey(String providerKey) {
		this.providerKey = providerKey;
	}

	@Override
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getInstanceKey() {
		return instanceKey;
	}

	public void setInstanceKey(String instanceKey) {
		this.instanceKey = instanceKey;
	}

}
