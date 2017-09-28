/* ==================================================================
 * DatumPropertyFilterConfig.java - 11/08/2017 4:44:29 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.samplefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Configuration object for datum property filtering.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumPropertyFilterConfig {

	private static final Logger LOG = LoggerFactory.getLogger(DatumPropertyFilterConfig.class);

	private String name;
	private Integer frequencySeconds;

	private Pattern namePattern;

	/**
	 * Default constructor.
	 */
	public DatumPropertyFilterConfig() {
		super();
	}

	/**
	 * Construct with properties.
	 * 
	 * @param name
	 *        the name regular expression
	 * @param frequencySeconds
	 *        the limit frequency, or {@literal null}
	 */
	public DatumPropertyFilterConfig(String name, Integer frequencySeconds) {
		super();
		setName(name);
		setFrequencySeconds(frequencySeconds);
	}

	/**
	 * Get setting specifiers for this class.
	 * 
	 * @param prefix
	 *        a prefix to add to all setting keys
	 * @return the settings
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();
		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "frequency", ""));
		return results;
	}

	@Override
	public String toString() {
		return "DatumPropertyFilterConfig{name=" + name + ", frequency=" + frequencySeconds + "}";
	}

	/**
	 * Get the datum property name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the datum property name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
		try {
			this.namePattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
		} catch ( PatternSyntaxException e ) {
			LOG.warn("Error compiling includePatterns regex [{}]", name, e);
		}

	}

	/**
	 * Get the name regular expression.
	 * 
	 * @return the name expression
	 */
	@SerializeIgnore
	public Pattern getNamePattern() {
		return namePattern;
	}

	/**
	 * Get the frequency limit in seconds, or {@literal null} for no limit.
	 * 
	 * @return the frequency seconds, or {@literal null}
	 */
	public Integer getFrequencySeconds() {
		return frequencySeconds;
	}

	/**
	 * Set the frequency limit in seconds.
	 * 
	 * @param frequencySeconds
	 *        the frequency to set, or {@literal null} for no limit
	 */
	public void setFrequencySeconds(Integer frequencySeconds) {
		this.frequencySeconds = frequencySeconds;
	}

	/**
	 * Alias for {@link #getFrequencySeconds()}.
	 * 
	 * @return the frequency seconds, or {@literal null}
	 */
	public Integer getFrequency() {
		return getFrequencySeconds();
	}

	/**
	 * Alias for {@link #setFrequencySeconds(Integer)}.
	 * 
	 * @param frequencySeconds
	 *        the frequency to set, or {@literal null} for no limit
	 */
	public void setFrequency(Integer frequencySeconds) {
		setFrequencySeconds(frequencySeconds);
	}
}
