/* ==================================================================
 * JMBusSerialWMBusNetwork.java - 02/07/2020 12:52:10 pm
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

package net.solarnetwork.node.io.mbus.jmbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusManufacturer;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusSerialBuilder;
import org.openmuc.jmbus.wireless.WMBusMode;
import net.solarnetwork.node.io.mbus.WMBusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Serial jMBus implementation of {@link WMBusNetwork}.
 * 
 * @author alex
 * @version 1.0
 */
public class JMBusSerialWMBusNetwork extends JMBusWMBusNetwork implements SettingSpecifierProvider {

	private JMBusSerialParameters serialParams = getDefaultSerialParametersInstance();
	private JMBusWirelessParameters wirelessParams = getDefaultWirelessParametersInstance();

	public JMBusSerialWMBusNetwork() {
		super();
		setUid("Wireless M-Bus Network");
	}

	private static JMBusSerialParameters getDefaultSerialParametersInstance() {
		final JMBusSerialParameters params = new JMBusSerialParameters();
		params.setPortName("/dev/ttyS0");
		return params;
	}

	private static JMBusWirelessParameters getDefaultWirelessParametersInstance() {
		final JMBusWirelessParameters params = new JMBusWirelessParameters();
		params.setManufacturer(WMBusManufacturer.AMBER);
		params.setMode(WMBusMode.T);
		return params;
	}

	@Override
	protected org.openmuc.jmbus.wireless.WMBusConnection createJMBusConnection() throws IOException {
		final WMBusSerialBuilder builder = new WMBusSerialBuilder(wirelessParams.getManufacturer(), this,
				serialParams.getPortName());
		builder.setMode(wirelessParams.getMode());
		return builder.build();
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.mbus";
	}

	@Override
	public String getDisplayName() {
		return "M-Bus port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		JMBusSerialWMBusNetwork defaults = new JMBusSerialWMBusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
		results.add(new BasicTextFieldSettingSpecifier("wirelessParams.manufacturerString",
				defaults.wirelessParams.getManufacturerString()));
		results.add(new BasicTextFieldSettingSpecifier("wirelessParams.modeString",
				defaults.wirelessParams.getModeString()));
		return results;
	}

}
