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
import java.util.List;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusManufacturer;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusSerialBuilder;
import org.openmuc.jmbus.wireless.WMBusMode;
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
public class JMBusSerialWMBusNetwork extends JMBusWMBusNetwork implements SettingSpecifierProvider {

	private JMBusSerialParameters serialParams = getDefaultSerialParametersInstance();
	private JMBusWirelessParameters wirelessParams = getDefaultWirelessParametersInstance();

	/**
	 * Constructor.
	 */
	public JMBusSerialWMBusNetwork() {
		super();
		setUid("M-Bus (Wireless) Port");
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
	public String getSettingUid() {
		return "net.solarnetwork.node.io.mbus.wireless.serial";
	}

	@Override
	public String getDisplayName() {
		return "M-Bus (Wireless) Serial Connection";
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
		StringBuilder buf = new StringBuilder("JMBusSerialWMBusNetwork{");
		buf.append(getNetworkDescription());
		buf.append("}");
		return buf.toString();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		JMBusSerialWMBusNetwork defaults = new JMBusSerialWMBusNetwork();
		List<SettingSpecifier> results = basicIdentifiableSettings();
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
		results.add(new BasicTextFieldSettingSpecifier("wirelessParams.manufacturerString",
				defaults.wirelessParams.getManufacturerString()));
		results.add(new BasicTextFieldSettingSpecifier("wirelessParams.modeString",
				defaults.wirelessParams.getModeString()));
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

	/**
	 * Get the wirelesss parameters.
	 * 
	 * @return the wireless parameters
	 */
	public JMBusWirelessParameters getWirelessParams() {
		return wirelessParams;
	}

	/**
	 * Set the wireless parameters.
	 * 
	 * @param wirelessParams
	 *        the wireless parameters to set
	 */
	public void setWirelessParams(JMBusWirelessParameters wirelessParams) {
		this.wirelessParams = wirelessParams;
	}

}
