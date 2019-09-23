/* ==================================================================
 * SocketcandCanbusConnection.java - 19/09/2019 4:13:27 pm
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.CanbusNetwork;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.FilterMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;
import net.solarnetwork.node.io.canbus.support.CanbusSubscription;

/**
 * Implementation of {@link CanbusNetwork} for socketcand CAN bus servers.
 * 
 * @author matt
 * @version 1.0
 * @see <a href=
 *      "https://github.com/linux-can/socketcand">linux-can/socketcand</a>
 */
public class SocketcandCanbusConnection implements CanbusConnection {

	/** The default value for the {@code socketTimeout} property. */
	public static final int DEFAULT_SOCKET_TIMEOUT = 400;

	/** The default value for the {@code socketTcpNoDelay} property. */
	public static final boolean DEFAULT_SOCKET_TCP_NO_DELAY = true;

	/** The default value for the {@code socketReuseAddress} property. */
	public static final boolean DEFAULT_SOCKET_REUSE_ADDRESS = true;

	/** The default value for the {@code socketLinger} property. */
	public static final int DEFAULT_SOCKET_LINGER = 1;

	/** The default value for the {@code socketKeepAlive} property. */
	public static final boolean DEFAULT_SOCKET_KEEP_ALIVE = false;

	private static final Logger log = LoggerFactory.getLogger(SocketcandCanbusConnection.class);

	private final ConcurrentMap<Integer, CanbusSubscription> subscriptions = new ConcurrentHashMap<>(16,
			0.9f, 1);

	private final char[] buffer = new char[4096];
	private final String host;
	private final int port;
	private final String busName;

	private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
	private boolean socketTcpNoDelay = DEFAULT_SOCKET_TCP_NO_DELAY;
	private boolean socketReuseAddress = DEFAULT_SOCKET_REUSE_ADDRESS;
	private int socketLinger = DEFAULT_SOCKET_LINGER;
	private boolean socketKeepAlive = DEFAULT_SOCKET_KEEP_ALIVE;

	private Socket socket;
	private Reader input;
	private Writer output;
	private boolean established;

	/**
	 * Constructor.
	 * 
	 * @param host
	 *        the host
	 * @param port
	 *        the port
	 * @param busName
	 *        the CAN bus to connect to
	 */
	public SocketcandCanbusConnection(String host, int port, String busName) {
		super();
		this.host = host;
		this.port = port;
		this.busName = busName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SocketcandCanbusConnection{");
		builder.append(busName);
		builder.append("@");
		builder.append(host);
		builder.append(":");
		builder.append(port);
		builder.append("}");
		return builder.toString();
	}

	private Message readNextMessage() throws IOException {
		synchronized ( buffer ) {
			return SocketcandUtils.readMessage(input, buffer);
		}
	}

	private void writeMessage(Message message) throws IOException {
		synchronized ( output ) {
			message.write(output);
			output.flush();
		}
	}

	@Override
	public void open() throws IOException {
		socket = new Socket(host, port);
		socket.setTcpNoDelay(socketTcpNoDelay);
		socket.setSoLinger(socketLinger > 0, socketLinger);
		socket.setKeepAlive(socketKeepAlive);
		socket.setReuseAddress(socketReuseAddress);

		// start socket timeout with a more generous value when initiating the connection
		socket.setSoTimeout(socketTimeout * 10);

		input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ASCII"), 2048);
		output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "ASCII"), 128);

		// the server immediately sends the Hi message when the socket connects
		Message m = readNextMessage();

		if ( m == null || m.getType() != MessageType.Hi ) {
			log.error("Did not receive expected greeting from [{}:{}]: {}", host, port, m);
			throw new IOException("Did not receive expected greeting.");
		}

		// open the desired bus name now
		writeMessage(new BasicMessage(MessageType.Open, null, Collections.singletonList(busName)));

		// expect an Ok response
		m = readNextMessage();
		if ( m == null || m.getType() != MessageType.Ok ) {
			log.error("Error opening bus [{}]: {}", busName, m);
			throw new IOException("Error opening bus [" + busName + "]: " + m);
		}

		// change socket timeout to normal value now
		socket.setSoTimeout(socketTimeout);

		// TODO: start reader thread
		established = true;
	}

	@Override
	public void close() throws IOException {
		// TODO: stop reader thread
		try {
			socket.close();
			// ignore this one
		} finally {
			socket = null;
			established = false;
		}

	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long dataFilter,
			CanbusFrameListener listener) throws IOException {
		CanbusSubscription sub = new CanbusSubscription(address, limit, dataFilter, listener);
		Message m = null;
		if ( sub.hasFilter() ) {
			m = new FilterMessageImpl(address, forceExtendedAddress, sub.getLimitSeconds(),
					sub.getLimitMicroseconds(), dataFilter);
		} else {
			m = new SubscribeMessageImpl(address, forceExtendedAddress, sub.getLimitSeconds(),
					sub.getLimitMicroseconds());
		}
		writeMessage(m);
		CanbusSubscription old = subscriptions.put(address, sub);
		if ( old != null ) {
			log.warn("Subscription to CAN bus [{}] {} replaced by new subscription", busName, old);
		}
		log.info("Subscribed to CAN bus [{}]: {}", busName, sub);

	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long identifierMask,
			Iterable<Long> dataFilters, CanbusFrameListener listener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unsubscribe(int address) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void monitor(CanbusFrameListener listener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unmonitor() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isEstablished() {
		return established;
	}

	@Override
	public String getBusName() {
		return busName;
	}

	/**
	 * Get the timeout for blocking socket operations like reading from the
	 * socket.
	 * 
	 * @return the socket timeout, in milliseconds; defaults to
	 *         {@link #DEFAULT_SOCKET_TIMEOUT}
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
	 *         defaults to {@link #DEFAULT_SOCKET_TCP_NO_DELAY}
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
	 *         used; defaults to {@link #DEFAULT_SOCKET_REUSE_ADDRESS}
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
	 *         defaults to {@link #DEFAULT_SOCKET_LINGER}
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
	 *         defaults to {@link #DEFAULT_SOCKET_KEEP_ALIVE}
	 */
	public boolean isSocketKeepAlive() {
		return socketKeepAlive;
	}

	/**
	 * Set the socket "keep alive" flag.
	 * 
	 * @return {@literal true} if the socket "keep alive" flag should be used
	 */
	public void setSocketKeepAlive(boolean socketKeepAlive) {
		this.socketKeepAlive = socketKeepAlive;
	}

}
