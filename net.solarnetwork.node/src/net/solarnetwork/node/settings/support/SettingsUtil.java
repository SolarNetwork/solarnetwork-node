/* ==================================================================
 * SettingsUtil.java - 26/06/2015 2:46:00 pm
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

package net.solarnetwork.node.settings.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.settings.GroupSettingSpecifier;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Helper utilities for settings.
 * 
 * @author matt
 * @version 1.1
 */
public final class SettingsUtil {

	private SettingsUtil() {
		// Do not construct me.
	}

	/**
	 * API to map a list element into a set of {@link SettingSpecifier} objects.
	 * 
	 * @param <T>
	 *        The collection type.
	 */
	public interface KeyedListCallback<T> {

		/**
		 * Map a single list element value into one or more
		 * {@link SettingSpecifier} objects.
		 * 
		 * @param value
		 *        The list element value.
		 * @param index
		 *        The list element index.
		 * @param key
		 *        An indexed key prefix to use for the grouped settings.
		 * @return The settings.
		 */
		public Collection<SettingSpecifier> mapListSettingKey(T value, int index, String key);

	}

	/**
	 * Get a dynamic list {@link GroupSettingSpecifier}.
	 * 
	 * @param collection
	 *        The collection to turn into settings.
	 * @param mapper
	 *        A helper to map individual elements into settings.
	 * @return The resulting {@link GroupSettingSpecifier}.
	 */
	public static <T> BasicGroupSettingSpecifier dynamicListSettingSpecifier(String key,
			Collection<T> collection, KeyedListCallback<T> mapper) {
		List<SettingSpecifier> listStringGroupSettings;
		if ( collection == null ) {
			listStringGroupSettings = Collections.emptyList();
		} else {
			final int len = collection.size();
			listStringGroupSettings = new ArrayList<SettingSpecifier>(len);
			int i = 0;
			for ( T value : collection ) {
				Collection<SettingSpecifier> res = mapper.mapListSettingKey(value, i,
						key + "[" + i + "]");
				i++;
				if ( res != null ) {
					listStringGroupSettings.addAll(res);
				}
			}
		}
		return new BasicGroupSettingSpecifier(key, listStringGroupSettings, true);
	}

	/**
	 * Add a prefix to the keys of all {@link MappableSpecifier} settings.
	 * 
	 * @param settings
	 *        the settings to map
	 * @param prefix
	 *        the prefix to add to all {@link MappableSpecifier} settings
	 * @return list of mapped settings, or {@literal null} if {@code settings}
	 *         is {@literal null}
	 * @since 1.1
	 */
	public static List<SettingSpecifier> mappedWithPrefix(List<SettingSpecifier> settings,
			String prefix) {
		if ( settings == null || settings.isEmpty() || prefix == null || prefix.isEmpty() ) {
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
	 * Adapt a list of core settings into (legacy) node settings.
	 * 
	 * <p>
	 * This method is designed to support the transition of node settings to
	 * core settings.
	 * </p>
	 * 
	 * @param settings
	 * @return the adapted settings, never {@literal null}
	 * @since 1.1
	 * @see #adapt(net.solarnetwork.settings.SettingSpecifier)
	 */
	public static List<SettingSpecifier> adapt(
			List<net.solarnetwork.settings.SettingSpecifier> settings) {
		if ( settings == null || settings.isEmpty() ) {
			return Collections.emptyList();
		}
		List<SettingSpecifier> result = new ArrayList<>(settings.size());
		for ( net.solarnetwork.settings.SettingSpecifier coreSetting : settings ) {
			SettingSpecifier adapted = adapt(coreSetting);
			if ( adapted != null ) {
				result.add(adapted);
			}
		}
		return result;
	}

	/**
	 * Adapt a single core setting to a (legacy) node setting.
	 * 
	 * <p>
	 * This method is designed to support the transition of node settings to
	 * core settings.
	 * </p>
	 * 
	 * @param setting
	 *        the core setting to adapt
	 * @return new setting instance, or {@literal null} if not supported
	 * @since 1.1
	 */
	public static SettingSpecifier adapt(net.solarnetwork.settings.SettingSpecifier setting) {
		SettingSpecifier result = null;
		if ( setting instanceof net.solarnetwork.settings.GroupSettingSpecifier ) {
			net.solarnetwork.settings.GroupSettingSpecifier coreGroup = (net.solarnetwork.settings.GroupSettingSpecifier) setting;
			BasicGroupSettingSpecifier group = new BasicGroupSettingSpecifier(coreGroup.getKey(),
					adapt(coreGroup.getGroupSettings()), coreGroup.isDynamic(),
					coreGroup.getFooterText());
			group.setTitle(coreGroup.getTitle());
			result = group;
		} else if ( setting instanceof net.solarnetwork.settings.ParentSettingSpecifier ) {
			net.solarnetwork.settings.ParentSettingSpecifier coreGroup = (net.solarnetwork.settings.ParentSettingSpecifier) setting;
			BasicParentSettingSpecifier group = new BasicParentSettingSpecifier();
			group.setTitle(coreGroup.getTitle());
			group.setChildSettings(adapt(coreGroup.getChildSettings()));
			result = group;
		} else if ( setting instanceof net.solarnetwork.settings.SliderSettingSpecifier ) {
			net.solarnetwork.settings.SliderSettingSpecifier core = (net.solarnetwork.settings.SliderSettingSpecifier) setting;
			BasicSliderSettingSpecifier s = new BasicSliderSettingSpecifier(core.getKey(),
					core.getDefaultValue(), core.getMinimumValue(), core.getMaximumValue(),
					core.getStep());
			s.setDescriptionArguments(core.getDescriptionArguments());
			s.setTitle(core.getTitle());
			s.setTransient(core.isTransient());
			result = s;
		} else if ( setting instanceof net.solarnetwork.settings.TextFieldSettingSpecifier ) {
			net.solarnetwork.settings.TextFieldSettingSpecifier core = (net.solarnetwork.settings.TextFieldSettingSpecifier) setting;
			BasicTextFieldSettingSpecifier s;
			if ( setting instanceof net.solarnetwork.settings.CronExpressionSettingSpecifier ) {
				s = new BasicCronExpressionSettingSpecifier(core.getKey(), core.getDefaultValue());
			} else if ( setting instanceof net.solarnetwork.settings.MultiValueSettingSpecifier ) {
				s = new BasicMultiValueSettingSpecifier(core.getKey(), core.getDefaultValue());
			} else if ( setting instanceof net.solarnetwork.settings.RadioGroupSettingSpecifier ) {
				net.solarnetwork.settings.RadioGroupSettingSpecifier cc = (net.solarnetwork.settings.RadioGroupSettingSpecifier) setting;
				BasicRadioGroupSettingSpecifier ss = new BasicRadioGroupSettingSpecifier(core.getKey(),
						core.getDefaultValue());
				ss.setFooterText(cc.getFooterText());
				s = ss;
			} else {
				s = new BasicTextFieldSettingSpecifier(core.getKey(), core.getDefaultValue(),
						core.isSecureTextEntry());
			}
			s.setDescriptionArguments(core.getDescriptionArguments());
			s.setTitle(core.getTitle());
			s.setTransient(core.isTransient());
			s.setValueTitles(s.getValueTitles());
			result = s;
		} else if ( setting instanceof net.solarnetwork.settings.TitleSettingSpecifier ) {
			net.solarnetwork.settings.TitleSettingSpecifier core = (net.solarnetwork.settings.TitleSettingSpecifier) setting;
			BasicTitleSettingSpecifier s = new BasicTitleSettingSpecifier(core.getKey(),
					core.getDefaultValue(), core.isTransient());
			s.setDescriptionArguments(core.getDescriptionArguments());
			s.setTitle(core.getTitle());
			s.setValueTitles(core.getValueTitles());
			result = s;
		} else if ( setting instanceof net.solarnetwork.settings.ToggleSettingSpecifier ) {
			net.solarnetwork.settings.ToggleSettingSpecifier core = (net.solarnetwork.settings.ToggleSettingSpecifier) setting;
			BasicToggleSettingSpecifier s = new BasicToggleSettingSpecifier(core.getKey(),
					core.getDefaultValue(), core.isTransient());
			s.setDescriptionArguments(core.getDescriptionArguments());
			s.setFalseValue(core.getFalseValue());
			s.setTitle(core.getTitle());
			s.setTrueValue(core.getTrueValue());
			result = s;
		}
		return result;
	}

}
