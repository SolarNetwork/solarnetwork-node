/* ==================================================================
 * Modbus4jTcpModbusNetwork.java - 22/11/2022 10:54:07 am
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

package net.solarnetwork.node.io.modbus.modbus4j.tcp;

import java.util.ArrayList;
import java.util.List;
import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.ip.tcp.TcpMaster;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.modbus4j.AbstractModbus4jModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Modbus4j implementation of {@link ModbusNetwork} using a TCP connection.
 * 
 * @author matt
 * @version 1.0
 */
public class Modbus4jTcpModbusNetwork extends AbstractModbus4jModbusNetwork<TcpMaster> {

	/** The {@code keepOpenSeconds} property default value. */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 90;

	/** The {@code socketKeepAlive} property default value. */
	public static final boolean DEFAULT_SOCKET_KEEP_ALIVE = true;

	private final IpParameters ipParameters = new IpParameters();
	private boolean socketKeepAlive = DEFAULT_SOCKET_KEEP_ALIVE;

	/**
	 * Default constructor.
	 */
	public Modbus4jTcpModbusNetwork() {
		super();
		setDisplayName("Modbus TCP");
	}

	@Override
	protected String getNetworkDescription() {
		return String.format("%s:%d", ipParameters.getHost(), ipParameters.getPort());
	}

	@Override
	protected boolean isConfigured() {
		String host = getHost();
		return (host != null && !host.isEmpty());
	}

	@Override
	protected TcpMaster createController() {
		return new TcpMaster(ipParameters, socketKeepAlive, true, false, ipParameters.getLingerTime());
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus.tcp";
	}

	@Override
	public String getDisplayName() {
		return "Modbus TCP port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("host", null));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(ModbusUtils.TCP_PORT)));

		results.addAll(baseModbus4jModbusNetworkSettings(DEFAULT_RETRIES, DEFAULT_KEEP_OPEN_SECONDS));

		results.add(new BasicTextFieldSettingSpecifier("socketLinger",
				ipParameters.getLingerTime() != null ? ipParameters.getLingerTime().toString() : null));
		results.add(new BasicToggleSettingSpecifier("socketKeepAlive", DEFAULT_SOCKET_KEEP_ALIVE));
		return results;
	}

	/**
	 * Get the host to connect to.
	 * 
	 * @return the host
	 */
	public String getHost() {
		return ipParameters.getHost();
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
		ipParameters.setHost(host);
	}

	/**
	 * Get the network port to connect to.
	 * 
	 * @return the network port; defaults to {@literal 502}
	 */
	public int getPort() {
		return ipParameters.getPort();
	}

	/**
	 * Set the network port to connect to.
	 * 
	 * @param port
	 *        the network port
	 */
	public void setPort(int port) {
		ipParameters.setPort(port);
	}

	/**
	 * Get the socket linger amount, in seconds.
	 * 
	 * @return the socket linger; defaults to {@literal 1}
	 */
	public Integer getSocketLinger() {
		return ipParameters.getLingerTime();
	}

	/**
	 * Set the socket linger time, in seconds.
	 * 
	 * @param lingerSeconds
	 *        the linger time, or {@literal null} or {@literal 0} to disable
	 */
	public void setSocketLinger(Integer lingerSeconds) {
		ipParameters.setLingerTime(lingerSeconds);
	}

	/**
	 * Get the socket keep-alive flag.
	 * 
	 * @return the keep-alive flag; defaults to {@literal true}
	 */
	public boolean isSocketKeepAlive() {
		return socketKeepAlive;
	}

	/**
	 * Set the socket keep-alive flag.
	 * 
	 * @param keepAlive
	 *        {@literal true} to enable keep alive mode
	 */
	public void setSocketKeepAlive(boolean keepAlive) {
		this.socketKeepAlive = keepAlive;
	}

}
