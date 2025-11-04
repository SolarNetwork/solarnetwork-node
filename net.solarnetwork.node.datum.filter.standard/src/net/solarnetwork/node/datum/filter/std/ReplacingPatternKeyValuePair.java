/* ==================================================================
 * ReplacingPatternKeyValuePair.java - 5/11/2025 6:05:12 am
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

package net.solarnetwork.node.datum.filter.std;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Extension of {@link PatternKeyValuePair} to support key replacement.
 *
 * @author matt
 * @version 1.0
 * @since 4.1
 */
public class ReplacingPatternKeyValuePair extends PatternKeyValuePair {

	@Serial
	private static final long serialVersionUID = 4019461396465235891L;

	/** The key replacement value. */
	private String replacement;

	/**
	 * Constructor.
	 */
	public ReplacingPatternKeyValuePair() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 */
	public ReplacingPatternKeyValuePair(String key, String value) {
		super(key, value);
	}

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 * @param replacement
	 *        the replacement
	 */
	public ReplacingPatternKeyValuePair(String key, String value, String replacement) {
		super(key, value);
		this.replacement = replacement;
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
		List<SettingSpecifier> result = new ArrayList<>(3);
		result.addAll(PatternKeyValuePair.settings(prefix));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "replacement", null));
		return result;
	}

	/**
	 * Get the replacement.
	 *
	 * @return the replacement
	 */
	public String getReplacement() {
		return replacement;
	}

	/**
	 * Set the replacement.
	 *
	 * @param replacement
	 *        the replacement to set
	 */
	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	/**
	 * Test if a replacement is configured.
	 *
	 * @return {@code true} if a non-blank {@code replacement} value is
	 *         configured
	 */
	public boolean hasReplacement() {
		final String r = getReplacement();
		return (r != null && !r.isBlank());
	}

}
