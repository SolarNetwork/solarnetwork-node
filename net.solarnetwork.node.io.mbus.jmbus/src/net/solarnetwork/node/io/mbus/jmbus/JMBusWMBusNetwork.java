/* ==================================================================
 * JMBusWMBusNetwork.java - 29/06/2020 12:36:22 pm
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.wireless.WMBusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.MBusMessage;
import net.solarnetwork.node.io.mbus.MBusMessageHandler;
import net.solarnetwork.node.io.mbus.MBusSecondaryAddress;
import net.solarnetwork.node.io.mbus.WMBusConnection;
import net.solarnetwork.node.io.mbus.WMBusNetwork;
import net.solarnetwork.service.support.BasicIdentifiable;

/**
 * Abstract jMBus implementation of {@link WMBusNetwork}.
 * 
 * @author alex
 * @version 2.1
 */
public abstract class JMBusWMBusNetwork extends BasicIdentifiable
		implements WMBusNetwork, WMBusListener {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private org.openmuc.jmbus.wireless.WMBusConnection connection;
	private final ConcurrentMap<org.openmuc.jmbus.SecondaryAddress, Set<MBusMessageHandler>> listeners = new ConcurrentHashMap<org.openmuc.jmbus.SecondaryAddress, Set<MBusMessageHandler>>();

	/**
	 * Constructor.
	 */
	public JMBusWMBusNetwork() {
		super();
	}

	/**
	 * Create a jMBus wireless connection instance.
	 * 
	 * @return the connection instance
	 * @throws IOException
	 *         if the connection cannot be created
	 */
	protected abstract org.openmuc.jmbus.wireless.WMBusConnection createJMBusConnection()
			throws IOException;

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

	private synchronized org.openmuc.jmbus.wireless.WMBusConnection getOrCreateConnection()
			throws IOException {
		if ( connection == null ) {
			connection = createJMBusConnection();
		}
		return connection;
	}

	@Override
	public WMBusConnection createConnection(MBusSecondaryAddress address, byte[] key) {
		return new JMBusWMBusConnection(address, key);
	}

	@Override
	public void newMessage(org.openmuc.jmbus.wireless.WMBusMessage message) {
		SecondaryAddress addr = message.getSecondaryAddress();

		log.debug("JMBus data received from secondary address {}: {}", addr, message);

		// route message to all registered listeners
		if ( addr != null ) {
			Set<MBusMessageHandler> handlers = listeners.get(addr);
			if ( handlers != null ) {
				MBusMessage msg = JMBusConversion.from(message);
				for ( MBusMessageHandler handler : handlers ) {
					handler.handleMessage(msg);
				}
			}
		}
	}

	@Override
	public void discardedBytes(byte[] bytes) {
	}

	@Override
	public void stoppedListening(IOException cause) {
		// TODO Handle this somehow
	}

	/**
	 * 
	 * Proxying connection class
	 */
	private class JMBusWMBusConnection implements WMBusConnection {

		private final SecondaryAddress address;
		private final byte[] key;
		private org.openmuc.jmbus.wireless.WMBusConnection conn;
		private MBusMessageHandler messageHandler = null;

		private JMBusWMBusConnection(MBusSecondaryAddress address, byte[] key) {
			this.address = JMBusConversion.to(address);
			this.key = key;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("JMBusWMBusConnection{");
			if ( address != null ) {
				builder.append(address.getDeviceId());
			}
			builder.append("@");
			builder.append(getNetworkDescription());
			builder.append("}");
			return builder.toString();
		}

		@Override
		public void open(MBusMessageHandler messageHandler) throws IOException {
			if ( conn == null ) {
				this.conn = getOrCreateConnection();
				if ( conn != null ) {
					Set<MBusMessageHandler> handlers = listeners.computeIfAbsent(address,
							k -> new CopyOnWriteArraySet<>());
					this.messageHandler = messageHandler;
					handlers.add(messageHandler);
					conn.addKey(this.address, key);
				}
			}
		}

		@Override
		public synchronized void close() {
			if ( conn != null ) {
				conn.removeKey(address);
				if ( messageHandler != null ) {
					Set<MBusMessageHandler> handlers = listeners.get(address);
					if ( handlers != null ) {
						handlers.remove(messageHandler);
					}
				}
			}
		}
	}
}
