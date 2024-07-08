/* ==================================================================
 * AbstractCanbusNetwork.java - 19/09/2019 4:19:37 pm
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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.CanbusNetwork;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Base implementation of {@link CanbusNetwork} for other implementations to
 * extend.
 *
 * <p>
 * This implementation is designed to work with connections that are kept open
 * for long periods of time. Each connection returned from
 * {@link #createConnection(String)} will be tracked internally, and if the
 * {@link #configurationChanged(Map)} method is called any open connections will
 * be automatically closed, so that clients using the connection can re-open the
 * connection with the new configuration.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public abstract class AbstractCanbusNetwork extends BasicIdentifiable
		implements CanbusNetwork, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The default value for the {@code timeout} property. */
	public static final long DEFAULT_TIMEOUT = 10L;

	private final AtomicInteger connectionCounter = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, CanbusConnection> connections = new ConcurrentHashMap<>(8,
			0.9f, 1);

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public AbstractCanbusNetwork() {
		super();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		// close all existing connections so they can be re-opened with new settings
		closeAllConnections();
	}

	@Override
	public void serviceDidStartup() {
		// extending classes can override
	}

	@Override
	public synchronized void serviceDidShutdown() {
		closeAllConnections();
	}

	private void closeAllConnections() {
		for ( CanbusConnection conn : connections.values() ) {
			if ( !conn.isClosed() ) {
				try {
					log.info("Closing CAN bus connection {} after network configuration change.", conn);
					conn.close();
				} catch ( Exception e ) {
					// ignore
				}
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + getNetworkDescription() + '}';
	}

	/**
	 * Get a description of this network.
	 *
	 * <p>
	 * This implementation simply calls {@code toString()} on this object.
	 * Extending classes may want to provide something more meaningful.
	 * </p>
	 *
	 * @return a description of this network
	 */
	protected String getNetworkDescription() {
		return this.toString();
	}

	@Override
	public final synchronized CanbusConnection createConnection(String busName) {
		final Integer id = connectionCounter.incrementAndGet();
		final CanbusConnection conn = createConnectionInternal(busName);
		if ( conn == null ) {
			return null;
		}
		TrackedCanbusConnection tconn = new TrackedCanbusConnection(id, conn);
		connections.put(id, tconn);
		return tconn;
	}

	/**
	 * Create a new CAN bus connection.
	 *
	 * @param busName
	 *        the bus name
	 * @return the new connection, or {@literal null} if a connection cannot be
	 *         created, for example from missing configuration
	 */
	protected abstract CanbusConnection createConnectionInternal(String busName);

	private class TrackedCanbusConnection implements CanbusConnection {

		private final Integer id;
		private final CanbusConnection delegate;

		private TrackedCanbusConnection(Integer id, CanbusConnection delegate) {
			super();
			this.id = id;
			this.delegate = delegate;
		}

		@Override
		public String toString() {
			return delegate.toString();
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !(obj instanceof TrackedCanbusConnection) ) {
				return false;
			}
			TrackedCanbusConnection other = (TrackedCanbusConnection) obj;
			return Objects.equals(id, other.id);
		}

		@Override
		public void close() throws IOException {
			try {
				delegate.close();
			} finally {
				connections.remove(id, this);
			}
		}

		@Override
		public String getBusName() {
			return delegate.getBusName();
		}

		@Override
		public void open() throws IOException {
			delegate.open();
		}

		@Override
		public boolean isEstablished() {
			return delegate.isEstablished();
		}

		@Override
		public boolean isClosed() {
			return delegate.isClosed();
		}

		@Override
		public Future<Boolean> verifyConnectivity() {
			return delegate.verifyConnectivity();
		}

		@Override
		public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long dataFilter,
				CanbusFrameListener listener) throws IOException {
			delegate.subscribe(address, forceExtendedAddress, limit, dataFilter, listener);
		}

		@Override
		public void subscribe(int address, boolean forceExtendedAddress, Duration limit,
				long identifierMask, Iterable<Long> dataFilters, CanbusFrameListener listener)
				throws IOException {
			delegate.subscribe(address, forceExtendedAddress, limit, identifierMask, dataFilters,
					listener);
		}

		@Override
		public void unsubscribe(int address, boolean forceExtendedAddress) throws IOException {
			delegate.unsubscribe(address, forceExtendedAddress);
		}

		@Override
		public void monitor(CanbusFrameListener listener) throws IOException {
			delegate.monitor(listener);
		}

		@Override
		public void unmonitor() throws IOException {
			delegate.unmonitor();
		}

		@Override
		public boolean isMonitoring() {
			return delegate.isMonitoring();
		}

	}

	/**
	 * Get a list of base network settings.
	 *
	 * @param prefix
	 *        the setting key prefix to use
	 * @return the base network settings
	 */
	public static List<SettingSpecifier> getBaseNetworkSettings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "timeout", String.valueOf(DEFAULT_TIMEOUT)));
		return results;
	}

}
