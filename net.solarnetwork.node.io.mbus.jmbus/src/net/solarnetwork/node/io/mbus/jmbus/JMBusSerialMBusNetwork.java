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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.WMBusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Serial jMBus implementation of {@link WMBusNetwork}.
 * 
 * @author alex
 * @version 2.0
 */
public class JMBusSerialMBusNetwork extends JMBusMBusNetwork implements SettingSpecifierProvider {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private JMBusSerialParameters serialParams = getDefaultSerialParametersInstance();

	public JMBusSerialMBusNetwork() {
		super();
		setUid("M-Bus Port");
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
		return "net.solarnetwork.node.io.mbus.serial";
	}

	@Override
	public String getDisplayName() {
		return "M-Bus Serial Connection";
	}

	@Override
	protected String getNetworkDescription() {
		if ( serialParams != null && serialParams.getPortName() != null ) {
			return serialParams.getPortName();
		} else if ( getUid() != null ) {
			return getUid();
		}
		return super.toString();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("JMBusSerialMBusNetwork{");
		buf.append(getNetworkDescription());
		buf.append("}");
		return buf.toString();
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
