/* ==================================================================
 * SocketcandCanbusNetwork.java - 19/09/2019 4:13:04 pm
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

package net.solarnetwork.node.io.canbus.socketcand;

import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.support.AbstractCanbusNetwork;
import net.solarnetwork.node.io.canbus.support.SocketCanbusSocketProvider;

/**
 * CAN bus network implementation using the socketcand server protocol.
 * 
 * @author matt
 * @version 1.0
 * @see <a href=
 *      "https://github.com/linux-can/socketcand">linux-can/socketcand</a>
 */
public class SocketcandCanbusNetwork extends AbstractCanbusNetwork {

	/** The default host value. */
	public static final String DEFAULT_HOST = "localhost";

	/** The default port value. */
	public static final int DEFAULT_PORT = 29536;

	private final CanbusSocketProvider socketProvider;
	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A {@link SocketCanbusSocketProvider} socket provider will be used.
	 * </p>
	 */
	public SocketcandCanbusNetwork() {
		this(new SocketCanbusSocketProvider());
	}

	/**
	 * Constructor.
	 * 
	 * @param socketProvider
	 *        the socket provider to use
	 * @throws IllegalArgumentException
	 *         if {@code socketProvider} is {@literal null}
	 */
	public SocketcandCanbusNetwork(CanbusSocketProvider socketProvider) {
		super();
		if ( socketProvider == null ) {
			throw new IllegalArgumentException("The socket provider must be provided.");
		}
		this.socketProvider = socketProvider;
	}

	@Override
	public String getDisplayName() {
		return "TCP CAN bus";
	}

	@Override
	protected String getNetworkDescription() {
		return host + ":" + port;
	}

	@Override
	protected CanbusConnection createConnectionInternal(String busName) {
		SocketcandCanbusConnection conn = new SocketcandCanbusConnection(socketProvider, getHost(),
				getPort(), busName);
		return conn;
	}

	/**
	 * Get the host to connect to.
	 * 
	 * @return the host; defaults to {@link #DEFAULT_HOST}
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the host to connect to.
	 * 
	 * @param host
	 *        the host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Get the port to connect to.
	 * 
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port to connect to.
	 * 
	 * @param port
	 *        the port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get the socket provider.
	 * 
	 * @return the socket provider
	 */
	public CanbusSocketProvider getSocketProvider() {
		return socketProvider;
	}

}
