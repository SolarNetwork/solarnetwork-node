/* ==================================================================
 * BasicGroupSettingSpecifier.java - Mar 12, 2012 9:58:03 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.settings.GroupSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Basic implementation of {@link GroupSettingSpecifier}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicGroupSettingSpecifier extends BaseSettingSpecifier implements GroupSettingSpecifier {

	private final String key;
	private final String footerText;
	private final List<SettingSpecifier> groupSettings;
	private final boolean dynamic;

	/**
	 * Construct without a key. The {@code dynamic} property will be set to
	 * <em>false</em>.
	 * 
	 * @param settings
	 *        The group settings.
	 */
	public BasicGroupSettingSpecifier(List<SettingSpecifier> settings) {
		this(null, settings, false, null);
	}

	/**
	 * Construct with the group settings. The {@code dynamic} property will be
	 * set to <em>false</em>.
	 * 
	 * @param groupKey
	 *        The key for the entire group.
	 * @param settings
	 *        The group settings.
	 */
	public BasicGroupSettingSpecifier(String groupKey, List<SettingSpecifier> settings) {
		this(groupKey, settings, false, null);
	}

	/**
	 * Construct with settings and dynamic flag.
	 * 
	 * @param groupKey
	 *        The key for the entire group.
	 * @param settings
	 *        The group settings.
	 * @param dynamic
	 *        The dynamic flag.
	 */
	public BasicGroupSettingSpecifier(String groupKey, List<SettingSpecifier> settings, boolean dynamic) {
		this(groupKey, settings, dynamic, null);
	}

	/**
	 * Construct with values.
	 * 
	 * @param groupKey
	 *        The key for the entire group.
	 * @param settings
	 *        The group settings.
	 * @param dynamic
	 *        The dynamic flag.
	 * @param footerText
	 *        The footer text.
	 */
	public BasicGroupSettingSpecifier(String groupKey, List<SettingSpecifier> settings, boolean dynamic,
			String footerText) {
		super();
		this.key = groupKey;
		this.groupSettings = Collections.unmodifiableList(settings);
		this.dynamic = dynamic;
		this.footerText = footerText;
	}

	@Override
	public String getFooterText() {
		return this.footerText;
	}

	@Override
	public List<SettingSpecifier> getGroupSettings() {
		return this.groupSettings;
	}

	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public String getKey() {
		return key;
	}

}
