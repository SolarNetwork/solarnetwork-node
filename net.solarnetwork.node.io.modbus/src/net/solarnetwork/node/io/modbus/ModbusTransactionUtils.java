/* ==================================================================
 * ModbusTransactionUtils.java - 3/02/2018 8:53:29 AM
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

package net.solarnetwork.node.io.modbus;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteCoilResponse;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.procimg.InputRegister;

/**
 * Utility methods for Modbus actions.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public class ModbusTransactionUtils {

	/** The UTF-8 character set name. */
	public static final String UTF8_CHARSET = "UTF-8";

	/** The ASCII character set name. */
	public static final String ASCII_CHARSET = "US-ASCII";

	private static final Logger LOG = LoggerFactory.getLogger(ModbusTransactionUtils.class);

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 1} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the count of registers to read with each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>addresses</code> parameter
	 */
	public static BitSet readDiscreetValues(ModbusTransaction trans, Integer[] addresses, int count,
			int unitId) {
		BitSet result = new BitSet(addresses.length);
		for ( int i = 0; i < addresses.length; i++ ) {
			BitSet set = readDiscreteValues(trans, addresses[i], count, unitId);
			result.or(set);
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus coil {} values: {}", addresses, result);
		}
		return result;
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 1} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of registers to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>address</code> parameter
	 */
	public static BitSet readDiscreteValues(ModbusTransaction trans, Integer address, int count,
			int unitId) {
		BitSet result = new BitSet(count);
		ReadCoilsRequest req = new ReadCoilsRequest(address, count);
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
			LOG.trace("Got Modbus read coil {} response [{}]", address, res.getCoils());
		}
		for ( int i = 0; i < res.getBitCount(); i++ ) {
			result.set(address + i, res.getCoilStatus(i));
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read {} Modbus coil {} values: {}", count, address, result);
		}
		return result;
	}

	/**
	 * Set the value of a set of "coil" type registers.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 5} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
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
	public static Boolean writeDiscreetValues(ModbusTransaction trans, Integer[] addresses, BitSet bits,
			int unitId) {
		for ( int i = 0; i < addresses.length; i++ ) {
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
	 * Get the values of a set of "input discrete" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 2} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of registers to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>address</code> parameter
	 */
	public static BitSet readInputDiscreteValues(ModbusTransaction trans, Integer address, int count,
			int unitId) {
		BitSet result = new BitSet(count);
		ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new RuntimeException(e);
		}
		ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) trans.getResponse();
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Got Modbus read input discretes {} response [{}]", address, res.getDiscretes());
		}
		for ( int i = 0; i < res.getBitCount(); i++ ) {
			result.set(address + i, res.getDiscreteStatus(i));
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read {} Modbus input discrete {} values: {}", count, address, result);
		}
		return result;
	}

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 4} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return map of integer addresses to corresponding integer values, there
	 *         should be {@code count} values for each {@code address} read
	 */
	public static Map<Integer, Integer> readInputValues(ModbusTransaction trans, Integer[] addresses,
			int count, int unitId) {
		Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>(
				(addresses == null ? 0 : addresses.length) * count);
		for ( int i = 0; i < addresses.length; i++ ) {
			int[] data = readInputValues(trans, addresses[i], count, unitId);
			if ( data != null ) {
				for ( int j = 0; j < data.length; j++ ) {
					result.put(addresses[i] + j, data[j]);
				}
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus input registers {} values: {}", addresses, result);
		}
		return result;
	}

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 4} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return register values, starting with {@code address} to
	 *         {@code address + count}
	 */
	public static int[] readInputValues(ModbusTransaction trans, Integer address, int count,
			int unitId) {
		ReadInputRegistersRequest req = new ReadInputRegistersRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new RuntimeException(e);
		}
		int[] result = new int[count];
		ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();
		for ( int w = 0; w < res.getWordCount(); w++ ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Got Modbus read input {} response {}", address + w, res.getRegisterValue(w));
			}
			result[w] = res.getRegisterValue(w);
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus input registers {} values: {}", address, result);
		}
		return result;
	}

	/**
	 * Get the raw bytes of specific registers as an array.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 3} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 2-byte "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register bytes; the result will have a length equal to
	 *         {@code count * 2}
	 */
	public static byte[] readBytes(ModbusTransaction trans, Integer address, int count, int unitId) {
		byte[] result = new byte[count * 2];
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new RuntimeException(e);
		}
		ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
		InputRegister[] registers = res.getRegisters();
		if ( registers != null ) {

			for ( int i = 0; i < registers.length; i++ ) {
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + i, res.getRegisterValue(i));
				}
				System.arraycopy(registers[i].toBytes(), 0, result, i * 2, 2);
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} bytes: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Read a set of "input" type registers and interpret as a string. This uses
	 * a Modbus function code {@code 3} request.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param trim
	 *        if <em>true</em> then remove leading/trailing whitespace from the
	 *        resulting string
	 * @param charsetName
	 *        the character set to interpret the bytes as
	 * @return String from interpreting raw bytes as a string
	 * @see #readBytes(SerialConnection, Integer, int, int)
	 */
	public static String readString(ModbusTransaction trans, Integer address, int count, int unitId,
			boolean trim, String charsetName) {
		final byte[] bytes = readBytes(trans, address, count, unitId);
		String result = null;
		if ( bytes != null ) {
			try {
				result = new String(bytes, charsetName);
				if ( trim ) {
					result = result.trim();
				}
			} catch ( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus input register {} count {} string: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Read a set of "input" type registers and interpret as a US-ASCII encoded
	 * string.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param trim
	 *        if <em>true</em> then remove leading/trailing whitespace from the
	 *        resulting string
	 * @return String from interpreting raw bytes as a US-ASCII encoded string
	 * @see #readString(ModbusTransaction, Integer, int, int, boolean, String)
	 */
	public static String readASCIIString(final ModbusTransaction trans, final Integer address,
			final int count, final int unitId, final boolean trim) {
		return readString(trans, address, count, unitId, trim, ASCII_CHARSET);
	}

	/**
	 * Read a set of "input" type registers and interpret as a UTF-8 encoded
	 * string.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param trim
	 *        if <em>true</em> then remove leading/trailing whitespace from the
	 *        resulting string
	 * @return String from interpreting raw bytes as a UTF-8 encoded string
	 * @see #readString(ModbusTransaction, Integer, int, int, boolean, String)
	 */
	public static String readUTF8String(final ModbusTransaction trans, final Integer address,
			final int count, final int unitId, final boolean trim) {
		return readString(trans, address, count, unitId, trim, UTF8_CHARSET);
	}

	/**
	 * Get the values of specific registers as an array of unsigned integers.
	 * This uses a Modbus function code {@code 3} request.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 */
	public static int[] readInts(ModbusTransaction trans, Integer address, int count, int unitId) {
		int[] result = new int[count];
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new RuntimeException(e);
		}
		ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
		for ( int w = 0; w < res.getWordCount(); w++ ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Got Modbus read {} response {}", address + w, res.getRegisterValue(w));
			}
			result[w] = res.getRegisterValue(w);
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} values: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Get the values of specific registers as an array of signed shorts. This
	 * uses a Modbus function code {@code 3} request.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 */
	public static short[] readSignedShorts(ModbusTransaction trans, Integer address, int count,
			int unitId) {
		short[] result = new short[count];
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new RuntimeException(e);
		}
		ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
		for ( int w = 0; w < res.getWordCount(); w++ ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Got Modbus read {} response {}", address + w, res.getRegisterValue(w));
			}
			result[w] = res.getRegister(w).toShort();
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} shorts: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Get the values of specific registers as an array. This uses a Modbus
	 * function code {@code 3} request.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 */
	public static Integer[] readValues(ModbusTransaction trans, Integer address, int count, int unitId) {
		Integer[] result = new Integer[count];
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new RuntimeException(e);
		}
		ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
		for ( int w = 0; w < res.getWordCount(); w++ ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Got Modbus read {} response {}", address + w, res.getRegisterValue(w));
			}
			result[w] = res.getRegisterValue(w);
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} values: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

}
