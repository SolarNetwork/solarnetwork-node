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
 * @version 2.1
 */
public class JMBusSerialMBusNetwork extends JMBusMBusNetwork implements SettingSpecifierProvider {

	/** The {@code uid} property default value. */
	public static final String DEFAULT_UID = "M-Bus Port";

	/** The {@code portName} property default value. */
	public static final String DEFAULT_PORT_NAME = "/dev/ttyS0";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private JMBusSerialParameters serialParams = getDefaultSerialParametersInstance();

	/**
	 * Constructor.
	 */
	public JMBusSerialMBusNetwork() {
		super();
		setUid(DEFAULT_UID);
	}

	private static JMBusSerialParameters getDefaultSerialParametersInstance() {
		final JMBusSerialParameters params = new JMBusSerialParameters();
		params.setPortName(DEFAULT_PORT_NAME);
		return params;
	}

	@Override
	protected org.openmuc.jmbus.MBusConnection createJMBusConnection() throws IOException {
		final MBusSerialBuilder builder = MBusConnection.newSerialBuilder(serialParams.getPortName());
		return builder.build();
	}

	@Override
	public String getSettingUid() {
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
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier("uid", DEFAULT_UID));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName", DEFAULT_PORT_NAME));
		results.addAll(super.getSettingSpecifiers());
		return results;
	}

	// Accessors

	/**
	 * Get the serial parameters.
	 * 
	 * @return the parameters
	 */
	public JMBusSerialParameters getSerialParams() {
		return serialParams;
	}

	/**
	 * Set the serial parameters.
	 * 
	 * @param serialParams
	 *        the parameters to set
	 */
	public void setSerialParams(JMBusSerialParameters serialParams) {
		this.serialParams = serialParams;
	}

}
