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

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import net.solarnetwork.node.LockTimeoutException;

/**
 * High level Modbus connection API.
 * 
 * <p>
 * This API aims to simplify accessing Modbus capable devices without having any
 * direct dependency on Jamod (or any other Modbus implementation).
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface ModbusConnection {

	/** The UTF-8 character set. */
	final String UTF8_CHARSET = "UTF-8";

	/** The US-ASCII character set. */
	final String ASCII_CHARSET = "US-ASCII";

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
	 */
	void open() throws IOException, LockTimeoutException;

	/**
	 * Close the connection, if it is open.
	 */
	void close();

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * @param addresses
	 *        the 0-based Modbus register addresses to read
	 * @param count
	 *        the count of registers to read with each address
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>addresses</code> parameter
	 */
	BitSet readDiscreetValues(final Integer[] addresses, final int count);

	/**
	 * Set the value of a set of "coil" type registers.
	 * 
	 * @param addresses
	 *        the 0-based Modbus register addresses to read
	 * @param bits
	 *        a BitSet representing the value to set for each corresponding
	 *        {@code addresses} value
	 * @return BitSet, with each index corresponding to an index in the
	 *         <code>addresses</code> parameter
	 */
	Boolean writeDiscreetValues(final Integer[] addresses, final BitSet bits);

	/**
	 * Get the values of specific "input" type registers.
	 * 
	 * @param addresses
	 *        the 0-based Modbus register addresses to read
	 * @param count
	 *        the number of Modbus "words" to read from each address
	 * @return map of integer addresses to corresponding integer values, there
	 *         should be {@code count} values for each {@code address} read
	 */
	Map<Integer, Integer> readInputValues(final Integer[] addresses, final int count);

	/**
	 * Get the raw bytes of specific registers as an array. This uses a Modbus
	 * function code {@code 3} request.
	 * 
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 2-byte "words" to read
	 * @return array of register bytes; the result will have a length equal to
	 *         {@code count * 2}
	 */
	byte[] readBytes(final Integer address, final int count);

	/**
	 * Read a set of "input" type registers and interpret as a string.
	 * 
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @param trim
	 *        if <em>true</em> then remove leading/trailing whitespace from the
	 *        resulting string
	 * @param charsetName
	 *        the character set to interpret the bytes as
	 * @return String from interpreting raw bytes as a string
	 * @see #readBytes(Integer, int, int)
	 */
	String readString(final Integer address, final int count, final boolean trim,
			final String charsetName);

	/**
	 * Get the values of specific registers as an array of unsigned integers.
	 * This uses a Modbus function code {@code 3} request.
	 * 
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 */
	int[] readInts(final Integer address, final int count);

	/**
	 * Get the values of specific registers as an array of signed shorts. This
	 * uses a Modbus function code {@code 3} request.
	 * 
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 */
	short[] readSignedShorts(final Integer address, final int count);

	/**
	 * Get the values of specific registers as an array. This uses a Modbus
	 * function code {@code 3} request.
	 * 
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus "words" to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 */
	Integer[] readValues(final Integer address, final int count);

}
