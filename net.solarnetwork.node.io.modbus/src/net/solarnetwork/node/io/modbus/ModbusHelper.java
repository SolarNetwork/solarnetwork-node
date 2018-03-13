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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.util.OptionalService;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Helper methods for working with Modbus serial connection.
 * 
 * @author matt
 * @version 1.7
 */
public final class ModbusHelper {

	/** The UTF-8 character set name. */
	public static final String UTF8_CHARSET = ModbusTransactionUtils.UTF8_CHARSET;

	/** The ASCII character set name. */
	public static final String ASCII_CHARSET = ModbusTransactionUtils.ASCII_CHARSET;

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
	 * @return the result of the callback, or {@literal null} if the callback is
	 *         never invoked
	 */
	public static <T> T execute(OptionalService<ModbusSerialConnectionFactory> connectionFactory,
			ModbusConnectionCallback<T> action) {
		T result = null;
		ModbusSerialConnectionFactory factory = (connectionFactory == null ? null
				: connectionFactory.service());
		if ( factory != null ) {
			result = factory.execute(action);
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
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readDiscreetValues(net.wimpi.modbus.io.ModbusTransaction, Integer[], int, int)}
	 */
	@Deprecated
	public static BitSet readDiscreetValues(SerialConnection conn, final Integer[] addresses,
			final int count, final int unitId) {
		return ModbusTransactionUtils.readDiscreetValues(new ModbusSerialTransaction(conn), addresses,
				count, unitId);
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 1} request.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of registers to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>address</code> parameter
	 * @since 1.5
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readDiscreteValues(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static BitSet readDiscreteValues(SerialConnection conn, final Integer address,
			final int count, final int unitId) {
		return ModbusTransactionUtils.readDiscreteValues(new ModbusSerialTransaction(conn), address,
				count, unitId);
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 1} request.
	 * </p>
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
	 * Set the value of a set of "coil" type registers.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 5} request.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use @param addresses the Modbus register
	 *        addresses to read @param bits a BitSet representing the value to
	 *        set for each corresponding {@code addresses} value @param unitId
	 *        the Modbus unit ID to use in the read request @return BitSet, with
	 *        each index corresponding to an index in the <code>addresses</code>
	 *        parameter @deprecated use
	 *        {@link ModbusTransactionUtils#writeDiscreetValues(net.wimpi.modbus.io.ModbusTransaction, Integer[], BitSet, int)
	 */
	@Deprecated
	public static Boolean writeDiscreetValues(SerialConnection conn, final Integer[] addresses,
			final BitSet bits, final int unitId) {
		return ModbusTransactionUtils.writeDiscreetValues(new ModbusSerialTransaction(conn), addresses,
				bits, unitId);
	}

	/**
	 * Get the values of a set of "input discrete" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 2} request.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of registers to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>address</code> parameter
	 * @since 1.5
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readInputDiscreteValues(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static BitSet readInputDiscreteValues(SerialConnection conn, final Integer address,
			final int count, final int unitId) {
		return ModbusTransactionUtils.readInputDiscreteValues(new ModbusSerialTransaction(conn), address,
				count, unitId);
	}

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 4} request.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return register values, starting with {@code address} to
	 *         {@code address + count}
	 * @since 1.5
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readInputValues(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static int[] readInputValues(SerialConnection conn, final Integer address, final int count,
			final int unitId) {
		return ModbusTransactionUtils.readInputValues(new ModbusSerialTransaction(conn), address, count,
				unitId);
	}

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 4} request.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return map of integer addresses to corresponding integer values, there
	 *         should be {@code count} values for each {@code address} read
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readInputValues(net.wimpi.modbus.io.ModbusTransaction, Integer[], int, int)}
	 */
	@Deprecated
	public static Map<Integer, Integer> readInputValues(SerialConnection conn, final Integer[] addresses,
			final int count, final int unitId) {
		return ModbusTransactionUtils.readInputValues(new ModbusSerialTransaction(conn), addresses,
				count, unitId);
	}

	/**
	 * Get the values of specific "input" type registers. This uses a Modbus
	 * function code {@code 4} request.
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
	 * Get the values of specific registers as an array. This uses a Modbus
	 * function code {@code 3} request.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readValues(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static Integer[] readValues(SerialConnection conn, final Integer address, final int count,
			final int unitId) {
		return ModbusTransactionUtils.readValues(new ModbusSerialTransaction(conn), address, count,
				unitId);
	}

	/**
	 * Get the values of specific registers as an array of unsigned integers.
	 * This uses a Modbus function code {@code 3} request.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readInts(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static int[] readInts(SerialConnection conn, final Integer address, final int count,
			final int unitId) {
		return ModbusTransactionUtils.readInts(new ModbusSerialTransaction(conn), address, count,
				unitId);
	}

	/**
	 * Get the values of specific registers as an array of signed shorts. This
	 * uses a Modbus function code {@code 3} request.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readSignedShorts(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static short[] readSignedShorts(SerialConnection conn, final Integer address, final int count,
			final int unitId) {
		return ModbusTransactionUtils.readSignedShorts(new ModbusSerialTransaction(conn), address, count,
				unitId);
	}

	/**
	 * Get the raw bytes of specific registers as an array.
	 * 
	 * <p>
	 * This uses a Modbus function code {@code 3} request.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param address
	 *        the Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 2-byte "words" to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @return array of register bytes; the result will have a length equal to
	 *         {@code count * 2}
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readBytes(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int)}
	 */
	@Deprecated
	public static byte[] readBytes(final SerialConnection conn, final Integer address, final int count,
			final int unitId) {
		return ModbusTransactionUtils.readBytes(new ModbusSerialTransaction(conn), address, count,
				unitId);
	}

	/**
	 * Read a set of "input" type registers and interpret as a UTF-8 encoded
	 * string.
	 * 
	 * @param conn
	 *        the Modbus connection to use
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
	 * @see #readString(SerialConnection, Integer, int, int, boolean, String)
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readUTF8String(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int, boolean)}
	 */
	@Deprecated
	public static String readUTF8String(final SerialConnection conn, final Integer address,
			final int count, final int unitId, final boolean trim) {
		return readString(conn, address, count, unitId, trim, UTF8_CHARSET);
	}

	/**
	 * Read a set of "input" type registers and interpret as a US-ASCII encoded
	 * string.
	 * 
	 * @param conn
	 *        the Modbus connection to use
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
	 * @see #readString(SerialConnection, Integer, int, int, boolean, String)
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readASCIIString(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int, boolean)}
	 */
	@Deprecated
	public static String readASCIIString(final SerialConnection conn, final Integer address,
			final int count, final int unitId, final boolean trim) {
		return readString(conn, address, count, unitId, trim, ASCII_CHARSET);
	}

	/**
	 * Read a set of "input" type registers and interpret as a string. This uses
	 * a Modbus function code {@code 3} request.
	 * 
	 * @param conn
	 *        the Modbus connection to use
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
	 * @deprecated use
	 *             {@link ModbusTransactionUtils#readString(net.wimpi.modbus.io.ModbusTransaction, Integer, int, int, boolean, String)}
	 */
	@Deprecated
	public static String readString(final SerialConnection conn, final Integer address, final int count,
			final int unitId, final boolean trim, final String charsetName) {
		return ModbusTransactionUtils.readString(new ModbusSerialTransaction(conn), address, count,
				unitId, trim, charsetName);
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
		return (((hiWord & 0xFFFF) << 16) | (loWord & 0xFFFF));
	}

	/**
	 * Parse an IEEE-754 32-bit float value from raw Modbus register values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of at least
	 * {@code offset} + {@literal 1}, and be arranged in big-endian order.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed float, or {@literal null} if not available or parsed
	 *         float is {@code NaN}
	 */
	public static Float parseFloat32(final int[] data, int offset) {
		Float result = null;
		if ( data != null && (offset + 1) < data.length ) {
			result = parseFloat32(data[offset], data[offset + 1]);
		}
		return result;
	}

	/**
	 * Parse an IEEE-754 32-bit float value from raw Modbus register values.
	 * 
	 * @param hi
	 *        the high 16 bits
	 * @param lo
	 *        the low 16 bits
	 * @return the parsed float, or {@literal null} if not available or parsed
	 *         float is {@code NaN}
	 */
	public static Float parseFloat32(final int hi, final int lo) {
		Long int32 = parseInt32(hi, lo);
		Float result = Float.intBitsToFloat(int32.intValue());
		if ( result.isNaN() ) {
			LOG.trace("Float32 data results in NaN: {} {}", hi, lo);
			result = null;
		}
		return result;
	}

	/**
	 * Parse an IEEE-754 32-bit float value from raw Modbus register values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of {@code 2}, and be
	 * arranged in big-endian order.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed float, or {@literal null} if not available or parsed
	 *         float is {@code NaN}
	 */
	public static Float parseFloat32(final Integer[] data) {
		Float result = null;
		if ( data != null && data.length == 2 ) {
			result = Float.intBitsToFloat(
					((data[0].intValue() & 0xFFFF) << 16) | (data[1].intValue() & 0xFFFF));
			if ( result.isNaN() ) {
				LOG.trace("Float32 data results in NaN: {}", (Object) data);
				result = null;
			}
		}
		return result;
	}

	/**
	 * Parse an IEEE-754 64-bit floating point value from raw Modbus register
	 * values.
	 * 
	 * @param h1
	 *        bits 63-48
	 * @param h2
	 *        bits 47-32
	 * @param l1
	 *        bits 31-16
	 * @param l2
	 *        bits 15-0
	 * @return the parsed {@code Double}, or {@literal null} if the result is
	 *         {@code NaN}
	 * @since 1.5
	 */
	public static Double parseFloat64(final int h1, final int h2, final int l1, final int l2) {
		Long l = parseInt64(h1, h2, l1, l2);
		Double result = Double.longBitsToDouble(l);
		if ( result.isNaN() ) {
			LOG.trace("Float64 data results in NaN: {} {} {} {}", h1, h2, l1, l2);
			result = null;
		}
		return result;
	}

	/**
	 * Parse an IEEE-754 64-bit floating point value from raw Modbus register
	 * values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of at least
	 * {@code offset} + {@literal 3}, and be arranged in big-endian order.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed {@code Double}, or {@literal null} if not {@code data}
	 *         is not suitable or parsed value is {@code NaN}
	 * @since 1.5
	 */
	public static Double parseFloat64(final int[] data, final int offset) {
		Double result = null;
		if ( data != null && (offset + 3) < data.length ) {
			result = parseFloat64(data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
		}
		return result;
	}

	/**
	 * Parse an IEEE-754 64-bit floating point value from raw Modbus register
	 * values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of {@literal 4}, and
	 * be arranged in big-endian order.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed {@code Double}, or {@literal null} if {@code data} is
	 *         not suitable or parsed value is {@code NaN}
	 * @since 1.5
	 */
	public static Double parseFloat64(final Integer[] data) {
		Double result = null;
		if ( data != null && data.length > 3 ) {
			result = parseFloat64(data[0], data[1], data[2], data[3]);
		}
		return result;
	}

	/**
	 * Parse a 64-bit long value from raw Modbus register values. The
	 * {@code data} array is expected to have a length of {@code 4}, and be
	 * arranged in big-endian order.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed long
	 */
	public static Long parseInt64(final Integer[] data) {
		Long result = null;
		if ( data != null && data.length == 4 ) {
			result = parseInt64(data[0], data[1], data[2], data[3]);
		}
		return result;
	}

	/**
	 * Parse a 64-bit integer value from raw Modbus register values.
	 * 
	 * @param h1
	 *        bits 63-48
	 * @param h2
	 *        bits 47-32
	 * @param l1
	 *        bits 31-16
	 * @param l2
	 *        bits 15-0
	 * @return the parsed long, never {@literal null}
	 */
	public static Long parseInt64(final int h1, final int h2, final int l1, final int l2) {
		return (((h1 & 0xFFFFL) << 48) | ((h2 & 0xFFFFL) << 32) | ((l1 & 0xFFFFL) << 16)
				| (l2 & 0xFFFFL));
	}

	/**
	 * Parse a 32-bit long value from raw Modbus register values. The
	 * {@code data} array is expected to have a length of at least
	 * {@code offset} + {@code 1}, and be arranged in big-endian order.
	 * 
	 * <p>
	 * <b>Note</b> a {@code Long} is returned to support unsigned 32-bit values.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset in the array to parse the 32-bit value
	 * @return the parsed long, or {@literal null} if {@code data} is
	 *         {@literal null} or not long enough to read from
	 */
	public static Long parseInt32(final int[] data, final int offset) {
		Long result = null;
		if ( data != null && (offset + 1) < data.length ) {
			result = parseInt32(data[offset], data[offset + 1]);
		}
		return result;
	}

	/**
	 * Parse a 32-bit unsigned integer value from raw Modbus register values.
	 * 
	 * <p>
	 * <b>Note</b> a {@code Long} is returned to support unsigned 32-bit values.
	 * </p>
	 * 
	 * @param hi
	 *        bits 31-16
	 * @param lo
	 *        bits 15-0
	 * @return the parsed long, never {@literal null}
	 * @since 1.5
	 */
	public static Long parseInt32(final int hi, final int lo) {
		return (((hi & 0xFFFFL) << 16) | (lo & 0xFFFFL));
	}

	/**
	 * Convert an array of ints to Integer objects.
	 * 
	 * @param array
	 *        the array to convert
	 * @return the converted array, or {@literal null} if {@code array} is
	 *         {@literal null}
	 */
	public static Integer[] integerArray(int[] array) {
		if ( array == null ) {
			return null;
		}
		int count = array.length;
		Integer[] result = new Integer[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = array[i];
		}
		return result;
	}

}
