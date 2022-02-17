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
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Extension of {@link KeyValuePair} where the key is treated as a pattern.
 * 
 * @author matt
 * @version 1.0
 */
public class PatternKeyValuePair extends KeyValuePair {

	private static final long serialVersionUID = -6339149657049288089L;

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
	 * @return {@literal true} if {@code k} matches the key pattern
	 */
	public boolean keyMatches(String k) {
		if ( k == null ) {
			return false;
		}
		final Pattern p = this.keyPattern;
		if ( p != null ) {
			return p.matcher(k).find();
		}
		return k.equals(getKey());
	}

}
