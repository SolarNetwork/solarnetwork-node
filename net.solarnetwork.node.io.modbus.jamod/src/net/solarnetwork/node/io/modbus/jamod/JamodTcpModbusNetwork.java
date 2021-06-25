/* ==================================================================
 * JamodTcpModbusNetwork.java - 3/02/2018 7:58:36 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.jamod;

import static java.lang.String.format;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ClassUtils;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.io.ModbusTransport;
import net.wimpi.modbus.msg.ModbusMessage;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.net.TCPMasterConnection;

/**
 * Jamod implementation of {@link ModbusNetwork} using a TCP connection.
 * 
 * <p>
 * Note that {@code headless} is set to {@literal false} and
 * {@code retryReconnect} to {@literal true} by default for this implementation.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class JamodTcpModbusNetwork extends AbstractModbusNetwork implements SettingSpecifierProvider {

	/**
	 * The default {@code keepOpenSeconds} property value.
	 * 
	 * @since 1.1
	 */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 90;

	private String host;
	private int port = net.wimpi.modbus.Modbus.DEFAULT_PORT;
	private boolean socketReuseAddress = true;
	private int socketLinger = 1;
	private boolean socketKeepAlive = true;
	private int keepOpenSeconds = DEFAULT_KEEP_OPEN_SECONDS;

	private final AtomicReference<CachedTcpConnection> cachedConnection = new AtomicReference<>();

	/**
	 * Default constructor.
	 */
	public JamodTcpModbusNetwork() {
		super();
		setUid("Modbus TCP");
		setHeadless(false);
		setRetryReconnect(true);
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		try {
			TCPMasterConnection conn;
			if ( keepOpenSeconds > 0 ) {
				synchronized ( cachedConnection ) {
					CachedTcpConnection c = cachedConnection.get();
					if ( c == null || !c.delayExpiry() ) {
						c = new CachedTcpConnection(InetAddress.getByName(host));
						cachedConnection.set(c);
					}
					conn = c;
				}
			} else {
				conn = new LockingTcpConnection(InetAddress.getByName(host));
			}
			conn.setPort(port);
			conn.setTimeout((int) getTimeoutUnit().toMillis(getTimeout()));

			// SN extended feature support; don't assume we are using this
			Map<String, Object> extendedProperties = new HashMap<String, Object>(4);
			extendedProperties.put("socketKeepAlive", isSocketKeepAlive());
			extendedProperties.put("socketLinger", getSocketLinger());
			extendedProperties.put("socketReuseAddress", isSocketReuseAddress());
			ClassUtils.setBeanProperties(conn, extendedProperties, true);

			JamodTcpModbusConnection mbconn = new JamodTcpModbusConnection(conn, unitId, isHeadless());
			mbconn.setRetries(getRetries());
			mbconn.setRetryReconnect(isRetryReconnect());
			return mbconn;
		} catch ( UnknownHostException e ) {
			throw new RuntimeException("Unknown modbus host [" + host + "]");
		}
	}

	@Override
	protected String getNetworkDescription() {
		return host + ":" + port;
	}

	/**
	 * Internal extension of {@link TCPMasterConnection} that utilizes a
	 * {@link Lock} to serialize access to the connection between threads.
	 */
	private class LockingTcpConnection extends TCPMasterConnection {

		/**
		 * Constructor.
		 * 
		 * @param addr
		 *        the host address
		 */
		private LockingTcpConnection(InetAddress addr) {
			super(addr);
		}

		@Override
		public void connect() throws Exception {
			if ( !isConnected() ) {
				acquireLock();
				super.connect();
			}
		}

		@Override
		public void close() {
			try {
				if ( isConnected() ) {
					super.close();
				}
			} finally {
				releaseLock();
			}
		}

	}

	/**
	 * Connection that stays open after closing while utilizing a {@link Lock}
	 * to serialize access to the connection between threads.
	 */
	private class CachedTcpConnection extends TCPMasterConnection implements Runnable {

		private Thread keepOpenTimeoutThread;
		private TrackingModbusTransport transport;
		private final AtomicLong keepOpenExpiry;

		/**
		 * Constructor.
		 * 
		 * @param addr
		 *        the host address
		 */
		private CachedTcpConnection(InetAddress addr) {
			super(addr);
			keepOpenExpiry = new AtomicLong(
					System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(keepOpenSeconds));
		}

		private boolean delayExpiry() {
			synchronized ( keepOpenExpiry ) {
				if ( keepOpenTimeoutThread == null || keepOpenTimeoutThread.isAlive() ) {
					activity();
					return true;
				}
			}
			return false;
		}

		private void activity() {
			log.trace("Extending TCP connection expiry to {} seconds from now", keepOpenSeconds);
			keepOpenExpiry.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(keepOpenSeconds));
		}

		private class TrackingModbusTransport implements ModbusTransport {

			private final ModbusTransport delegate;

			private TrackingModbusTransport(ModbusTransport delegate) {
				super();
				this.delegate = delegate;
			}

			@Override
			public void close() throws IOException {
				delegate.close();
			}

			@Override
			public void writeMessage(ModbusMessage msg) throws ModbusIOException {
				try {
					delegate.writeMessage(msg);
					activity();
				} catch ( ModbusIOException e ) {
					if ( isRetryReconnect() && getRetries() > 0 ) {
						doClose();
					}
					throw e;
				}
			}

			@Override
			public ModbusRequest readRequest() throws ModbusIOException {
				try {
					ModbusRequest req = delegate.readRequest();
					activity();
					return req;
				} catch ( ModbusIOException e ) {
					if ( isRetryReconnect() && getRetries() > 0 ) {
						doClose();
					}
					throw e;
				}
			}

			@Override
			public ModbusResponse readResponse() throws ModbusIOException {
				try {
					ModbusResponse res = delegate.readResponse();
					activity();
					return res;
				} catch ( ModbusIOException e ) {
					if ( isRetryReconnect() && getRetries() > 0 ) {
						doClose();
					}
					throw e;
				}
			}

		}

		@Override
		public void connect() throws Exception {
			acquireLock();
			synchronized ( keepOpenExpiry ) {
				if ( !isConnected() ) {
					super.connect();
					if ( transport == null ) {
						transport = new TrackingModbusTransport(super.getModbusTransport());
					}
					if ( keepOpenTimeoutThread == null || !keepOpenTimeoutThread.isAlive() ) {
						keepOpenTimeoutThread = new Thread(this,
								format("Modbus TCP Expiry %s", getNetworkDescription()));
						keepOpenTimeoutThread.setDaemon(true);
						keepOpenTimeoutThread.start();
					}
					if ( log.isInfoEnabled() ) {
						log.info("Opened Modbus TCP connection {}; keep for {}s",
								getNetworkDescription(), keepOpenSeconds);
					}
				}
			}
		}

		@Override
		public void close() {
			try {
				if ( keepOpenExpiry.get() < System.currentTimeMillis() ) {
					doClose();
				}
			} finally {
				releaseLock();
			}
		}

		private void doClose() {
			synchronized ( keepOpenExpiry ) {
				try {
					super.close();
					if ( log.isInfoEnabled() && cachedConnection.get() == this ) {
						log.info("Closed Modbus TCP connection {}", getNetworkDescription());
					}
				} finally {
					cachedConnection.compareAndSet(this, null);
				}
			}
		}

		@Override
		public ModbusTransport getModbusTransport() {
			return transport;
		}

		@Override
		public void run() {
			try {
				while ( true ) {
					long now, expire;
					synchronized ( keepOpenExpiry ) {
						now = System.currentTimeMillis();
						expire = keepOpenExpiry.get();
						if ( expire < now ) {
							doClose();
							return;
						}
					}
					long sleep = expire - now;
					if ( log.isDebugEnabled() ) {
						log.debug("Connection {} expires in {}ms", getNetworkDescription(), sleep);
					}
					Thread.sleep(sleep);
				}
			} catch ( InterruptedException e ) {
				// end
			}
		}

	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.modbus.tcp";
	}

	@Override
	public String getDisplayName() {
		return "Modbus TCP port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		JamodTcpModbusNetwork defaults = new JamodTcpModbusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(5);
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.getUid())));
		results.add(new BasicTextFieldSettingSpecifier("host", defaults.host));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(defaults.port)));
		results.addAll(getBaseSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("keepOpenSeconds",
				String.valueOf(DEFAULT_KEEP_OPEN_SECONDS)));
		results.add(new BasicToggleSettingSpecifier("socketReuseAddress", defaults.socketReuseAddress));
		results.add(new BasicTextFieldSettingSpecifier("socketLinger",
				String.valueOf(defaults.socketLinger)));
		results.add(new BasicToggleSettingSpecifier("socketKeepAlive", defaults.socketKeepAlive));
		return results;
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

	/**
	 * Get the socket reuse address flag.
	 * 
	 * @return the socket reuse flag; defaults to {@literal true}
	 */
	public boolean isSocketReuseAddress() {
		return socketReuseAddress;
	}

	/**
	 * Control the socket reuse setting.
	 * 
	 * @param reuse
	 *        {@literal true} to enable socket reuse
	 */
	public void setSocketReuseAddress(boolean reuse) {
		this.socketReuseAddress = reuse;
	}

	/**
	 * Get the socket linger amount, in seconds.
	 * 
	 * @return the socket linger; defaults to {@literal 1}
	 */
	public int getSocketLinger() {
		return socketLinger;
	}

	/**
	 * Set the socket linger time, in seconds.
	 * 
	 * @param lingerSeconds
	 *        the linger time, or {@literal 0} to disable
	 */
	public void setSocketLinger(int lingerSeconds) {
		this.socketLinger = lingerSeconds;
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

	/**
	 * Get the number of seconds to keep the TCP connection open, for repeated
	 * transaction use.
	 * 
	 * @return the number of seconds; defaults to
	 *         {@link #DEFAULT_KEEP_OPEN_SECONDS}
	 * @since 1.1
	 */
	public int getKeepOpenSeconds() {
		return keepOpenSeconds;
	}

	/**
	 * Set the number of seconds to keep the TCP connection open, for repeated
	 * transaction use.
	 * 
	 * @param keepOpenSeconds
	 *        the number of seconds, or anything less than {@literal 1} to not
	 *        keep connections open
	 * @since 1.1
	 */
	public void setKeepOpenSeconds(int keepOpenSeconds) {
		this.keepOpenSeconds = keepOpenSeconds;
	}

}
