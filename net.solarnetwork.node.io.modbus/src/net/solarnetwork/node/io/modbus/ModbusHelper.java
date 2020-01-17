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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for working with Modbus serial connection.
 * 
 * @author matt
 * @version 2.0
 */
public final class ModbusHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ModbusHelper.class);

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
			result = ModbusDataUtils.parseFloat32(data[offset], data[offset + 1]);
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
			result = ModbusDataUtils.parseFloat64(data[offset], data[offset + 1], data[offset + 2],
					data[offset + 3]);
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
			result = ModbusDataUtils.parseFloat64(data[0], data[1], data[2], data[3]);
		}
		return result;
	}

	/**
	 * Parse a signed 64-bit long value from raw Modbus register values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of {@code 4}, and be
	 * arranged in big-endian order.
	 * </p>
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed long
	 */
	public static Long parseInt64(final Integer[] data) {
		Long result = null;
		if ( data != null && data.length == 4 ) {
			result = ModbusDataUtils.parseInt64(data[0], data[1], data[2], data[3]);
		}
		return result;
	}

	/**
	 * Parse an unsigned 32-bit long value from raw Modbus register values.
	 * 
	 * <p>
	 * The {@code data} array is expected to have a length of at least
	 * {@code offset} + {@code 1}, and be arranged in big-endian order.
	 * </p>
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
			result = ModbusDataUtils.parseUnsignedInt32(data[offset], data[offset + 1]);
		}
		return result;
	}

	/**
	 * Get a {@link ModbusFunction} for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the function
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 * @since 1.5
	 */
	public static ModbusFunction functionForCode(int code) {
		ModbusFunction f;
		try {
			f = ModbusReadFunction.forCode(code);
		} catch ( IllegalArgumentException e ) {
			try {
				f = ModbusWriteFunction.forCode(code);
			} catch ( IllegalArgumentException e2 ) {
				throw new IllegalArgumentException("Unknown Modbus function code: " + code);
			}
		}
		return f;
	}

}
