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
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
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
 * @version 1.0
 */
public class NiftyTcpModbusNetwork extends AbstractNiftyModbusNetwork implements ThreadFactory {

	/** The {@code keepOpenSeconds} property default value. */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 90;

	/** The {@code eventLoopGroupMaxThreadCount} property default value. */
	public static final int DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT = 4;

	private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

	private final NettyTcpModbusClientConfig config = new NettyTcpModbusClientConfig();

	private EventLoopGroup eventLoopGroup;
	private int eventLoopGroupMaxThreadCount;

	/**
	 * Default constructor.
	 */
	public NiftyTcpModbusNetwork() {
		super();
		setDisplayName("Modbus TCP");
		setKeepOpenSeconds(DEFAULT_KEEP_OPEN_SECONDS);
		config.setAutoReconnect(false);
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( eventLoopGroup != null ) {
			eventLoopGroup.shutdownGracefully();
			eventLoopGroup = null;
		}
		super.configurationChanged(properties);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		super.serviceDidShutdown();
		if ( eventLoopGroup != null ) {
			eventLoopGroup.shutdownGracefully();
			eventLoopGroup = null;
		}
	}

	@Override
	protected String getNetworkDescription() {
		return String.format("%s:%d", config.getHost(), config.getPort());
	}

	@Override
	protected boolean isConfigured() {
		String host = getHost();
		return (host != null && !host.isEmpty());
	}

	@Override
	protected synchronized ModbusClient createController() {
		if ( eventLoopGroup == null ) {
			eventLoopGroup = new NioEventLoopGroup(eventLoopGroupMaxThreadCount, this);
		}
		TcpNettyModbusClient controller = new TcpNettyModbusClient(config, eventLoopGroup,
				NioSocketChannel.class);
		controller.setWireLogging(isWireLogging());
		return controller;
	}

	// SettingSpecifierProvider

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r,
				"TcpModbusClient-" + getHost() + ":" + getPort() + "-" + THREAD_COUNT.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

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

	/**
	 * Get the event loop group maximum thread count.
	 * 
	 * @return the maximum thread count; defaults to
	 *         {@link #DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT}
	 */
	public int getEventLoopGroupMaxThreadCount() {
		return eventLoopGroupMaxThreadCount;
	}

	/**
	 * Set the event loop group maximum thread count.
	 * 
	 * @param eventLoopGroupMaxThreadCount
	 *        the maximum thread count to set
	 */
	public void setEventLoopGroupMaxThreadCount(int eventLoopGroupMaxThreadCount) {
		this.eventLoopGroupMaxThreadCount = eventLoopGroupMaxThreadCount;
	}

}
