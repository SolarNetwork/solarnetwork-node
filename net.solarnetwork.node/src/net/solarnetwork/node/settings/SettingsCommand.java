/* ==================================================================
 * SettingsCommand.java - Mar 18, 2012 3:37:02 PM
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Edit settings command object.
 * 
 * @author matt
 * @version 1.1
 */
public class SettingsCommand implements SettingsUpdates {

	private String providerKey;
	private String instanceKey;
	private List<SettingValueBean> values;

	private final Iterable<Pattern> settingKeyPatternsToClean;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code values} property will be created automatically.
	 * </p>
	 */
	public SettingsCommand() {
		this(null);
	}

	/**
	 * Constructor.
	 * 
	 * @param values
	 *        the values, or {@literal null} to have a list created
	 *        automatically
	 */
	public SettingsCommand(List<SettingValueBean> values) {
		this(values, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param values
	 *        the values, or {@literal null} to have a list created
	 *        automatically
	 * @param settingKeyPatternsToClean
	 *        the key patterns to clean, or {@literal null}
	 */
	public SettingsCommand(List<SettingValueBean> values, Iterable<Pattern> settingKeyPatternsToClean) {
		super();
		setValues(values != null ? values : new ArrayList<>(8));
		this.settingKeyPatternsToClean = (settingKeyPatternsToClean != null ? settingKeyPatternsToClean
				: Collections.emptyList());
	}

	@Override
	public boolean hasSettingKeyPatternsToClean() {
		if ( settingKeyPatternsToClean instanceof Collection<?> ) {
			return !((Collection<?>) settingKeyPatternsToClean).isEmpty();
		} else if ( settingKeyPatternsToClean != null ) {
			Iterator<Pattern> itr = settingKeyPatternsToClean.iterator();
			return itr.hasNext();
		}
		return false;
	}

	@Override
	public boolean hasSettingValueUpdates() {
		return (values != null && !values.isEmpty());
	}

	@Override
	public Iterable<Pattern> getSettingKeyPatternsToClean() {
		return settingKeyPatternsToClean;
	}

	@Override
	public Iterable<? extends Change> getSettingValueUpdates() {
		return getValues();
	}

	public List<SettingValueBean> getValues() {
		return values;
	}

	public void setValues(List<SettingValueBean> values) {
		this.values = values;
	}

	public String getProviderKey() {
		return providerKey;
	}

	public void setProviderKey(String providerKey) {
		this.providerKey = providerKey;
	}

	public String getInstanceKey() {
		return instanceKey;
	}

	public void setInstanceKey(String instanceKey) {
		this.instanceKey = instanceKey;
	}

}
