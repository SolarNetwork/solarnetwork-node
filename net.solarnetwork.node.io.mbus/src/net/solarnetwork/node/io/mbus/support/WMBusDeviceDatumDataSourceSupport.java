/* ==================================================================
 * WMBusDatumDataSource.java - 06/07/2020 15:17:51 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus.support;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.mbus.WMBusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.util.OptionalService;

public class WMBusDeviceDatumDataSourceSupport extends DatumDataSourceSupport {

	private OptionalService<WMBusNetwork> wmbusNetwork;

	/**
	 * Get the configured {@link WMBusNetwork}.
	 * 
	 * @return the modbus network
	 */
	public OptionalService<WMBusNetwork> getWMBusNetwork() {
		return wmbusNetwork;
	}

	/**
	 * Set the {@link WMBusNetwork} to use.
	 * 
	 * @param wmbusNetwork
	 *        the WMBus network
	 */
	public void setWMBusNetwork(OptionalService<WMBusNetwork> wmbusNetwork) {
		this.wmbusNetwork = wmbusNetwork;
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['UID']} properties.
	 * 
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getWMBusNetworkSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(
				new BasicTextFieldSettingSpecifier("wMBusNetwork.propertyFilters['UID']", "M-Bus Port"));
		results.add(new BasicTextFieldSettingSpecifier("secondaryAddress", ""));
		results.add(new BasicTextFieldSettingSpecifier("key", ""));
		return results;
	}

}
