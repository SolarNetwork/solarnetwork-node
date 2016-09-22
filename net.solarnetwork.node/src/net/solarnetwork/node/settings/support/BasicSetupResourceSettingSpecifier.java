/* ==================================================================
 * BasicSetupResourceSettingSpecifier.java - 21/09/2016 12:36:54 PM
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

package net.solarnetwork.node.settings.support;

import java.util.Map;
import net.solarnetwork.node.settings.SetupResourceSettingSpecifier;
import net.solarnetwork.node.setup.SetupResourceProvider;

/**
 * Basic implementation of {@link SetupResourceSettingSpecifier}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicSetupResourceSettingSpecifier extends BaseSettingSpecifier
		implements SetupResourceSettingSpecifier {

	private final SetupResourceProvider provider;
	private final Map<String, ?> props;

	/**
	 * Construct without properties.
	 * 
	 * @param provider
	 *        The provider to use.
	 */
	public BasicSetupResourceSettingSpecifier(SetupResourceProvider provider) {
		this(provider, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param provider
	 *        The provider to use.
	 * @param properties
	 *        The properties to use.
	 */
	public BasicSetupResourceSettingSpecifier(SetupResourceProvider provider,
			Map<String, ?> properties) {
		super();
		this.provider = provider;
		this.props = properties;
	}

	@Override
	public SetupResourceProvider getSetupResourceProvider() {
		return provider;
	}

	@Override
	public Map<String, ?> getSetupResourceProperties() {
		return props;
	}

}
