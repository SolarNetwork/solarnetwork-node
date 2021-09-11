/* ==================================================================
 * JamodUdpModbusNetwork.java - 3/02/2018 7:58:36 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.jamod;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.wimpi.modbus.net.UDPMasterConnection;

/**
 * Jamod implementation of {@link ModbusNetwork} using a UDP connection.
 * 
 * <p>
 * Note that the "headless" mode is set to {@literal false} by default for this
 * implementation!.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class JamodUdpModbusNetwork extends AbstractModbusNetwork implements SettingSpecifierProvider {

	private String host;
	private int port = net.wimpi.modbus.Modbus.DEFAULT_PORT;

	/**
	 * Default constructor.
	 */
	public JamodUdpModbusNetwork() {
		super();
		setUid("Modbus UDP");
		setHeadless(false);
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		try {
			UDPMasterConnection conn = new UDPMasterConnection(InetAddress.getByName(host));
			conn.setPort(port);
			conn.setTimeout((int) getTimeoutUnit().toMillis(getTimeout()));
			JamodUdpModbusConnection mbconn = new JamodUdpModbusConnection(conn, unitId, isHeadless());
			mbconn.setRetries(getRetries());
			return createLockingConnection(mbconn);
		} catch ( UnknownHostException e ) {
			throw new RuntimeException("Unknown modbus host [" + host + "]");
		}
	}

	@Override
	protected String getNetworkDescription() {
		return host + ":" + port;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.modbus.udp";
	}

	@Override
	public String getDisplayName() {
		return "Modbus UDP port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		JamodUdpModbusNetwork defaults = new JamodUdpModbusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(5);
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.getUid())));
		results.add(new BasicTextFieldSettingSpecifier("host", defaults.host));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(defaults.port)));
		results.addAll(getBaseSettingSpecifiers());
		return results;
	}

	// Accessors

	/**
	 * Get the host to connect to.
	 * 
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the host to connect to.
	 * 
	 * <p>
	 * This can be a host name or IPv4 or IPv6 address.
	 * </p>
	 * 
	 * @param host
	 *        the host to connect to
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Get the network port to connect to.
	 * 
	 * @return the network port; defaults to {@literal 502}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the network port to connect to.
	 * 
	 * @param port
	 *        the network port
	 */
	public void setPort(int port) {
		this.port = port;
	}

}
