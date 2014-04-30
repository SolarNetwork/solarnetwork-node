/* ==================================================================
 * BundlePlugin.java - Apr 22, 2014 8:22:09 PM
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
import net.solarnetwork.util.SerializeIgnore;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.osgi.framework.Bundle;

/**
 * Implementation of {@link Plugin} that wraps a {@link Bundle}.
 * 
 * @author matt
 * @version 1.0
 */
public class BundlePlugin implements Plugin {

	private final Bundle bundle;
	private final BundlePluginVersion version;
	private final BundlePluginInfo info;
	private final boolean coreFeature;

	/**
	 * Construct with a {@link Bundle}.
	 * 
	 * @param bundle
	 *        the bundle
	 */
	public BundlePlugin(Bundle bundle, boolean coreFeature) {
		super();
		this.bundle = bundle;
		this.coreFeature = coreFeature;
		this.version = new BundlePluginVersion(bundle.getVersion());
		this.info = new BundlePluginInfo(bundle);
	}

	@Override
	public String getUID() {
		return bundle.getSymbolicName();
	}

	@Override
	public PluginVersion getVersion() {
		return version;
	}

	@Override
	public PluginInfo getInfo() {
		return info;
	}

	@Override
	public PluginInfo getLocalizedInfo(Locale locale) {
		return new LocalizedPluginInfo(info, locale);
	}

	@Override
	public boolean isCoreFeature() {
		return coreFeature;
	}

	/**
	 * Get the Bundle associated with this plugin.
	 * 
	 * @return the Bundle
	 */
	@JsonIgnore
	@SerializeIgnore
	public Bundle getBundle() {
		return bundle;
	}

}
