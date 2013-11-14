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
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.util.OptionalService;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteCoilResponse;
import net.wimpi.modbus.net.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for working with Modbus.
 * 
 * @author matt
 * @version 1.1
 */
public final class ModbusHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ModbusHelper.class);

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
			result = factory.execute(action);
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

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param unitId
	 * @return map of integer addresses to corresponding integer values, there
	 *         should be {@code count} values for each {@code address} read
	 */
	public static Map<Integer, Integer> readInputValues(SerialConnection conn,
			final Integer[] addresses, final int count, final int unitId) {
		Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>((addresses == null ? 0
				: addresses.length) * count);
		try {
			for ( int i = 0; i < addresses.length; i++ ) {
				ModbusSerialTransaction trans = new ModbusSerialTransaction(conn);
				ReadInputRegistersRequest req = new ReadInputRegistersRequest(addresses[i], count);
				req.setUnitID(unitId);
				req.setHeadless();
				trans.setRequest(req);
				try {
					trans.execute();
				} catch ( ModbusException e ) {
					throw new RuntimeException(e);
				}
				ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();
				for ( int w = 0; w < res.getWordCount(); w++ ) {
					if ( LOG.isTraceEnabled() ) {
						LOG.trace("Got Modbus read input {} response {}", addresses[i] + w,
								res.getRegisterValue(w));
					}
					result.put(addresses[i] + w, res.getRegisterValue(w));
				}
			}
		} finally {
			conn.close();
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus input registers {} values: {}", addresses, result);
		}
		return result;
	}

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * @param connectionFactory
	 *        the connection factory to obtain a connection with
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param unitId
	 * @return list of integer values, there should be {@code count} values for
	 *         each {@code address} read
	 */
	public static Map<Integer, Integer> readInputValues(
			OptionalService<ModbusSerialConnectionFactory> connectionFactory, final Integer[] addresses,
			final int count, final int unitId) {
		return execute(connectionFactory, new ModbusConnectionCallback<Map<Integer, Integer>>() {

			@Override
			public Map<Integer, Integer> doInConnection(SerialConnection conn) throws IOException {
				return readInputValues(conn, addresses, count, unitId);
			}

		});
	}

	/**
	 * Get a 32-bit Modbus long word from a 16-bit high word and a 16-bit low
	 * word.
	 * 
	 * @param hiWord
	 *        the high word
	 * @param loWord
	 *        the low word
	 * @return a 32-bit long word value
	 */
	public static int getLongWord(int hiWord, int loWord) {
		return ((hiWord & 0xFFFF) << 16 | (loWord & 0xFFFF));
	}

}
