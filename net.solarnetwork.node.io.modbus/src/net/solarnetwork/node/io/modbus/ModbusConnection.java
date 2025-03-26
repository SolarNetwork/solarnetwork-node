/* ==================================================================
 * ModbusConnection.java - Jul 29, 2014 11:19:18 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * High level Modbus connection API.
 *
 * <p>
 * This API aims to simplify accessing Modbus capable devices without having any
 * direct dependency on Jamod (or any other Modbus implementation).
 * </p>
 *
 * @author matt
 * @version 3.2
 * @since 2.0
 */
public interface ModbusConnection extends Closeable {

	/**
	 * Get the Modbus Unit ID this device represents.
	 *
	 * @return the unit ID
	 */
	int getUnitId();

	/**
	 * Open the connection, if it is not already open. The connection must be
	 * opened before calling any of the other methods in this API.
	 *
	 * @throws IOException
	 *         if the connection cannot be opened
	 * @throws LockTimeoutException
	 *         if a lock is required to open the connection and it could not be
	 *         obtained within a configured maximum amount of time
	 */
	void open() throws IOException, LockTimeoutException;

	/**
	 * Close the connection, if it is open.
	 */
	@Override
	void close();

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 1} request.
	 * </p>
	 *
	 * <p>
	 * This method by default invokes {@link #readDiscreetValues(int, int)} for
	 * backwards compatibility.
	 * </p>
	 *
	 * @param address
	 *        the 0-based Modbus register address to read
	 * @param count
	 *        the count of discreet registers to read
	 * @return BitSet, with indexes set from {@literal 0} to a {@code count - 1}
	 * @since 3.1
	 * @throws IOException
	 *         if any communication error occurs
	 */
	default BitSet readDiscreteValues(int address, int count) throws IOException {
		return readDiscreetValues(address, count);
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 1} request.
	 * </p>
	 *
	 * <p>
	 * This method is required but deprecated to preserve backwards
	 * compatibility.
	 * </p>
	 *
	 * @param address
	 *        the 0-based Modbus register address to read
	 * @param count
	 *        the count of discreet registers to read
	 * @return BitSet, with indexes set from {@literal 0} to a {@code count - 1}
	 * @since 1.1
	 * @throws IOException
	 *         if any communication error occurs
	 * @deprecated since 3.1 use {@link #readDiscreteValues(int, int)}
	 */
	@Deprecated
	BitSet readDiscreetValues(int address, int count) throws IOException;

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 1} request. The returned set
	 * will have a size equal to {@code addresses.length * count}.
	 * </p>
	 *
	 * <p>
	 * This method by default invokes {@link #readDiscreetValues(int, int)} for
	 * backwards compatibility.
	 * </p>
	 *
	 * @param addresses
	 *        the 0-based Modbus register addresses to read
	 * @param count
	 *        the count of coils to read with each address
	 * @return BitSet, with each {@code count} indexes for each index in the
	 *         {@code addresses} parameter
	 * @throws IOException
	 *         if any communication error occurs
	 */
	default BitSet readDiscreteValues(int[] addresses, int count) throws IOException {
		return readDiscreetValues(addresses, count);
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 1} request. The returned set
	 * will have a size equal to {@code addresses.length * count}.
	 * </p>
	 *
	 * <p>
	 * This method is required but deprecated to preserve backwards
	 * compatibility.
	 * </p>
	 *
	 * @param addresses
	 *        the 0-based Modbus register addresses to read
	 * @param count
	 *        the count of coils to read with each address
	 * @return BitSet, with each {@code count} indexes for each index in the
	 *         {@code addresses} parameter
	 * @throws IOException
	 *         if any communication error occurs
	 * @deprecated since 3.1 use {@link #readDiscreteValues(int[], int)}
	 */
	@Deprecated
	BitSet readDiscreetValues(int[] addresses, int count) throws IOException;

	/**
	 * Write values of a set of "coil" type registers, via a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 5} request, once for each
	 * address in {@code addresses}. Each address at index <em>i</em>
	 * corresponds to the value of bit at index <em>i</em>. Thus bits
	 * {@literal 0} to {@code addresses.length - 1} are used.
	 * </p>
	 *
	 * <p>
	 * This method by default invokes
	 * {@link #writeDiscreetValues(int[], BitSet)} for backwards compatibility.
	 * </p>
	 *
	 * @param addresses
	 *        the Modbus register addresses to start writing to
	 * @param bits
	 *        the bits to write, each index corresponding to an index in
	 *        {@code addresses}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	default void writeDiscreteValues(int[] addresses, BitSet bits) throws IOException {
		writeDiscreetValues(addresses, bits);
	}

	/**
	 * Write values of a set of "coil" type registers, via a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 5} request, once for each
	 * address in {@code addresses}. Each address at index <em>i</em>
	 * corresponds to the value of bit at index <em>i</em>. Thus bits
	 * {@literal 0} to {@code addresses.length - 1} are used.
	 * </p>
	 *
	 * <p>
	 * This method is required but deprecated to preserve backwards
	 * compatibility.
	 * </p>
	 *
	 * @param addresses
	 *        the Modbus register addresses to start writing to
	 * @param bits
	 *        the bits to write, each index corresponding to an index in
	 *        {@code addresses}
	 * @throws IOException
	 *         if any communication error occurs
	 * @deprecated since 3.1 use {@link #writeDiscreteValues(int[], BitSet)}
	 */
	@Deprecated
	void writeDiscreetValues(int[] addresses, BitSet bits) throws IOException;

	/**
	 * Write values of a set of "coil" type registers, via a BitSet.
	 *
	 * <p>
	 * The bit indexes in {@code bits} starting at {@code 0} represents the
	 * value for the Modbus register at {@code address}. Increasing bit indexes
	 * correspond to increasing Modbus regsiter offsets from {@code address}.
	 * </p>
	 *
	 * <p>
	 * For API backwards compatibility this default implementation simply calls
	 * {@link #writeDiscreteValues(int[], BitSet)}.
	 * </p>
	 *
	 * @param function
	 *        the Modbus function code to use, one of
	 *        {@link ModbusWriteFunction#WriteCoil} or
	 *        {@link ModbusWriteFunction#WriteMultipleCoils}
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the count of bits to write
	 * @param bits
	 *        the bits to write
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 3.2
	 */
	default void writeDiscreteValues(ModbusWriteFunction function, int address, int count, BitSet bits)
			throws IOException {
		writeDiscreteValues(new int[] { address }, bits);
	}

	/**
	 * Get the values of a set of "input discrete" type registers, as a BitSet.
	 *
	 * <p>
	 * This uses a Modbus function code {@literal 2} request. The returned
	 * bitset will have {@code count} values set, from {@literal 0} to
	 * {@code count - 1}.
	 * </p>
	 *
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of registers to read
	 * @return BitSet, with each {@literal 0} to {@code count} indexes
	 * @throws IOException
	 *         if any communication error occurs
	 */
	BitSet readInputDiscreteValues(int address, int count) throws IOException;

	/**
	 * Get the values of specific 16-bit Modbus registers as an array of 16-bit
	 * words.
	 *
	 * <p>
	 * Note that the raw short values can be treated as unsigned shorts by
	 * converting them to integers, like
	 * {@code int unsigned = ((int)s) && 0xFFFF}, or by calling
	 * {@link Short#toUnsignedInt(short)}. Thus the values returned by this
	 * method are technically the same as those returned by
	 * {@link #readWordsUnsigned(ModbusReadFunction, int, int)}, without having
	 * been cast to ints.
	 * </p>
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	short[] readWords(ModbusReadFunction function, int address, int count) throws IOException;

	/**
	 * Get the values of specific 16-bit Modbus registers as an array of
	 * unsigned 16-bit words.
	 *
	 * <p>
	 * Note that the raw int values can be treated as signed shorts by casting
	 * them to shorts, like {@code short signed = (short)s}. Thus the values
	 * returned by this method are technically the same as those returned by
	 * {@link #readWords(ModbusReadFunction, int, int)}, having been cast to
	 * ints.
	 * </p>
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of 16-bit Modbus registers to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	int[] readWordsUnsigned(ModbusReadFunction function, int address, int count) throws IOException;

	/**
	 * Write 16-bit word values to 16-bit Modbus registers.
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to
	 * @param values
	 *        the 16-bit values to write
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void writeWords(ModbusWriteFunction function, int address, short[] values) throws IOException;

	/**
	 * Write unsigned 16-bit word values to 16-bit Modbus registers.
	 *
	 * <p>
	 * All the elements in {@code values} will be truncated to 16-bits and then
	 * stored in Modbus registers.
	 * </p>
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to
	 * @param values
	 *        the unsigned 16-bit values to write
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void writeWords(ModbusWriteFunction function, int address, int[] values) throws IOException;

	/**
	 * Get the raw bytes of specific registers.
	 *
	 * <p>
	 * Each 16-bit modbus register value will be decomposed into two output
	 * bytes, so that the returned result will have a length equal to
	 * {@code count * 2}.
	 * </p>
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @return register words as an array of bytes
	 * @throws IOException
	 *         if any communication error occurs
	 */
	byte[] readBytes(ModbusReadFunction function, int address, int count) throws IOException;

	/**
	 * Write raw byte values to registers.
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to;
	 *        {@code values.length * 2} 16-bit registers will be written
	 * @param values
	 *        the byte values to write
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void writeBytes(ModbusWriteFunction function, int address, byte[] values) throws IOException;

	/**
	 * Read a set of registers as bytes and interpret as a string.
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @param charset
	 *        the character set to interpret the bytes as
	 * @return String from interpreting raw bytes as a string
	 * @see #readBytes(ModbusReadFunction, int, int)
	 * @throws IOException
	 *         if any communication error occurs
	 */
	String readString(ModbusReadFunction function, int address, int count, boolean trim, Charset charset)
			throws IOException;

	/**
	 * Write a string as raw byte values to registers.
	 *
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to
	 * @param value
	 *        the string value to write
	 * @param charset
	 *        the character set to interpret the bytes as
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void writeString(ModbusWriteFunction function, int address, String value, Charset charset)
			throws IOException;

}
