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
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginInfo;
import net.solarnetwork.node.setup.PluginVersion;
import org.osgi.service.obr.Resource;

/**
 * Plugin implementation that wraps an OBR {link Resource}.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRResourcePlugin implements Plugin {

	private final Resource resource;

	/**
	 * Construct with a {@link Resource}.
	 * 
	 * @param the
	 *        Resource to wrap
	 */
	public OBRResourcePlugin(Resource resource) {
		super();
		this.resource = resource;
	}

	@Override
	public String getUID() {
		return resource.getSymbolicName();
	}

	@Override
	public PluginVersion getVersion() {
		return new OBRPluginVersion(resource.getVersion());
	}

	@Override
	public PluginInfo getInfo() {
		// OBR doesn't support localization... too bad
		return new PluginInfo() {

			@Override
			public String getLocalizedName(Locale locale) {
				return resource.getPresentationName();
			}

			@Override
			public String getLocalizedDescription(Locale locale) {
				// TODO: what can we return here?
				return "";
			}
		};
	}

}
