/* ==================================================================
 * PatternKeyValuePair.java - 17/02/2022 2:58:04 PM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.std;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Extension of {@link KeyValuePair} where the key is treated as a pattern.
 *
 * @author matt
 * @version 1.1
 */
public class PatternKeyValuePair extends KeyValuePair {

	private static final long serialVersionUID = -6339149657049288089L;

	/** The key regular expression. */
	private Pattern keyPattern;

	/**
	 * Default constructor.
	 */
	public PatternKeyValuePair() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 */
	public PatternKeyValuePair(String key, String value) {
		super();
		setKey(key);
		setValue(value);
	}

	/**
	 * Get settings for configuring an instance of this class.
	 *
	 * @param prefix
	 *        the optional prefix to use
	 * @return the settings
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		prefix = (prefix != null ? prefix : "");
		List<SettingSpecifier> result = new ArrayList<>(2);
		result.add(new BasicTextFieldSettingSpecifier(prefix + "key", null));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "value", null));
		return result;
	}

	@Override
	public void setKey(String key) {
		super.setKey(key);
		try {
			keyPattern = Pattern.compile(key);
		} catch ( PatternSyntaxException e ) {
			// ignore
			keyPattern = null;
		}
	}

	/**
	 * Test if a string matches the key pattern.
	 *
	 * @param k
	 *        the string to test
	 * @return if the string does not match, {@literal null}; otherwise an array
	 *         whose first element is the key and any additional elements are
	 *         pattern placeholder values
	 */
	public String[] keyMatches(final String k) {
		if ( k == null ) {
			return null;
		}
		final Pattern p = this.keyPattern;
		if ( p != null ) {
			Matcher m = p.matcher(k);
			if ( m.find() ) {
				int groupCount = m.groupCount();
				String[] result = new String[1 + groupCount];
				result[0] = k;
				for ( int i = 1; i <= groupCount; i++ ) {
					result[i] = m.group(i);
				}
				return result;
			}
			return null;
		}
		return (k.equals(getKey()) ? new String[] { k } : null);
	}

	/**
	 * Expand a matching key as a template string.
	 *
	 * <p>
	 * This method works with the result of {@link #keyMatches(String)} to
	 * expand the first value of {@code captureGroups} as a template string
	 * using the remaining capture group values as parameters. The capture
	 * groups will be made available as parameters named as numbers, starting
	 * with {@code {1}}. The {@code propertyName} will be made available as a
	 * parameter named {@code {p}}.
	 *
	 * @param propertyName
	 *        the property name, to use as the {@code {p}} parameter
	 * @param capturedGroups
	 *        previously captured groups, as returned by
	 *        {@link #keyMatches(String)}
	 * @return the expanded
	 * @see StringUtils#expandTemplateString(String, Map)
	 * @since 1.1
	 */
	public static String expand(final String propertyName, final String[] capturedGroups) {
		Map<String, Object> params = new HashMap<>(capturedGroups.length);
		params.put("p", propertyName);
		for ( int i = 1; i < capturedGroups.length; i++ ) {
			params.put(String.valueOf(i), capturedGroups[i]);
		}
		return StringUtils.expandTemplateString(capturedGroups[0], params);
	}

}
