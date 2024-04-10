/* ==================================================================
 * JMBusMBusNetwork.java - 13/08/2020 10:36:38 am
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

package net.solarnetwork.node.io.mbus.jmbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.openmuc.jmbus.VariableDataStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.MBusConnection;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusNetwork;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Abstract jMBus implementation of {@link MBusNetwork}.
 *
 * @author alex
 * @version 2.2
 */
public abstract class JMBusMBusNetwork extends BasicIdentifiable implements MBusNetwork {

	/**
	 * A default value for the {@code timeout} property.
	 *
	 * @since 2.1
	 */
	public static final long DEFAULT_TIMEOUT_SECS = 10L;

	/**
	 * The {@code transportTimeout} property default value.
	 *
	 * @since 2.2
	 */
	public static final int DEFAULT_TRANSPORT_TIMEOUT_MS = 500;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation

	private long timeout = DEFAULT_TIMEOUT_SECS;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;
	private int transportTimeout = DEFAULT_TRANSPORT_TIMEOUT_MS;

	/**
	 * Constructor.
	 */
	public JMBusMBusNetwork() {
		super();
	}

	@Override
	public MBusConnection createConnection(int address) {
		return new LockingMBusConnection(new JMBusMBusConnection(address), lock, timeout, timeoutUnit,
				connectionString(address), log);
	}

	/**
	 * Create a jMBus connection instance.
	 *
	 * @return the connection instance
	 * @throws IOException
	 *         if the connection cannot be created
	 */
	protected abstract org.openmuc.jmbus.MBusConnection createJMBusConnection() throws IOException;

	/**
	 * Get a description of the network for display purposes.
	 *
	 * <p>
	 * A good description might be the serial port device name, for example.
	 * </p>
	 *
	 * @return a description, never {@literal null}
	 */
	protected abstract String getNetworkDescription();

	@Override
	public MBusData read(int address) throws IOException {
		try (MBusConnection conn = createConnection(address)) {
			conn.open();
			return conn.read();
		}
	}

	/**
	 * Get a description for a connection with a given address.
	 *
	 * @param address
	 *        the address
	 * @return the description
	 * @since 2.1
	 */
	protected String connectionString(int address) {
		StringBuilder builder = new StringBuilder();
		builder.append("JMBusMBusConnection{");
		builder.append(address);
		builder.append("@");
		builder.append(getNetworkDescription());
		builder.append("}");
		return builder.toString();
	}

	/**
	 *
	 * Connection class
	 */
	private class JMBusMBusConnection implements MBusConnection {

		private final int address;
		private org.openmuc.jmbus.MBusConnection conn;

		private JMBusMBusConnection(int address) {
			this.address = address;
		}

		@Override
		public String toString() {
			return connectionString(address);
		}

		@Override
		public void open() throws IOException {
			if ( conn == null ) {
				this.conn = createJMBusConnection();
			}
		}

		@Override
		public synchronized void close() {
			if ( conn != null ) {
				conn.close();
				conn = null;
			}
		}

		@Override
		public MBusData read() {
			try {
				final VariableDataStructure data = conn.read(address);
				log.debug("JMBus [{}] data read from primary address {}: {}", getNetworkDescription(),
						address, data);
				if ( data == null )
					return null;
				return JMBusConversion.from(data);
			} catch ( IOException e ) {
				log.error("Could not read from JMBus connection [{}]: {}", getNetworkDescription(),
						e.getMessage());
				return null;
			}
		}
	}

	/**
	 * Get setting specifiers for the configurable properties of this class.
	 *
	 * @return the setting specifiers
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(DEFAULT_TIMEOUT_SECS)));
		results.add(new BasicTextFieldSettingSpecifier("transportTimeout",
				String.valueOf(DEFAULT_TRANSPORT_TIMEOUT_MS)));
		return results;
	}

	/**
	 * Get the timeout value.
	 *
	 * @return the timeout value, defaults to {@literal 10}
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Set a timeout value.
	 *
	 * @param timeout
	 *        the timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get the timeout unit.
	 *
	 * @return the timeout unit; defaults to seconds
	 */
	public TimeUnit getTimeoutUnit() {
		return timeoutUnit;
	}

	/**
	 * Set the timeout unit.
	 *
	 * @param unit
	 *        the unit
	 */
	public void setTimeoutUnit(TimeUnit unit) {
		this.timeoutUnit = unit;
	}

	/**
	 * Get the transport timeout.
	 *
	 * @return the timeout, in milliseconds, or {@literal 0} for no timeout;
	 *         defaults to {@link #DEFAULT_TRANSPORT_TIMEOUT_MS}
	 * @since 2.2
	 */
	public int getTransportTimeout() {
		return transportTimeout;
	}

	/**
	 * Set the transport timeout.
	 *
	 * @param transportTimeout
	 *        the timeout to set, in milliseconds
	 * @since 2.2
	 */
	public void setTransportTimeout(int transportTimeout) {
		this.transportTimeout = transportTimeout;
	}

}
