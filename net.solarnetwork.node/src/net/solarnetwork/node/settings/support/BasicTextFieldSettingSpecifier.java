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

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.TextFieldSettingSpecifier;

/**
 * Basic implementation of {@link TextFieldSettingSpecifier}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicTextFieldSettingSpecifier extends BasicTitleSettingSpecifier implements
		TextFieldSettingSpecifier {

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

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicTextFieldSettingSpecifier spec = new BasicTextFieldSettingSpecifier(String.format(template,
				getKey()), getDefaultValue());
		spec.setTitle(getTitle());
		spec.setValueTitles(getValueTitles());
		return spec;
	}

	@Override
	public SettingSpecifier mappedWithMapper(
			net.solarnetwork.node.settings.KeyedSettingSpecifier.Mapper mapper) {
		BasicTextFieldSettingSpecifier spec = new BasicTextFieldSettingSpecifier(
				mapper.mapKey(getKey()), getDefaultValue());
		spec.setTitle(getTitle());
		spec.setValueTitles(getValueTitles());
		return spec;
	}

}
