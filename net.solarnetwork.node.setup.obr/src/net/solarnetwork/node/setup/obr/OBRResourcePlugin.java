/* ==================================================================
 * OBRResourcePlugin.java - Apr 21, 2014 5:36:11 PM
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

package net.solarnetwork.node.setup.obr;

import java.util.Locale;
import net.solarnetwork.node.setup.BundlePluginVersion;
import net.solarnetwork.node.setup.LocalizedPluginInfo;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginInfo;
import net.solarnetwork.node.setup.PluginVersion;
import org.osgi.framework.Version;
import org.osgi.service.obr.Resource;

/**
 * Plugin implementation that wraps an OBR {link Resource}.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRResourcePlugin implements Plugin {

	private final Resource resource;
	private final BundlePluginVersion version;
	private final OBRResourcePluginInfo info;
	private final boolean coreFeature;

	/**
	 * Construct with a {@link Resource}.
	 * 
	 * @param the
	 *        Resource to wrap
	 * @param coreFeature
	 *        the core feature flag
	 */
	public OBRResourcePlugin(Resource resource, boolean coreFeature) {
		super();
		this.resource = resource;
		this.coreFeature = coreFeature;
		this.version = new BundlePluginVersion(getResourceVersion());
		this.info = new OBRResourcePluginInfo(resource);
	}

	@Override
	public String getUID() {
		return resource.getSymbolicName();
	}

	@Override
	public PluginVersion getVersion() {
		return version;
	}

	/**
	 * Get the underlying {@link Resource#getVersion()} directly.
	 * 
	 * @return the version
	 */
	public Version getResourceVersion() {
		return resource.getVersion();
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
	 * Get the Resource associated with this Plugin.
	 * 
	 * @return the Resource
	 */
	Resource getResource() {
		return resource;
	}

}
