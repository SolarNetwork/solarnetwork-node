/* ==================================================================
 * MappableSpecifier.java - 27/06/2015 2:11:00 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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
import java.util.List;

/**
 * API for a specifier that can be mapped to some other specifier.
 * 
 * @author matt
 * @version 1.1
 */
public interface MappableSpecifier {

	/**
	 * API to dynamically map a key to a new key.
	 */
	interface Mapper {

		/**
		 * Map an input key to an output key.
		 * 
		 * @param key
		 *        the key to map
		 * @return the mapped key
		 */
		String mapKey(String key);

	}

	/**
	 * Return a setting specifier mapped to a new path.
	 * 
	 * <p>
	 * This is to allow delegating setting specifiers to re-map the key.
	 * </p>
	 * 
	 * @param prefix
	 *        the new prefix to add to the key
	 * @return the new instance
	 */
	SettingSpecifier mappedTo(String prefix);

	/**
	 * Return a setting specifier mapped to a new path, using a format template.
	 * 
	 * @param template
	 *        the format template
	 * @return the new instance
	 */
	SettingSpecifier mappedWithPlaceholer(String template);

	/**
	 * Return a setting specifier mapped to a new path, using a {@link Mapper}.
	 * 
	 * @param mapper
	 *        the mapper
	 * @return the new instance
	 */
	SettingSpecifier mappedWithMapper(Mapper mapper);

	/**
	 * Map a list of settings to a new prefix.
	 * 
	 * @param settings
	 *        the settings to map
	 * @param prefix
	 *        the prefix to add to each setting
	 * @return the mapped settings
	 * @since 1.1
	 */
	static List<SettingSpecifier> mapTo(List<SettingSpecifier> settings, String prefix) {
		if ( settings == null || prefix == null || prefix.isEmpty() ) {
			return settings;
		}
		List<SettingSpecifier> result = new ArrayList<>(settings.size());
		for ( SettingSpecifier setting : settings ) {
			if ( setting instanceof MappableSpecifier ) {
				result.add(((MappableSpecifier) setting).mappedTo(prefix));
			} else {
				result.add(setting);
			}
		}
		return result;
	}

	/**
	 * Map a list of settings with a template.
	 * 
	 * @param settings
	 *        the settings to map
	 * @param template
	 *        the template to use
	 * @return the mapped settings
	 * @since 1.1
	 */
	static List<SettingSpecifier> mapWithPlaceholder(List<SettingSpecifier> settings, String template) {
		if ( settings == null || template == null ) {
			return settings;
		}
		List<SettingSpecifier> result = new ArrayList<>(settings.size());
		for ( SettingSpecifier setting : settings ) {
			if ( setting instanceof MappableSpecifier ) {
				result.add(((MappableSpecifier) setting).mappedWithPlaceholer(template));
			} else {
				result.add(setting);
			}
		}
		return result;
	}

	/**
	 * Map a list of settings with a {@code Mapper}.
	 * 
	 * @param settings
	 *        the settings to map
	 * @param mapper
	 *        the {@code Mapper} to use
	 * @return the mapped settings
	 * @since 1.1
	 */
	static List<SettingSpecifier> mapWithMapper(List<SettingSpecifier> settings, Mapper mapper) {
		if ( settings == null || mapper == null ) {
			return settings;
		}
		List<SettingSpecifier> result = new ArrayList<>(settings.size());
		for ( SettingSpecifier setting : settings ) {
			if ( setting instanceof MappableSpecifier ) {
				result.add(((MappableSpecifier) setting).mappedWithMapper(mapper));
			} else {
				result.add(setting);
			}
		}
		return result;
	}

}
