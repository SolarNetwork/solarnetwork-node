/* ==================================================================
 * LocalizedPlugin.java - Apr 22, 2014 7:12:01 AM
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

package net.solarnetwork.node.setup;

import java.util.Locale;

/**
 * Localized version of {@link PluginInfo} so that calls to non-localized
 * JavaBean accessors return localized values.
 * 
 * @author matt
 * @version 1.0
 */
public class LocalizedPlugin implements Plugin {

	private final Plugin plugin;
	private final Locale locale;

	/**
	 * Construct with values.
	 * 
	 * @param plugin
	 *        the non-localized plugin to delegate to
	 * @param locale
	 *        the desired locale to use
	 */
	public LocalizedPlugin(Plugin plugin, Locale locale) {
		super();
		this.plugin = plugin;
		this.locale = locale;
	}

	@Override
	public String getUID() {
		return plugin.getUID();
	}

	@Override
	public PluginVersion getVersion() {
		return plugin.getVersion();
	}

	@Override
	public PluginInfo getInfo() {
		return new LocalizedPluginInfo(plugin.getInfo(), locale);
	}

	@Override
	public PluginInfo getLocalizedInfo(Locale otherLocale) {
		return plugin.getLocalizedInfo(otherLocale);
	}

}
