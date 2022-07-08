/* ==================================================================
 * J2ModUdpModbusNetwork.java - 8/07/2022 2:46:33 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.j2mod;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.net.UDPMasterConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * j2mod implementation of {@link ModbusNetwork} using a UDP connection.
 * 
 * @author matt
 * @version 1.0
 */
public class J2ModUdpModbusNetwork extends AbstractModbusNetwork implements SettingSpecifierProvider {

	private String host;
	private int port = Modbus.DEFAULT_PORT;

	/**
	 * Default constructor.
	 */
	public J2ModUdpModbusNetwork() {
		super();
		setDisplayName("Modbus UDP port");
		setHeadless(false);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus.udp";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(5);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("host", null));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(Modbus.DEFAULT_PORT)));
		results.addAll(getBaseSettingSpecifiers());
		return results;
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		try {
			UDPMasterConnection conn = new UDPMasterConnection(InetAddress.getByName(host));
			conn.setPort(port);
			conn.setTimeout((int) getTimeoutUnit().toMillis(getTimeout()));
			J2ModUdpModbusConnection mbconn = new J2ModUdpModbusConnection(conn, unitId, isHeadless());
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
