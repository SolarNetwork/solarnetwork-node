/* ==================================================================
 * ModbusServer.java - 17/09/2020 4:54:54 PM
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

package net.solarnetwork.node.io.modbus.server.tcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.impl.BaseModbusServer;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Modbus TCP server service.
 *
 * @author matt
 * @version 3.6
 */
public class ModbusServer extends BaseModbusServer<NettyTcpModbusServer> {

	/** The default listen port. */
	public static final int DEFAULT_PORT = 502;

	/**
	 * The default {@code maxBacklog} property value.
	 *
	 * @deprecated no longer used
	 */
	@Deprecated
	public static final int DEFAULT_BACKLOG = 5;

	/** The default {@code bindAddress} property value. */
	public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

	/**
	 * The setting UID used by this service.
	 *
	 * @since 2.3
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.io.modbus.server";

	/** The {@code pendingMessageTtl} property default value. */
	public static final long DEFAULT_PENDING_MESSAGE_TTL = TimeUnit.MINUTES.toMillis(2);

	private int port = DEFAULT_PORT;
	private String bindAddress = DEFAULT_BIND_ADDRESS;
	private long pendingMessageTtl = DEFAULT_PENDING_MESSAGE_TTL;

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        the executor to handle client connections with
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServer(Executor executor) {
		this(executor, new ConcurrentHashMap<>(2, 0.9f, 2));
	}

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        the executor to handle client connections with
	 * @param registers
	 *        the register data map to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServer(Executor executor, ConcurrentMap<Integer, ModbusRegisterData> registers) {
		super(executor, registers);
	}

	@Override
	protected String description() {
		String uid = getUid();
		if ( uid != null && !uid.isEmpty() ) {
			return uid + " (port " + port + ")";
		}
		return "port " + port;
	}

	@Override
	protected NettyTcpModbusServer startServer() throws IOException {
		NettyTcpModbusServer server = new NettyTcpModbusServer(bindAddress, port);
		server.setMessageHandler(handler);
		server.setWireLogging(isWireLogging());
		server.setPendingMessageTtl(pendingMessageTtl);
		server.start();
		return server;
	}

	@Override
	protected void stopServer(NettyTcpModbusServer server) {
		server.stop();
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	protected List<SettingSpecifier> getExtendedSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(2);

		result.add(new BasicTextFieldSettingSpecifier("bindAddress", DEFAULT_BIND_ADDRESS));
		result.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));

		return result;
	}

	/**
	 * Get the address to bind to.
	 *
	 * @return the address; defaults to {@link #DEFAULT_BIND_ADDRESS}
	 */
	public String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the address to bind to.
	 *
	 * @param bindAddress
	 *        the address to set
	 */
	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Get the server socket backlog setting.
	 *
	 * @return the backlog; defaults to {@link #DEFAULT_BACKLOG}
	 * @deprecated only returns 0 now
	 */
	@Deprecated
	public int getBacklog() {
		return 0;
	}

	/**
	 * Set the server socket backlog setting.
	 *
	 * @param backlog
	 *        the backlog to set
	 * @deprecated does not do anything anymore
	 */
	@Deprecated
	public void setBacklog(int backlog) {
		// nothing
	}

	/**
	 * Get the server listen port.
	 *
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the server listen port.
	 *
	 * @param port
	 *        the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get the pending Modbus message time-to-live expiration time.
	 *
	 * @return the pendingMessageTtl the pending Modbus message time-to-live, in
	 *         milliseconds; defaults to {@link #DEFAULT_PENDING_MESSAGE_TTL}
	 * @since 4.0
	 */
	public long getPendingMessageTtl() {
		return pendingMessageTtl;
	}

	/**
	 * Set the pending Modbus message time-to-live expiration time.
	 *
	 * <p>
	 * This timeout represents the minimum amount of time the client will wait
	 * for a Modbus message response, before it qualifies for removal from the
	 * pending message queue.
	 * </p>
	 *
	 * @param pendingMessageTtl
	 *        the pending Modbus message time-to-live, in milliseconds
	 * @since 4.0
	 */
	public void setPendingMessageTtl(long pendingMessageTtl) {
		this.pendingMessageTtl = pendingMessageTtl;
	}

}
