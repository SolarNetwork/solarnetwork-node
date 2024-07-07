/* ==================================================================
 * WebBoxServiceDevice.java - 14/09/2020 5:18:46 PM
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

package net.solarnetwork.node.datum.sma.webbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.hw.sma.modbus.webbox.WebBoxDevice;
import net.solarnetwork.node.hw.sma.modbus.webbox.WebBoxService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * A configurable {@link WebBoxService}.
 *
 * @author matt
 * @version 2.1
 */
public class WebBoxServiceDevice extends WebBoxService implements SettingSpecifierProvider {

	/**
	 * Constructor.
	 */
	public WebBoxServiceDevice() {
		super();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.sma.webbox";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(4);

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));

		try {
			Collection<WebBoxDevice> devices = availableDevices();
			if ( devices != null ) {
				for ( WebBoxDevice d : devices ) {
					results.add(
							new BasicTitleSettingSpecifier("device", d.getDeviceDescription(), true));
				}
			}
		} catch ( RuntimeException e ) {
			log.warn("Error getting WebBox device list: {}", e.toString());
		}

		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.node.io.modbus.ModbusNetwork)"));

		return results;
	}

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

}
