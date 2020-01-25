/* ==================================================================
 * SerialUtils.java - 25/10/2014 7:28:41 AM
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

package net.solarnetwork.node.io.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for working with serial IO.
 * 
 * @author matt
 * @version 2.0
 */
public final class SerialUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SerialUtils.class);

	private SerialUtils() {
		// can't create me
	}

	/**
	 * Parse a big-endian 32-bit float value from a data array.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset within the array to parse the value from
	 * @return the float, or <em>null</em> if not available
	 */
	public static Float parseBigEndianFloat32(Integer[] data, int offset) {
		Float result = null;
		if ( data != null && offset >= 0 && data.length > (offset + 1) ) {
			result = SerialUtils.parseBigEndianFloat32(new Integer[] { data[offset], data[offset + 1] });
		}
		return result;
	}

	/**
	 * Parse a 32-bit float value from raw Modbus register values. The
	 * {@code data} array is expected to have a length of at least
	 * {@code offset} + {@code 1}, and be arranged in big-endian order.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed float, or <em>null</em> if not available or parsed
	 *         float is {@code NaN}
	 */
	public static Float parseBigEndianFloat32(final int[] data, int offset) {
		Float result = null;
		if ( data != null && (offset + 1) < data.length ) {
			result = parseFloat32(data[0], data[1]);
		}
		return result;
	}

	/**
	 * Parse a 32-bit float value from raw integer values.
	 * 
	 * @param high
	 *        the high 16 bits
	 * @param low
	 *        the low 16 bits
	 * @return the parsed float, or <em>null</em> if not available or parsed
	 *         float is {@code NaN}
	 */
	public static Float parseFloat32(final int high, final int low) {
		Float result = Float.intBitsToFloat(((high & 0xFFFF) << 16) | (low & 0xFFFF));
		if ( result.isNaN() ) {
			LOG.trace("Data results in NaN: {} {}", high, low);
			result = null;
		}
		return result;
	}

	/**
	 * Parse a 32-bit float value from raw integer values. The {@code data}
	 * array is expected to have a length of {@code 2}, and be arranged in
	 * big-endian order.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed float, or <em>null</em> if not available or parsed
	 *         float is {@code NaN}
	 */
	public static Float parseBigEndianFloat32(final Integer[] data) {
		Float result = null;
		if ( data != null && data.length == 2 ) {
			result = Float.intBitsToFloat(
					((data[0].intValue() & 0xFFFF) << 16) | (data[1].intValue() & 0xFFFF));
			if ( result.isNaN() ) {
				LOG.trace("Data results in NaN: {}", (Object) data);
				result = null;
			}
		}
		return result;
	}

	/**
	 * Parse a big-endian 64-bit integer value from a data array.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset within the array to parse the value from
	 * @return the long, or <em>null</em> if not available
	 */
	public static Long parseBigEndianInt64(Integer[] data, int offset) {
		Long result = null;
		if ( data != null && offset >= 0 && data.length > (offset + 3) ) {
			result = SerialUtils.parseBigEndianInt64(new Integer[] { data[offset], data[offset + 1],
					data[offset + 2], data[offset + 3] });
		}
		return result;
	}

	/**
	 * Parse a 64-bit long value from raw integer values. The {@code data} array
	 * is expected to have a length of {@code 4}, and be arranged in big-endian
	 * order.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed long
	 */
	public static Long parseBigEndianInt64(final Integer[] data) {
		Long result = null;
		if ( data != null && data.length == 4 ) {
			result = parseInt64(data[0], data[1], data[2], data[3]);
		}
		return result;
	}

	/**
	 * Parse a 64-bit long value from raw integer values.
	 * 
	 * @param h1
	 *        bits 63-48
	 * @param h2
	 *        bits 47-32
	 * @param l1
	 *        bits 31-16
	 * @param l2
	 *        bits 15-0
	 * @return the parsed long
	 */
	public static Long parseInt64(final int h1, final int h2, final int l1, final int l2) {
		return ((((long) h1 & 0xFFFF) << 48) | (((long) h2 & 0xFFFF) << 32)
				| (((long) l1 & 0xFFFF) << 16) | ((long) l2 & 0xFFFF));
	}

	/**
	 * Parse a 32-bit long value from raw integer values. The {@code data} array
	 * is expected to have a length of at least {@code offset} + {@code 1}, and
	 * be arranged in big-endian order. <b>Note</b> a {@code Long} is returned
	 * to support unsigned 32-bit values.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset in the array to parse the 32-bit value
	 * @return the parsed long
	 */
	public static Long parseBigEndianInt32(final int[] data, int offset) {
		Long result = null;
		if ( data != null && (offset + 1) < data.length ) {
			result = ((long) ((data[offset] & 0xFFFF) << 16) | (long) (data[offset + 1] & 0xFFFF));
		}
		return result;
	}

}
