/* ==================================================================
 * BundlePluginInfo.java - Apr 22, 2014 8:52:26 PM
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
import org.osgi.framework.Bundle;

/**
 * PluginInfo implementation that wraps a {@link Bundle}.
 * 
 * @author matt
 * @version 1.0
 */
public class BundlePluginInfo implements PluginInfo {

	private final Bundle bundle;

	/**
	 * Construct with a Bundle.
	 * 
	 * @param bundle
	 *        the bundle
	 */
	public BundlePluginInfo(Bundle bundle) {
		super();
		this.bundle = bundle;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalizedName(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalizedDescription(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

}
