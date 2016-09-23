/* ==================================================================
 * SetupResourceUtils.java - 23/09/2016 10:20:14 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Utility methods for setup resources.
 * 
 * @author matt
 * @version 1.0
 */
public final class SetupResourceUtils {

	private SetupResourceUtils() {
		// don't construct me
	}

	/**
	 * A pattern to match locale specifications in filenames. Matches with 2
	 * groups: the language, the country. The country is a nested group, so is
	 * actually referred by as group {code 3}.
	 */
	public static final Pattern LOCALE_PAT = Pattern.compile("_([a-z]{2,3})(_([A-Z]{2,3}))?(?:\\..*)$");

	/**
	 * Get a default mapping of file extensions to associated content types.
	 */
	public static final Map<String, String> DEFAULT_FILENAME_EXTENSION_CONTENT_TYPES = defaultFilenameExtensionContentTypeMap();

	private static Map<String, String> defaultFilenameExtensionContentTypeMap() {
		Map<String, String> m = new HashMap<String, String>();
		m.put("js", SetupResource.JAVASCRIPT_CONTENT_TYPE);
		m.put("css", SetupResource.CSS_CONTENT_TYPE);
		m.put("html", SetupResource.HTML_CONTENT_TYPE);
		m.put("txt", "text/plain");
		m.put("xml", "text/xml");
		return Collections.unmodifiableMap(m);
	}

	/**
	 * Get a {@link Locale} based on a path.
	 * 
	 * The path is expected to follow {@link java.util.ResourceBundle} naming
	 * conventions, for example {@code file.txt} for no locale,
	 * {@code file_en.txt} for a specific language, and {@code file_en_US.txt}
	 * for a language plus country variant.
	 * 
	 * @param path
	 *        The path to derive a locale from.
	 * @return The Locale, or {@code null} if not appropriate or none found.
	 */
	public static Locale localeForPath(String path) {
		if ( path == null ) {
			return null;
		}
		Matcher m = LOCALE_PAT.matcher(path);
		if ( m.find() ) {
			String lang = m.group(1);
			String country = m.group(3);
			if ( country != null ) {
				return new Locale(lang, country);
			}
			return localeForLanguage(lang);
		}
		return null;
	}

	/**
	 * Get a base filename for a given path, without any extension or locale
	 * specifier.
	 * 
	 * @param path
	 *        The path to get the base filename from.
	 * @return The base filename, or {@code null} if {@code path} is
	 *         {@code null}.
	 */
	public static String baseFilenameForPath(String path) {
		if ( path == null ) {
			return null;
		}
		Matcher m = LOCALE_PAT.matcher(path);
		if ( m.find() ) {
			int end = m.end(2);
			if ( end < 0 ) {
				end = m.end(1);
			}
			String result = StringUtils.getFilename(path.substring(0, m.start())) + path.substring(end);
			return result;
		}
		return StringUtils.getFilename(path);
	}

	/**
	 * Get a locale for just a language.
	 * 
	 * @param lang
	 *        The language to get the locale for.
	 * @return The Locale, or {@code null} if {@code lang} is {@code null}.
	 */
	public static Locale localeForLanguage(String lang) {
		if ( lang == null ) {
			return null;
		}
		if ( Locale.ENGLISH.getLanguage().equals(lang) ) {
			return Locale.ENGLISH;
		}
		return new Locale(lang);
	}

	/**
	 * Assign a score to a resource for how closely it matches the desired
	 * locale.
	 * 
	 * @param rsrc
	 *        The resource to test
	 * @param desiredLocale
	 *        The desired locale.
	 * @param defaultLocale
	 *        The default locale to use for resources where
	 *        {@link SetupResource#getLocale()} returns {@code null}.
	 * @return A matching score. Higher values more closely match. If
	 *         {@link Integer#MAX_VALUE} is returned then the match is exact.
	 */
	public static int localeScore(SetupResource rsrc, Locale desiredLocale, Locale defaultLocale) {
		if ( rsrc == null ) {
			return Integer.MIN_VALUE;
		}

		if ( defaultLocale == null ) {
			defaultLocale = Locale.getDefault();
		}
		if ( desiredLocale == null ) {
			desiredLocale = defaultLocale;
		}

		Locale rsrcLocale = rsrc.getLocale();
		if ( rsrcLocale == null ) {
			rsrcLocale = defaultLocale;
		}

		if ( desiredLocale.equals(rsrcLocale) ) {
			return Integer.MAX_VALUE;
		}

		boolean lMatch = desiredLocale.getLanguage().equals(rsrcLocale.getLanguage());
		boolean cMatch = desiredLocale.getCountry().equals(rsrcLocale.getCountry());
		if ( lMatch && cMatch ) {
			return 2;
		} else if ( lMatch ) {
			return 1;
		}

		return -1;
	}

}
