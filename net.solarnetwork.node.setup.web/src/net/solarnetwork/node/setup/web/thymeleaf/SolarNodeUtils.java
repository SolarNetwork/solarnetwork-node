/* ==================================================================
 * SolarNodeUtils.java - 18/06/2025 6:17:26 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.thymeleaf;

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.EscapedAttributeUtils;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.settings.MarkupSetting;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.util.ClassUtils;

/**
 * Helper for SolarNode object utilities.
 *
 * @author matt
 * @version 1.0
 */
public final class SolarNodeUtils {

	/** The instance. */
	public static final SolarNodeUtils INSTANCE = new SolarNodeUtils();

	private static final Pattern IndexKeysPattern = Pattern.compile("\\[\\d+\\]");

	/**
	 * A cache of datum type mappings.
	 *
	 * <p>
	 * The {@link #settingTypes(Class)} method populates this cache.
	 * </p>
	 */
	private final ConcurrentMap<Class<?>, String[]> settingTypeCache = new ConcurrentHashMap<Class<?>, String[]>();

	private SolarNodeUtils() {
		super();
	}

	/**
	 * Return {@literal true} if {@code o} is an instance of the class
	 * {@code className}.
	 *
	 * @param o
	 *        the object to test
	 * @param className
	 *        the class name to test
	 * @return boolean
	 */
	public boolean instanceOf(Object o, String className) {
		if ( o == null || className == null ) {
			return false;
		}
		Class<?> clazz;
		try {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
			return clazz.isInstance(o);
		} catch ( ClassNotFoundException e ) {
			return false;
		}
	}

	/**
	 * Get a message from a specific {@link MessageSource}.
	 *
	 * @param key
	 *        the message key
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the desired locale
	 * @return the message
	 */
	public Object message(String key, MessageSource messageSource, Locale locale) {
		return message(key, null, null, messageSource, locale);
	}

	/**
	 * Get a message from a specific {@link MessageSource}.
	 *
	 * @param key
	 *        the message key
	 * @param defaultMessage
	 *        the default message
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the desired locale
	 * @return the message
	 */
	public Object message(String key, String defaultMessage, MessageSource messageSource,
			Locale locale) {
		return message(key, null, defaultMessage, messageSource, locale);
	}

	/**
	 * Get a message from a specific {@link MessageSource}.
	 *
	 * @param key
	 *        the message key
	 * @param arguments
	 *        the message arguments
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the desired locale
	 * @return the message
	 */
	public Object message(String key, Object[] arguments, MessageSource messageSource, Locale locale) {
		return message(key, arguments, null, messageSource, locale);
	}

	/**
	 * Get a message from a specific {@link MessageSource}.
	 *
	 * @param key
	 *        the message key
	 * @param arguments
	 *        the message arguments
	 * @param defaultMessage
	 *        the default message
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the desired locale
	 * @return the message
	 */
	public Object message(String key, Object[] arguments, String defaultMessage,
			MessageSource messageSource, Locale locale) {
		if ( key == null || messageSource == null || key.isBlank() ) {
			return null;
		}
		String msg = messageSource.getMessage(key, arguments, null, locale);
		if ( msg == null ) {
			// try with index subscripts removed
			String keyNoIndcies = IndexKeysPattern.matcher(key).replaceAll("Item");
			if ( !keyNoIndcies.equals(key) ) {
				Object[] params = arguments;
				msg = messageSource.getMessage(keyNoIndcies, params, null, locale);
			}
		}
		return (msg != null ? msg : defaultMessage != null ? defaultMessage : "???" + key + "???");
	}

	/**
	 * Get a setting value, optionally escaped.
	 *
	 * @param service
	 *        the settings service
	 * @param provider
	 *        the setting specifier provider
	 * @param setting
	 *        the setting specifier
	 * @return the current setting value
	 */
	public Object settingValue(SettingsService service, SettingSpecifierProvider provider,
			SettingSpecifier setting) {
		if ( service == null || provider == null || setting == null ) {
			return null;
		}
		Object val = service.getSettingValue(provider, setting);
		if ( val != null && setting instanceof MarkupSetting m && !m.isMarkup() ) {
			val = EscapedAttributeUtils.escapeAttribute(TemplateMode.HTML, val.toString());
		}
		return val;
	}

	/**
	 * Get the primary setting type name of a setting.
	 *
	 * @param setting
	 *        the setting to inspect
	 * @return the primary setting type
	 */
	public String settingType(SettingSpecifier setting) {
		String[] result = settingTypeCache.get(setting.getClass());
		if ( result != null ) {
			return (result.length > 0 ? result[0] : null);
		}

		Set<Class<?>> interfaces = ClassUtils.getAllNonJavaInterfacesForClassAsSet(setting.getClass());

		// remove all but SettingSpecifier extensions
		for ( Iterator<Class<?>> itr = interfaces.iterator(); itr.hasNext(); ) {
			Class<?> c = itr.next();
			if ( !SettingSpecifier.class.isAssignableFrom(c) ) {
				itr.remove();
			}
		}

		result = new String[interfaces.size()];
		int i = 0;
		for ( Class<?> intf : interfaces ) {
			result[i] = intf.getName();
			i++;
		}
		settingTypeCache.putIfAbsent(setting.getClass(), result);
		return (result.length > 0 ? result[0] : null);
	}

}
