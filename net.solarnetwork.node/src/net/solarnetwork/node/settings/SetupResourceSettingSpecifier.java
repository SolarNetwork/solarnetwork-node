/* ==================================================================
 * SetupResourceSettingSpecifier.java - 21/09/2016 12:32:31 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings;

import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;

/**
 * Setting that renders a custom UI via {@link SetupResource} instnaces.
 * 
 * @author matt
 * @version 1.0
 */
public interface SetupResourceSettingSpecifier extends SettingSpecifier {

	/**
	 * Get the provider of setup resources for this specifier.
	 * 
	 * @return The resource provider.
	 */
	SetupResourceProvider getSetupResourceProvider();

}
