/* ==================================================================
 * BasicTextFieldSettingSpecifier.java - Mar 12, 2012 10:10:44 AM
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

package net.solarnetwork.node.settings.support;

import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.TextFieldSettingSpecifier;

/**
 * Basic implementation of {@link TextFieldSettingSpecifier}.
 * 
 * @author matt
 * @version 1.3
 */
public class BasicTextFieldSettingSpecifier extends BasicTitleSettingSpecifier
		implements TextFieldSettingSpecifier {

	private boolean secureTextEntry = false;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 */
	public BasicTextFieldSettingSpecifier(String key, String defaultValue) {
		super(key, defaultValue);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 * @param secureTextEntry
	 *        <em>true</em> if the text should be hidden when editing.
	 */
	public BasicTextFieldSettingSpecifier(String key, String defaultValue, boolean secureTextEntry) {
		super(key, defaultValue);
		this.secureTextEntry = secureTextEntry;
	}

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicTextFieldSettingSpecifier spec = new BasicTextFieldSettingSpecifier(
				String.format(template, getKey()), getDefaultValue());
		spec.setTitle(getTitle());
		spec.setValueTitles(getValueTitles());
		return spec;
	}

	@SuppressWarnings("deprecation")
	@Override
	public SettingSpecifier mappedWithMapper(
			net.solarnetwork.node.settings.KeyedSettingSpecifier.Mapper mapper) {
		return mappedWithMapper((MappableSpecifier.Mapper) mapper);
	}

	@Override
	public SettingSpecifier mappedWithMapper(MappableSpecifier.Mapper mapper) {
		BasicTextFieldSettingSpecifier spec = new BasicTextFieldSettingSpecifier(mapper.mapKey(getKey()),
				getDefaultValue());
		spec.setTitle(getTitle());
		spec.setValueTitles(getValueTitles());
		return spec;
	}

	@Override
	public boolean isSecureTextEntry() {
		return secureTextEntry;
	}

}
