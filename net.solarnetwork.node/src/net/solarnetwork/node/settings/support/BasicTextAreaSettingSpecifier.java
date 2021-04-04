/* ==================================================================
 * BasicTextAreaSettingSpecifier.java - 16/09/2019 4:50:39 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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
import net.solarnetwork.node.settings.TextAreaSettingSpecifier;

/**
 * Basic implementation of {@link TextAreaSettingSpecifier}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.70
 */
public class BasicTextAreaSettingSpecifier extends BaseKeyedSettingSpecifier<String>
		implements TextAreaSettingSpecifier {

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 */
	public BasicTextAreaSettingSpecifier(String key, String defaultValue) {
		super(key, defaultValue);
	}

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicTextAreaSettingSpecifier spec = new BasicTextAreaSettingSpecifier(
				String.format(template, getKey()), getDefaultValue());
		spec.setTitle(getTitle());
		spec.setDescriptionArguments(getDescriptionArguments());
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
		BasicTextAreaSettingSpecifier spec = new BasicTextAreaSettingSpecifier(mapper.mapKey(getKey()),
				getDefaultValue());
		spec.setTitle(getTitle());
		spec.setDescriptionArguments(getDescriptionArguments());
		return spec;
	}

}
