/* ==================================================================
 * BasicTitleSettingSpecifier.java - Mar 12, 2012 10:05:04 AM
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

import java.util.Collections;
import java.util.Map;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.TitleSettingSpecifier;

/**
 * Basic implemtation of {@link TitleSettingSpecifier}.
 * 
 * @author matt
 * @version 1.2
 */
public class BasicTitleSettingSpecifier extends BaseKeyedSettingSpecifier<String> implements
		TitleSettingSpecifier {

	private Map<String, String> valueTitles;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 */
	public BasicTitleSettingSpecifier(String key, String defaultValue) {
		super(key, defaultValue);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 * @param trans
	 *        the transient flag value
	 */
	public BasicTitleSettingSpecifier(String key, String defaultValue, boolean trans) {
		super(key, defaultValue, trans);
	}

	@Override
	public Map<String, String> getValueTitles() {
		return this.valueTitles;
	}

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicTitleSettingSpecifier spec = new BasicTitleSettingSpecifier(String.format(template,
				getKey()), getDefaultValue());
		spec.setTitle(getTitle());
		spec.setValueTitles(valueTitles);
		return spec;
	}

	@SuppressWarnings("deprecation")
	@Override
	public SettingSpecifier mappedWithMapper(Mapper mapper) {
		return mappedWithMapper((MappableSpecifier.Mapper) mapper);
	}

	@Override
	public SettingSpecifier mappedWithMapper(MappableSpecifier.Mapper mapper) {
		BasicTitleSettingSpecifier spec = new BasicTitleSettingSpecifier(mapper.mapKey(getKey()),
				getDefaultValue());
		spec.setTitle(getTitle());
		spec.setValueTitles(valueTitles);
		return spec;
	}

	public void setValueTitles(Map<String, String> valueTitles) {
		this.valueTitles = (valueTitles == null ? null : Collections.unmodifiableMap(valueTitles));
	}

}
