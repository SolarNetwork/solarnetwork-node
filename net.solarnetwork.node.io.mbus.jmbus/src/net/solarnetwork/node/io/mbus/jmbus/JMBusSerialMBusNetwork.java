/* ==================================================================
 * JMBusSerialMBusNetwork.java - 13/08/2020 10:52:48 am
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
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;
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
public class JMBusSerialMBusNetwork extends JMBusMBusNetwork implements SettingSpecifierProvider {

	private JMBusSerialParameters serialParams = getDefaultSerialParametersInstance();

	public JMBusSerialMBusNetwork() {
		super();
		setUid("M-Bus Network");
	}

	private static JMBusSerialParameters getDefaultSerialParametersInstance() {
		final JMBusSerialParameters params = new JMBusSerialParameters();
		params.setPortName("/dev/ttyS0");
		return params;
	}

	@Override
	protected org.openmuc.jmbus.MBusConnection createJMBusConnection() throws IOException {
		final MBusSerialBuilder builder = MBusConnection.newSerialBuilder(serialParams.getPortName());
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
		JMBusSerialMBusNetwork defaults = new JMBusSerialMBusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
		return results;
	}

	// Accessors

	public JMBusSerialParameters getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(JMBusSerialParameters serialParams) {
		this.serialParams = serialParams;
	}

}
