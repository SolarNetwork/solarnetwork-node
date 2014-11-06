/* ==================================================================
 * OBRResourcePluginInfo.java - Apr 22, 2014 7:17:31 AM
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
import java.util.Map;
import net.solarnetwork.node.setup.PluginInfo;
import org.osgi.service.obr.Resource;

/**
 * PluginInfo implementation that wraps an OBR {link Resource}.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRResourcePluginInfo implements PluginInfo {

	private final Resource resource;

	/**
	 * Construct with a resource.
	 * 
	 * @param resource
	 *        the resource
	 */
	public OBRResourcePluginInfo(Resource resource) {
		super();
		this.resource = resource;
	}

	@Override
	public String getLocalizedName(Locale locale) {
		return getName();
	}

	@Override
	public String getLocalizedDescription(Locale locale) {
		return getDescription();
	}

	@Override
	public String getName() {
		return resource.getPresentationName();
	}

	@Override
	public String getDescription() {
		@SuppressWarnings("unchecked")
		Map<String, ?> props = resource.getProperties();
		Object descr = props.get(Resource.DESCRIPTION);
		return (descr instanceof String ? (String) descr : null);
	}
}
