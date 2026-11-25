/* ==================================================================
 * SettingsUtils.java - 17 Nov 2026 7:10:36 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.FileSettingSpecifier;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SetupResourceSettingSpecifier;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.GroupSettingSpecifier;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.settings.TextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * Helpers for settings.
 *
 * @author matt
 * @version 1.0
 */
public final class SettingsUtils {

	/** The title message key and info property name. */
	public static final String TITLE_PROP = "title";

	/** The description message key and info property name. */
	public static final String DESCRIPTION_PROP = "desc";

	/** A redacted setting value. */
	public static final String REDACTED_VALUE = "*****";

	/** A sensitive setting name pattern. */
	public static Pattern SENSITVE_PATTERN = Pattern.compile("(apikey|password|secret)",
			Pattern.CASE_INSENSITIVE);

	private SettingsUtils() {
		// not available
	}

	/**
	 * Generate a list of "factory info" map objects.
	 *
	 * @param factories
	 *        the factories
	 * @param locale
	 *        the desired locale, or {@code null} to use the system default
	 * @return the factory info list
	 */
	public static List<Map<String, Object>> factoryInfos(
			final List<SettingSpecifierProviderFactory> factories, final @Nullable Locale locale) {
		final Locale l = (locale != null ? locale : Locale.getDefault());
		final List<Map<String, Object>> results = new ArrayList<>(factories.size());
		for ( SettingSpecifierProviderFactory f : factories ) {
			Map<String, Object> props = new LinkedHashMap<>(2);
			props.put("id", f.getFactoryUid());
			String title = f.getDisplayName();
			if ( f.getMessageSource() != null ) {
				title = f.getMessageSource().getMessage(TITLE_PROP, null, title, l);
			}
			props.put(TITLE_PROP, title);
			results.add(props);
		}
		return results;
	}

	/**
	 * Generate a list of "provider info" map objects.
	 *
	 * @param providers
	 *        the providers
	 * @param locale
	 *        the desired locale, or {@code null} to use the system default
	 * @return the provider info list
	 */
	public static List<Map<String, Object>> providerInfos(final List<SettingSpecifierProvider> providers,
			final @Nullable Locale locale) {
		final Locale l = (locale != null ? locale : Locale.getDefault());
		final List<Map<String, Object>> results = new ArrayList<>(providers.size());
		for ( SettingSpecifierProvider p : providers ) {
			Map<String, Object> props = new LinkedHashMap<>(2);
			props.put("id", p.getSettingUid());
			String title = p.getDisplayName();
			if ( p.getMessageSource() != null ) {
				title = p.getMessageSource().getMessage(TITLE_PROP, null, title, l);
			}
			props.put(TITLE_PROP, title);
			results.add(props);
		}
		return results;
	}

	/**
	 * Generate a list of "setting info" map objects for a factory provider.
	 *
	 * @param service
	 *        the settings service to use
	 * @param provider
	 *        the factory provider
	 * @param specification
	 *        {@code true} for specification style settings, {@code false} for
	 *        active settings
	 * @param locale
	 *        a locale to render title and description values with in
	 *        specification mode, or {@code null} to omit
	 * @return the setting info list
	 */
	public static List<Map<String, Object>> factoryInstanceSettingInfos(final SettingsService service,
			final FactorySettingSpecifierProvider provider, final boolean specification,
			final @Nullable Locale locale) {
		return (specification ? resultSpecification(service, provider, locale)
				: resultSettings(service, provider));
	}

	/**
	 * Generate a list of "setting info" map objects for a settings provider.
	 *
	 * @param service
	 *        the settings service to use
	 * @param provider
	 *        the settings provider
	 * @param specification
	 *        {@code true} for specification style settings, {@code false} for
	 *        active settings
	 * @param locale
	 *        a locale to render title and description values with in
	 *        specification mode, or {@code null} to omit
	 * @return the setting info list
	 */
	public static List<Map<String, Object>> providerSettingInfos(final SettingsService service,
			final SettingSpecifierProvider provider, final boolean specification,
			final @Nullable Locale locale) {
		return (specification ? resultSpecification(service, provider, locale)
				: resultSettings(service, provider));
	}

	private static List<Map<String, Object>> resultSpecification(SettingsService service,
			SettingSpecifierProvider provider, @Nullable Locale locale) {
		final MessageSource msg = provider.getMessageSource();
		return SettingUtils.templateSpecification(provider, (spec, props) -> {
			if ( spec instanceof FileSettingSpecifier ) {
				FileSettingSpecifier file = (FileSettingSpecifier) spec;
				props.put("acceptableFileTypeSpecifiers", file.getAcceptableFileTypeSpecifiers());
				if ( file.isMultiple() ) {
					props.put("multiple", file.isMultiple());
				}
			} else if ( spec instanceof LocationLookupSettingSpecifier ) {
				LocationLookupSettingSpecifier look = (LocationLookupSettingSpecifier) spec;
				props.put("locationTypeKey", look.getLocationTypeKey());
			} else if ( spec instanceof SetupResourceSettingSpecifier ) {
				SetupResourceSettingSpecifier setup = (SetupResourceSettingSpecifier) spec;
				props.put("setupResourceProperties", setup.getSetupResourceProperties());
			}

			if ( locale != null && msg != null && spec instanceof KeyedSettingSpecifier<?> k ) {
				final String title = settingMessage(msg, k, ".key", locale);
				if ( title != null ) {
					props.put(TITLE_PROP, title);
				}
				final String desc = settingMessage(msg, k, ".desc", locale);
				if ( desc != null ) {
					props.put(DESCRIPTION_PROP, desc);
				}
			}
		});
	}

	private static final Pattern INDEX_KEYS_PATTERN = Pattern.compile("\\[\\d+\\]");

	private static @Nullable String settingMessage(final MessageSource msg,
			final KeyedSettingSpecifier<?> k, String suffix, Locale locale) {
		final String key = k.getKey() + suffix;

		String result = msg.getMessage(key, null, null, locale);
		if ( result == null ) {
			// try with index subscripts removed
			String keyNoIndcies = INDEX_KEYS_PATTERN.matcher(key).replaceAll("Item");
			if ( !keyNoIndcies.equals(key) ) {
				result = msg.getMessage(keyNoIndcies, null, null, locale);
			}
		}
		return result;
	}

	private static List<Map<String, Object>> resultSettings(SettingsService service,
			SettingSpecifierProvider provider) {
		String instanceId = provider instanceof FactorySettingSpecifierProvider
				? ((FactorySettingSpecifierProvider) provider).getFactoryInstanceUID()
				: null;
		List<SettingSpecifier> specs = provider.getSettingSpecifiers();
		List<Setting> settings = service.getSettings(
				instanceId != null ? provider.getSettingUid() : null,
				instanceId != null ? instanceId : provider.getSettingUid());
		Map<String, Setting> settingsMap;
		if ( settings != null && !settings.isEmpty() ) {
			settingsMap = new HashMap<>(settings.size());
			for ( Setting s : settings ) {
				// in a Setting, the key is the UID and type is the setting key
				settingsMap.put(s.getType(), s);
			}
		} else {
			settingsMap = Collections.emptyMap();
		}
		List<Map<String, Object>> resultSettings = new ArrayList<>(specs.size());
		for ( SettingSpecifier spec : specs ) {
			generateResultSetting(resultSettings, settingsMap, spec);
		}
		return resultSettings;
	}

	private static void generateResultSetting(final List<Map<String, Object>> resultSettings,
			final Map<String, Setting> settingsMap, final SettingSpecifier spec) {
		if ( spec instanceof KeyedSettingSpecifier<?> ) {
			KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
			if ( keyedSpec.isTransient() ) {
				return;
			}
			Map<String, Object> props = new LinkedHashMap<>(4);
			props.put("key", keyedSpec.getKey());
			Setting setting = settingsMap.get(keyedSpec.getKey());
			props.put("value",
					setting != null ? settingValue(setting, keyedSpec) : keyedSpec.getDefaultValue());
			if ( setting == null ) {
				props.put("default", true);
			}
			resultSettings.add(props);
		} else if ( spec instanceof GroupSettingSpecifier ) {
			GroupSettingSpecifier group = (GroupSettingSpecifier) spec;
			for ( SettingSpecifier groupSpec : group.getGroupSettings() ) {
				generateResultSetting(resultSettings, settingsMap, groupSpec);
			}
		}
	}

	private static @Nullable String settingValue(Setting setting, KeyedSettingSpecifier<?> spec) {
		String key = setting.getType();
		String value = setting.getValue();
		final boolean secure = (spec instanceof TextFieldSettingSpecifier
				&& ((TextFieldSettingSpecifier) spec).isSecureTextEntry());
		if ( value != null && (secure || (key != null && SENSITVE_PATTERN.matcher(key).find())) ) {
			value = REDACTED_VALUE;
		}
		return value;
	}

}
