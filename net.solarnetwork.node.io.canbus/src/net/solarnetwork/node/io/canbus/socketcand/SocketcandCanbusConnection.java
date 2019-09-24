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

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.CanbusNetwork;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;
import net.solarnetwork.node.io.canbus.socketcand.msg.FilterMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.MuxFilterMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.SubscribeMessageImpl;
import net.solarnetwork.node.io.canbus.socketcand.msg.UnsubscribeMessageImpl;
import net.solarnetwork.node.io.canbus.support.CanbusSubscription;

/**
 * Implementation of {@link CanbusNetwork} for socketcand CAN bus servers.
 * 
 * @author matt
 * @version 1.0
 * @see <a href=
 *      "https://github.com/linux-can/socketcand">linux-can/socketcand</a>
 */
public class SocketcandCanbusConnection implements CanbusConnection, Runnable {

	public static final long DEFAULT_TIMEOUT_MS = 300000L;

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
	private static final Logger log = LoggerFactory.getLogger(SocketcandCanbusConnection.class);

	private final ConcurrentMap<Integer, CanbusSubscription> subscriptions = new ConcurrentHashMap<>(16,
			0.9f, 1);
	private final AtomicReference<CanbusFrameListener> monitorSubscription = new AtomicReference<>();

	private final CanbusSocketProvider socketProvider;
	private final String host;
	private final int port;
	private final String busName;
	private long messageTimeout = DEFAULT_TIMEOUT_MS;
	private TimeUnit messageTimeoutUnit = TimeUnit.MILLISECONDS;

	private Thread readerThread;
	private CanbusSocket socket;
	private boolean closed = false;

	/**
	 * Constructor.
	 * 
	 * @param socketProvider
	 *        the socket provider
	 * @param host
	 *        the host
	 * @param port
	 *        the port
	 * @param busName
	 *        the CAN bus to connect to
	 */
	public SocketcandCanbusConnection(CanbusSocketProvider socketProvider, String host, int port,
			String busName) {
		super();
		this.socketProvider = socketProvider;
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

	private CanbusSocket getSocket() {
		return socket;
	}

	private void setSocket(CanbusSocket socket) {
		this.socket = socket;
	}

	private Message readNextMessage(CanbusSocket s) throws IOException {
		if ( s != null ) {
			return s.nextMessage(messageTimeout, messageTimeoutUnit);
		}
		throw new IOException("Connection not open.");
	}

	private void writeMessage(CanbusSocket s, Message message) throws IOException {
		if ( s != null ) {
			s.writeMessage(message);
		} else {
			throw new IOException("Connection not open.");
		}
	}

	@Override
	public synchronized void open() throws IOException {
		CanbusSocket s = socketProvider.createCanbusSocket();
		s.open(host, port);
		setSocket(s);

		// the server immediately sends the Hi message when the socket connects
		Message m = readNextMessage(s);

		if ( m == null || m.getType() != MessageType.Hi ) {
			log.error("Did not receive expected greeting from [{}:{}]: {}", host, port, m);
			throw new IOException("Did not receive expected greeting.");
		}

		// open the desired bus name now
		writeMessage(s, new BasicMessage(MessageType.Open, null, Collections.singletonList(busName)));

		// expect an Ok response
		m = readNextMessage(s);
		if ( m == null || m.getType() != MessageType.Ok ) {
			log.error("Error opening bus [{}]: expected Ok response from Open message, but got {}",
					busName, m);
			throw new IOException("Error opening bus [" + busName + "]: " + m);
		}

		// create reader thread
		readerThread = new Thread(this);
		readerThread.setName("SocketcandCanbusConnection-" + THREAD_COUNTER.incrementAndGet());
		readerThread.setDaemon(true);
		readerThread.start();

		s.connectionConfirmed();
	}

	@Override
	public void run() {
		while ( true ) {
			if ( isClosed() || Thread.interrupted() ) {
				return;
			}
			try {
				CanbusFrameListener listener = monitorSubscription.get();
				final Message m = readNextMessage(getSocket());
				if ( m == null ) {
					return;
				}
				if ( m instanceof CanbusFrame ) {
					final CanbusFrame frame = (CanbusFrame) m;
					if ( listener != null ) {
						listener.canbusFrameReceived(frame);
					} else {
						final int addr = frame.getAddress();
						for ( CanbusSubscription sub : subscriptions.values() ) {
							if ( addr == sub.getAddress() ) {
								listener = sub.getListener();
								if ( listener != null ) {
									listener.canbusFrameReceived(frame);
								}
							}
						}
					}
				}
			} catch ( IOException e ) {
				log.debug("Communication error in CanbusSocket message reader thread: {}", e.toString());
				// ignore?
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if ( closed ) {
			return;
		}
		closed = true;
		CanbusSocket socket = getSocket();
		if ( socket != null ) {
			// TODO: stop reader thread
			try {
				socket.close();
				// ignore this one
			} finally {
				setSocket(null);
			}
		}
		if ( readerThread != null && readerThread.isAlive() ) {
			try {
				readerThread.interrupt();
				readerThread.join(1000);
			} catch ( Exception e ) {
				// ignore
			} finally {
				readerThread = null;
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
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
		subscribe(m, sub);
	}

	private void subscribe(Message m, CanbusSubscription sub) throws IOException {
		synchronized ( subscriptions ) {
			writeMessage(getSocket(), m);
			CanbusSubscription old = subscriptions.put(sub.getAddress(), sub);
			if ( old != null ) {
				log.warn("Subscription to CAN bus [{}] {} replaced by new subscription", busName, old);
			}
			log.info("Subscribed to CAN bus [{}]: {}", busName, sub);
		}
	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long identifierMask,
			Iterable<Long> dataFilters, CanbusFrameListener listener) throws IOException {
		CanbusSubscription sub = new CanbusSubscription(address, limit, identifierMask, listener);
		List<Long> filters;
		if ( dataFilters instanceof List<?> ) {
			filters = (List<Long>) dataFilters;
		} else {
			filters = StreamSupport.stream(dataFilters.spliterator(), false)
					.collect(Collectors.toList());
		}
		Message m = new MuxFilterMessageImpl(address, forceExtendedAddress, sub.getLimitSeconds(),
				sub.getLimitMicroseconds(), identifierMask, filters);
		subscribe(m, sub);
	}

	@Override
	public void unsubscribe(int address, boolean forceExtendedAddress) throws IOException {
		Message m = new UnsubscribeMessageImpl(address, forceExtendedAddress);
		synchronized ( subscriptions ) {
			writeMessage(getSocket(), m);
			subscriptions.remove(address);
			log.info("Unsubscribed to CAN bus [{}] {}", busName, address);
		}
	}

	@Override
	public void monitor(CanbusFrameListener listener) throws IOException {
		if ( listener == null ) {
			throw new IllegalArgumentException("The listener must not be null.");
		}
		synchronized ( monitorSubscription ) {
			writeMessage(getSocket(), new BasicMessage(MessageType.Rawmode));
			monitorSubscription.set(listener);
		}

	}

	@Override
	public void unmonitor() throws IOException {
		synchronized ( monitorSubscription ) {
			writeMessage(getSocket(), new BasicMessage(MessageType.Bcmmode));
			monitorSubscription.set(null);
		}
	}

	@Override
	public boolean isEstablished() {
		CanbusSocket s = getSocket();
		return (s != null ? s.isEstablished() : false);
	}

	@Override
	public String getBusName() {
		return busName;
	}

	/**
	 * Get a timeout value to use when waiting for CAN messages.
	 * 
	 * @return the message timeout, or zero to wait forever; defaults to
	 *         {@link #DEFAULT_TIMEOUT_MS}
	 * @see #getMessageTimeoutUnit()
	 */
	public long getMessageTimeout() {
		return messageTimeout;
	}

	/**
	 * Set the timeout value to use when waiting for CAN messages.
	 * 
	 * @param messageTimeout
	 *        the timeout to use
	 * @see #setMessageTimeoutUnit(TimeUnit)
	 */
	public void setMessageTimeout(long messageTimeout) {
		this.messageTimeout = messageTimeout;
	}

	/**
	 * Get the timeout units to use when waiting for CAN messages.
	 * 
	 * @return the time unit; defaults to {@link TimeUnit#MILLISECONDS}
	 * @see #getMessageTimeout()
	 */
	public TimeUnit getMessageTimeoutUnit() {
		return messageTimeoutUnit;
	}

	/**
	 * Set the timeout units to use when waiting for CAN messages.
	 *
	 * @param messageTimeoutUnit
	 *        the time unit to use
	 * @see #setMessageTimeout(long)
	 */
	public void setMessageTimeoutUnit(TimeUnit messageTimeoutUnit) {
		this.messageTimeoutUnit = messageTimeoutUnit;
	}

}
