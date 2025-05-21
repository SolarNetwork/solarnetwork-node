/* ==================================================================
 * NiftyTcpModbusNetwork.java - 19/12/2022 11:06:50 am
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

package net.solarnetwork.node.io.modbus.nifty.tcp;

import java.util.ArrayList;
import java.util.List;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.tcp.TcpModbusClientConfig;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusClientConfig;
import net.solarnetwork.io.modbus.tcp.netty.TcpNettyModbusClient;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.nifty.AbstractNiftyModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Nifty Modbus implementation of {@link ModbusNetwork} using a TCP connection.
 *
 * @author matt
 * @version 1.1
 */
public class NiftyTcpModbusNetwork extends AbstractNiftyModbusNetwork<NettyTcpModbusClientConfig> {

	/**
	 * Default constructor.
	 */
	public NiftyTcpModbusNetwork() {
		super(new NettyTcpModbusClientConfig());
		config.setAutoReconnect(false);
		setDisplayName("Modbus TCP");
	}

	@Override
	protected boolean isConfigured() {
		String host = getHost();
		return (host != null && !host.isEmpty());
	}

	@Override
	protected synchronized ModbusClient createController() {
		EventLoopGroup g = getOrCreateEventLoopGroup(() -> {
			return new MultiThreadIoEventLoopGroup(getEventLoopGroupMaxThreadCount(),
					NiftyTcpModbusNetwork.this, NioIoHandler.newFactory());
		});
		TcpNettyModbusClient controller = new TcpNettyModbusClient(config, g, NioSocketChannel.class);
		controller.setWireLogging(isWireLogging());
		controller.setReplyTimeout(getReplyTimeout());
		return controller;
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
		results.add(new BasicTextFieldSettingSpecifier("port",
				String.valueOf(TcpModbusClientConfig.DEFAULT_PORT)));

		results.addAll(baseNiftyModbusNetworkSettings(DEFAULT_KEEP_OPEN_SECONDS));

		return results;
	}

	/**
	 * Get the host to connect to.
	 *
	 * @return the host
	 */
	public String getHost() {
		return config.getHost();
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
		config.setHost(host);
	}

	/**
	 * Get the network port to connect to.
	 *
	 * @return the network port; defaults to {@literal 502}
	 */
	public int getPort() {
		return config.getPort();
	}

	/**
	 * Set the network port to connect to.
	 *
	 * @param port
	 *        the network port
	 */
	public void setPort(int port) {
		config.setPort(port);
	}

}
