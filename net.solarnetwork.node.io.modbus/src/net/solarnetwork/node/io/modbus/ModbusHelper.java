/* ==================================================================
 * ModbusHelper.java - Jul 15, 2013 7:54:17 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import java.util.BitSet;
import net.solarnetwork.util.OptionalService;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteCoilResponse;
import net.wimpi.modbus.net.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for working with Modbus.
 * 
 * @author matt
 * @version 1.0
 */
public final class ModbusHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ModbusHelper.class);

	/**
	 * Callback API for performing an action with a Modbus
	 * {@link SerialConnection}.
	 * 
	 * <p>
	 * If no result object is needed, simply use {@link Object} as the parameter
	 * type and return <em>null</em> from
	 * {@link #doInConnection(SerialConnection)}.
	 * </p>
	 * 
	 * @param <T>
	 *        the action return type
	 */
	public static interface ModbusConnectionCallback<T> {

		/**
		 * Perform an action with a Modbus {@link SerialConnection}.
		 * 
		 * <p>
		 * If no result object is needed, simply return <em>null</em>.
		 * </p>
		 * 
		 * @param conn
		 *        the connection
		 * @return the result
		 * @throws IOException
		 */
		T doInConnection(SerialConnection conn) throws IOException;
	}

	/**
	 * Perform some work with a Modbus {@link SerialConnection}.
	 * 
	 * <p>
	 * This method attempts to obtain a {@link SerialConnection} from the
	 * supplied {@link ModbusSerialConnectionFactory}. If the connection is
	 * obtained, it will call
	 * {@link ModbusConnectionCallback#doInConnection(SerialConnection)}, and
	 * then close the connection when finished.
	 * </p>
	 * 
	 * <p>
	 * <b>Note</b> that if either the connection factory is unavailable, or it
	 * fails to return a connection, the callback method will never be called.
	 * </p>
	 * 
	 * @param connectionFactory
	 *        the connection factory to use, via an {@link OptionalService}
	 * @param action
	 *        the connection callback
	 * @return the result of the callback, or <em>null</em> if the callback is
	 *         never invoked
	 */
	public static <T> T execute(OptionalService<ModbusSerialConnectionFactory> connectionFactory,
			ModbusConnectionCallback<T> action) {
		T result = null;
		ModbusSerialConnectionFactory factory = (connectionFactory == null ? null : connectionFactory
				.service());
		if ( factory != null ) {
			SerialConnection conn = factory.getSerialConnection();
			if ( conn != null ) {
				try {
					result = action.doInConnection(conn);
				} catch ( IOException e ) {
					throw new RuntimeException(e);
				} finally {
					conn.close();
				}
			}
		}
		return result;
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the count of registers to read with each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>addresses</code> parameter
	 */
	public static BitSet readDiscreetValues(SerialConnection conn, final Integer[] addresses,
			final int count, final int unitId) {
		BitSet result = new BitSet(addresses.length);
		try {
			for ( int i = 0; i < addresses.length; i++ ) {
				ModbusSerialTransaction trans = new ModbusSerialTransaction(conn);
				ReadCoilsRequest req = new ReadCoilsRequest(addresses[i], 1);
				req.setUnitID(unitId);
				req.setHeadless();
				trans.setRequest(req);
				try {
					trans.execute();
				} catch ( ModbusException e ) {
					throw new RuntimeException(e);
				}
				ReadCoilsResponse res = (ReadCoilsResponse) trans.getResponse();
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read coil {} response [{}]", addresses[i], res.getCoils());
				}
				result.set(i, res.getCoilStatus(0));
			}
		} finally {
			conn.close();
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus coil {} values: {}", addresses, result);
		}
		return result;
	}

	/**
	 * Set the value of a set of "coil" type registers.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param bits
	 *        a BitSet representing the value to set for each corresponding
	 *        {@code addresses} value
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>addresses</code> parameter
	 */
	public static Boolean writeDiscreetValues(SerialConnection conn, final Integer[] addresses,
			final BitSet bits, final int unitId) {
		for ( int i = 0; i < addresses.length; i++ ) {
			ModbusSerialTransaction trans = new ModbusSerialTransaction(conn);
			WriteCoilRequest req = new WriteCoilRequest(addresses[i], bits.get(i));
			req.setUnitID(unitId);
			req.setHeadless();
			trans.setRequest(req);
			try {
				trans.execute();
			} catch ( ModbusException e ) {
				throw new RuntimeException(e);
			}
			WriteCoilResponse res = (WriteCoilResponse) trans.getResponse();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Got write {} response [{}]", addresses[i], res.getCoil());
			}
		}
		return Boolean.TRUE;
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * @param connectionFactory
	 *        the connection factory to obtain a connection with
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the count of registers to read with each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>addresses</code> parameter
	 * @see readDiscreetValues(SerialConnection, Integer[], int, int)
	 */
	public static BitSet readDiscreetValues(
			OptionalService<ModbusSerialConnectionFactory> connectionFactory, final Integer[] addresses,
			final int count, final int unitId) {
		return execute(connectionFactory, new ModbusConnectionCallback<BitSet>() {

			@Override
			public BitSet doInConnection(SerialConnection conn) throws IOException {
				return readDiscreetValues(conn, addresses, count, unitId);
			}

		});
	}

}
