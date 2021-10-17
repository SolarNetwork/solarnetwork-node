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
import org.openmuc.jmbus.VariableDataStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.MBusConnection;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusNetwork;
import net.solarnetwork.service.support.BasicIdentifiable;

/**
 * Abstract jMBus implementation of {@link MBusNetwork}.
 * 
 * @author alex
 * @version 2.0
 */
public abstract class JMBusMBusNetwork extends BasicIdentifiable implements MBusNetwork {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public MBusConnection createConnection(int address) {
		return new JMBusMBusConnection(address);
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
			StringBuilder builder = new StringBuilder();
			builder.append("JMBusMBusConnection{");
			builder.append(address);
			builder.append("@");
			builder.append(getNetworkDescription());
			builder.append("}");
			return builder.toString();
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
				log.debug("JMBus data read from primary address {}: {}", address, data);
				if ( data == null )
					return null;
				return JMBusConversion.from(data);
			} catch ( IOException e ) {
				log.error("Could not read from JMBus connection: {}", e.getMessage());
				return null;
			}
		}
	}
}
