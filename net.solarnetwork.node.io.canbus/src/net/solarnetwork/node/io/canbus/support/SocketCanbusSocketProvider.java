/* ==================================================================
 * SocketCanbusSocketProvider.java - 23/09/2019 6:16:54 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.support;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocket;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocketProvider;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link CanbusSocketProvider} for {@link SocketCanbusSocket} instances.
 *
 * @author matt
 * @version 2.0
 */
public class SocketCanbusSocketProvider implements CanbusSocketProvider, SettingSpecifierProvider {

	private int socketTimeout = SocketCanbusSocket.DEFAULT_SOCKET_TIMEOUT;
	private boolean socketTcpNoDelay = SocketCanbusSocket.DEFAULT_SOCKET_TCP_NO_DELAY;
	private boolean socketReuseAddress = SocketCanbusSocket.DEFAULT_SOCKET_REUSE_ADDRESS;
	private int socketLinger = SocketCanbusSocket.DEFAULT_SOCKET_LINGER;
	private boolean socketKeepAlive = SocketCanbusSocket.DEFAULT_SOCKET_KEEP_ALIVE;

	/**
	 * Constructor.
	 */
	public SocketCanbusSocketProvider() {
		super();
	}

	@Override
	public CanbusSocket createCanbusSocket() {
		SocketCanbusSocket socket = new SocketCanbusSocket();
		socket.setSocketTimeout(getSocketTimeout());
		socket.setSocketTcpNoDelay(isSocketTcpNoDelay());
		socket.setSocketReuseAddress(isSocketReuseAddress());
		socket.setSocketLinger(getSocketLinger());
		socket.setSocketKeepAlive(isSocketKeepAlive());
		return socket;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.canbus.support.SocketCanbusSocketProvider";
	}

	@Override
	public String getDisplayName() {
		return "Java Socket CanbusSocket";
	}

	@Override
	public MessageSource getMessageSource() {
		return null;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(5);

		results.add(new BasicTextFieldSettingSpecifier("socketTimeout",
				String.valueOf(SocketCanbusSocket.DEFAULT_SOCKET_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("socketLinger",
				String.valueOf(SocketCanbusSocket.DEFAULT_SOCKET_LINGER)));
		results.add(new BasicToggleSettingSpecifier("socketTcpNoDelay",
				SocketCanbusSocket.DEFAULT_SOCKET_TCP_NO_DELAY));
		results.add(new BasicToggleSettingSpecifier("socketReuseAddress",
				SocketCanbusSocket.DEFAULT_SOCKET_REUSE_ADDRESS));
		results.add(new BasicToggleSettingSpecifier("socketKeepAlive",
				SocketCanbusSocket.DEFAULT_SOCKET_KEEP_ALIVE));

		return results;
	}

	// Accessors

	/**
	 * Get the timeout for blocking socket operations like reading from the
	 * socket.
	 *
	 * @return the socket timeout, in milliseconds; defaults to
	 *         {@link SocketCanbusSocket#DEFAULT_SOCKET_TIMEOUT}
	 */
	public int getSocketTimeout() {
		return socketTimeout;
	}

	/**
	 * Set the timeout for blocking socket operations like reading from the
	 * socket.
	 *
	 * @param socketTimeout
	 *        the socket timeout to use, in milliseconds
	 */
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	/**
	 * Get the TCP "no delay" flag.
	 *
	 * @return {@literal true} if the TCP "no delay" option should be used;
	 *         defaults to
	 *         {@link SocketCanbusSocket#DEFAULT_SOCKET_TCP_NO_DELAY}
	 */
	public boolean isSocketTcpNoDelay() {
		return socketTcpNoDelay;
	}

	/**
	 * Set the TCP "no delay" flag.
	 *
	 * @param socketTcpNoDelay
	 *        {@literal true} if the TCP "no delay" option should be used
	 */
	public void setSocketTcpNoDelay(boolean socketTcpNoDelay) {
		this.socketTcpNoDelay = socketTcpNoDelay;
	}

	/**
	 * Get the socket "reuse address" flag.
	 *
	 * @return {@literal true} if the socket "reuse address" flag should be
	 *         used; defaults to
	 *         {@link SocketCanbusSocket#DEFAULT_SOCKET_REUSE_ADDRESS}
	 */
	public boolean isSocketReuseAddress() {
		return socketReuseAddress;
	}

	/**
	 * Set the socket "reuse address" flag.
	 *
	 * @param socketReuseAddress
	 *        {@literal true} if the socket "reuse address" flag should be used
	 */
	public void setSocketReuseAddress(boolean socketReuseAddress) {
		this.socketReuseAddress = socketReuseAddress;
	}

	/**
	 * Get the socket linger amount.
	 *
	 * @return the socket linger amount, in seconds, or {@literal 0} to disable;
	 *         defaults to {@link SocketCanbusSocket#DEFAULT_SOCKET_LINGER}
	 */
	public int getSocketLinger() {
		return socketLinger;
	}

	/**
	 * Set the socket linger amount.
	 *
	 * @param socketLinger
	 *        the socket linger amount, in seconds, or {@literal 0} to disable
	 */
	public void setSocketLinger(int socketLinger) {
		this.socketLinger = socketLinger;
	}

	/**
	 * Get the socket "keep alive" flag.
	 *
	 * @return {@literal true} if the socket "keep alive" flag should be used;
	 *         defaults to {@link SocketCanbusSocket#DEFAULT_SOCKET_KEEP_ALIVE}
	 */
	public boolean isSocketKeepAlive() {
		return socketKeepAlive;
	}

	/**
	 * Set the socket "keep alive" flag.
	 *
	 * @param socketKeepAlive
	 *        {@literal true} if the socket "keep alive" flag should be used
	 */
	public void setSocketKeepAlive(boolean socketKeepAlive) {
		this.socketKeepAlive = socketKeepAlive;
	}

}
