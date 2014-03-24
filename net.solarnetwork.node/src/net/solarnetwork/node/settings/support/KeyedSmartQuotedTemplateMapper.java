/* ==================================================================
 * KeyedSmartQuotedTemplateMapper.java - Mar 24, 2014 8:21:04 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.node.settings.KeyedSettingSpecifier.Mapper;

/**
 * Dynamically maps property keys to support nested collections.
 * 
 * <p>
 * Only if the key passed to {@link #mapKey(String)} is a <em>simple</em>
 * property, it will be quoted. A simple property is one named with only
 * letters, numbers, and underscore characters.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>template</dt>
 * <dd>A format template that accepts a single parameter to be within the
 * template quote.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class KeyedSmartQuotedTemplateMapper implements Mapper {

	private static final Pattern PROPERTY_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)(.*)");

	private String template = "trigger.jobDataMap['%s']";

	@Override
	public String mapKey(String key) {
		String quoteParam;
		String suffix = "";
		Matcher m = PROPERTY_PATTERN.matcher(key);
		if ( m.matches() ) {
			quoteParam = m.group(1);
			suffix = m.group(2);
		} else {
			quoteParam = key;
		}
		return String.format(template, quoteParam) + suffix;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

}
