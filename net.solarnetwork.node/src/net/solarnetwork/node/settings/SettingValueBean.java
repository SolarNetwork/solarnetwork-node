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
 * @version 1.4
 */
public class SettingValueBean implements SettingsUpdates.Change {

	private String providerKey;
	private String instanceKey;
	private String key;
	private String value;
	private String note;
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
		this(null, null, key, value);
	}

	/**
	 * Constructor.
	 *
	 * @param providerKey
	 *        the provider key
	 * @param instanceKey
	 *        the instance key
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 * @since 1.3
	 */
	public SettingValueBean(String providerKey, String instanceKey, String key, String value) {
		this(providerKey, instanceKey, key, value, null);
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
		this.key = key;
		this.remove = remove;
	}

	/**
	 * Constructor.
	 *
	 * @param providerKey
	 *        the provider key
	 * @param instanceKey
	 *        the instance key
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 * @since 1.4
	 */
	public SettingValueBean(String providerKey, String instanceKey, String key, String value,
			String note) {
		super();
		this.providerKey = providerKey;
		this.instanceKey = instanceKey;
		this.key = key;
		this.value = value;
		this.note = note;
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
	public void setRemove(boolean remove) {
		this.remove = remove;
	}

	@Override
	public boolean isTransient() {
		return trans;
	}

	/**
	 * Set the transient flag.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setTransient(boolean value) {
		this.trans = value;
	}

	@Override
	public String getProviderKey() {
		return providerKey;
	}

	/**
	 * Set the provider key.
	 *
	 * @param providerKey
	 *        the key to set
	 */
	public void setProviderKey(String providerKey) {
		this.providerKey = providerKey;
	}

	@Override
	public String getKey() {
		return key;
	}

	/**
	 * Set the key.
	 *
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getValue() {
		return value;
	}

	/**
	 * Set the value.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String getInstanceKey() {
		return instanceKey;
	}

	/**
	 * Set the instance key.
	 *
	 * @param instanceKey
	 *        the key to set
	 */
	public void setInstanceKey(String instanceKey) {
		this.instanceKey = (instanceKey != null && !instanceKey.isEmpty() ? instanceKey : null);
	}

	/**
	 * Get the note.
	 *
	 * @return the note
	 * @since 1.4
	 */
	@Override
	public final String getNote() {
		return note;
	}

	/**
	 * Set the note.
	 *
	 * @param note
	 *        the note to set
	 * @since 1.4
	 */
	public final void setNote(String note) {
		this.note = note;
	}

}
