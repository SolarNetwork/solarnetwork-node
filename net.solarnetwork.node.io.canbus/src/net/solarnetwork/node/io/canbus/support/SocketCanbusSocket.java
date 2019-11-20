/* ==================================================================
 * SocketCanbusSocket.java - 23/09/2019 5:34:35 pm
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocket;
import net.solarnetwork.node.io.canbus.socketcand.Message;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * Standard {@link Socket} based implementation of {@link CanbusSocket}.
 * 
 * @author matt
 * @version 1.0
 */
public class SocketCanbusSocket implements CanbusSocket {

	/** The default value for the {@code socketTimeout} property. */
	public static final int DEFAULT_SOCKET_TIMEOUT = 300000;

	/** The default value for the {@code socketTcpNoDelay} property. */
	public static final boolean DEFAULT_SOCKET_TCP_NO_DELAY = true;

	/** The default value for the {@code socketReuseAddress} property. */
	public static final boolean DEFAULT_SOCKET_REUSE_ADDRESS = true;

	/** The default value for the {@code socketLinger} property. */
	public static final int DEFAULT_SOCKET_LINGER = 1;

	/** The default value for the {@code socketKeepAlive} property. */
	public static final boolean DEFAULT_SOCKET_KEEP_ALIVE = false;

	private final char[] buffer = new char[4096];

	private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
	private boolean socketTcpNoDelay = DEFAULT_SOCKET_TCP_NO_DELAY;
	private boolean socketReuseAddress = DEFAULT_SOCKET_REUSE_ADDRESS;
	private int socketLinger = DEFAULT_SOCKET_LINGER;
	private boolean socketKeepAlive = DEFAULT_SOCKET_KEEP_ALIVE;

	private Socket socket;
	private Reader input;
	private Writer output;
	private boolean established;
	private boolean closed;

	/**
	 * Constructor.
	 */
	public SocketCanbusSocket() {
		super();
	}

	@Override
	public synchronized void open(String host, int port) throws IOException {
		socket = new Socket(host, port);
		socket.setTcpNoDelay(socketTcpNoDelay);
		socket.setSoLinger(socketLinger > 0, socketLinger);
		socket.setKeepAlive(socketKeepAlive);
		socket.setReuseAddress(socketReuseAddress);

		// start socket timeout with a more generous value when initiating the connection
		socket.setSoTimeout(socketTimeout * 10);

		input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ASCII"), 2048);
		output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "ASCII"), 128);
	}

	@Override
	public void connectionConfirmed() throws IOException {
		// change socket timeout to normal value now
		socket.setSoTimeout(socketTimeout);
		established = true;
	}

	@Override
	public Message nextMessage(long timeout, TimeUnit unit) throws IOException {
		Reader in;
		synchronized ( this ) {
			in = this.input;
		}
		if ( in != null ) {
			synchronized ( buffer ) {
				return SocketcandUtils.readMessage(in, buffer);
			}
		}
		throw new IOException("Connection not open.");
	}

	@Override
	public void writeMessage(Message message) throws IOException {
		Writer out;
		synchronized ( this ) {
			out = this.output;
		}
		if ( out != null ) {
			synchronized ( out ) {
				message.write(out);
				out.flush();
			}
		} else {
			throw new IOException("Connection not open.");
		}
	}

	@Override
	public boolean isEstablished() {
		return established;
	}

	@Override
	public synchronized void close() throws IOException {
		if ( socket == null ) {
			return;
		}
		try {
			socket.close();
			// ignore this one
		} finally {
			socket = null;
			established = false;
			closed = true;
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
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
