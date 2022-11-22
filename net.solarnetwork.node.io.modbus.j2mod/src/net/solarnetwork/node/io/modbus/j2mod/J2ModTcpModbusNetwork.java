/* ==================================================================
 * J2ModTcpModbusNetwork.java - 8/07/2022 11:30:40 am
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

package net.solarnetwork.node.io.modbus.j2mod;

import static java.lang.String.format;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.io.AbstractModbusTransport;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * j2mod implementation of {@link ModbusNetwork} using a TCP connection.
 * 
 * @author matt
 * @version 1.0
 */
public class J2ModTcpModbusNetwork extends AbstractModbusNetwork implements SettingSpecifierProvider {

	/** The {@code keepOpenSeconds} property default value. */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 90;

	/** The {@code socketReuseAddress} property default value. */
	public static final boolean DEFAULT_SOCKET_REUSE_ADDRESS = true;

	/** The {@code socketLinger} property default value. */
	public static final int DEFAULT_SOCKET_LINGER = 1;

	/** The {@code socketKeepAlive} property default value. */
	public static final boolean DEFAULT_SOCKET_KEEP_ALIVE = true;

	private String host;
	private int port = Modbus.DEFAULT_PORT;
	private boolean socketReuseAddress = DEFAULT_SOCKET_REUSE_ADDRESS;
	private int socketLinger = DEFAULT_SOCKET_LINGER;
	private boolean socketKeepAlive = DEFAULT_SOCKET_KEEP_ALIVE;
	private int keepOpenSeconds = DEFAULT_KEEP_OPEN_SECONDS;

	private final AtomicReference<CachedTcpConnection> cachedConnection = new AtomicReference<>();

	/**
	 * Constructor.
	 */
	public J2ModTcpModbusNetwork() {
		super();
		setDisplayName("Modbus TCP port");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus.tcp";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("host", null));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(Modbus.DEFAULT_PORT)));
		results.addAll(getBaseSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("keepOpenSeconds",
				String.valueOf(DEFAULT_KEEP_OPEN_SECONDS)));
		/*-
		results.add(new BasicToggleSettingSpecifier("socketReuseAddress", DEFAULT_SOCKET_REUSE_ADDRESS));
		results.add(new BasicTextFieldSettingSpecifier("socketLinger",
				String.valueOf(DEFAULT_SOCKET_LINGER)));
		results.add(new BasicToggleSettingSpecifier("socketKeepAlive", DEFAULT_SOCKET_KEEP_ALIVE));
		*/
		return results;
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		try {
			TCPMasterConnection conn;
			if ( keepOpenSeconds > 0 ) {
				synchronized ( cachedConnection ) {
					CachedTcpConnection c = cachedConnection.get();
					if ( c == null ) {
						c = new CachedTcpConnection(InetAddress.getByName(host));
						cachedConnection.set(c);
					}
					conn = c;
				}
			} else {
				conn = new TCPMasterConnection(InetAddress.getByName(host));
			}
			conn.setPort(port);
			conn.setTimeout((int) getTimeoutUnit().toMillis(getTimeout()));

			J2ModTcpModbusConnection mbconn = new J2ModTcpModbusConnection(conn, unitId, isHeadless());
			mbconn.setRetries(getRetries());
			mbconn.setRetryReconnect(isRetryReconnect());
			return createLockingConnection(mbconn);
		} catch ( UnknownHostException e ) {
			throw new RuntimeException("Unknown modbus host [" + host + "]");
		}
	}

	@Override
	protected String getNetworkDescription() {
		return host + ":" + port;
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

		private void activity() {
			log.trace("Extending Modbus TCP connection {} expiry to {} seconds from now",
					getNetworkDescription(), keepOpenSeconds);
			keepOpenExpiry.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(keepOpenSeconds));
		}

		private class TrackingModbusTransport extends AbstractModbusTransport {

			private final AbstractModbusTransport delegate;

			private TrackingModbusTransport(AbstractModbusTransport delegate) {
				super();
				this.delegate = delegate;
			}

			@Override
			public void close() throws IOException {
				delegate.close();
			}

			@Override
			public ModbusTransaction createTransaction() {
				return delegate.createTransaction();
			}

			@Override
			public void writeRequest(ModbusRequest msg) throws ModbusIOException {
				try {
					delegate.writeRequest(msg);
					activity();
				} catch ( ModbusIOException e ) {
					if ( isRetryReconnect() && getRetries() > 0 ) {
						doClose();
					}
					throw e;
				}
			}

			@Override
			public void writeResponse(ModbusResponse msg) throws ModbusIOException {
				try {
					delegate.writeResponse(msg);
					activity();
				} catch ( ModbusIOException e ) {
					if ( isRetryReconnect() && getRetries() > 0 ) {
						doClose();
					}
					throw e;
				}
			}

			@Override
			public ModbusRequest readRequest(AbstractModbusListener listener) throws ModbusIOException {
				try {
					ModbusRequest req = delegate.readRequest(listener);
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
			if ( keepOpenExpiry.get() < System.currentTimeMillis() ) {
				doClose();
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
		public AbstractModbusTransport getModbusTransport() {
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
	 */
	public void setKeepOpenSeconds(int keepOpenSeconds) {
		this.keepOpenSeconds = keepOpenSeconds;
	}

}
