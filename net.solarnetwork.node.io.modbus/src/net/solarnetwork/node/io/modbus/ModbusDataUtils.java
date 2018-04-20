/* ==================================================================
 * ModbusDataUtils.java - 10/04/2018 2:02:53 PM
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

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utilities for converting to/from Modbus 16-bit register values.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public final class ModbusDataUtils {

	/** The UTF-8 character set name. */
	public static final String UTF8_CHARSET = ModbusTransactionUtils.UTF8_CHARSET;

	/** The ASCII character set name. */
	public static final String ASCII_CHARSET = ModbusTransactionUtils.ASCII_CHARSET;

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

	/**
	 * Encode a number into raw Modbus register values.
	 * 
	 * <p>
	 * This method always returns a value, even if {@code number} is
	 * {@literal null} or {@code dataType} is not a supported type. In the event
	 * of any error, {@li
	 * 
	 * @param dataType
	 *        the desired Modbus data type
	 * @param number
	 *        the number to encode
	 * @return the encoded register values
	 * @throws IllegalArgumentException
	 *         if {@code dataType} is not supported
	 */
	public static int[] encodeNumber(ModbusDataType dataType, Number number) {
		int[] result = null;
		switch (dataType) {
			case Boolean:
				result = new int[] { (number != null && number.intValue() != 0 ? 1 : 0) };
				break;

			case Float32:
				result = encodeFloat32(number != null ? number.floatValue() : 0f);
				break;

			case Float64:
				result = encodeFloat64(number != null ? number.doubleValue() : 0.0);
				break;

			case Int16:
				result = encodeInt16(number != null ? number.shortValue() : (short) 0);
				break;

			case UInt16:
				result = encodeUnsignedInt16(number != null ? number.intValue() : 0);
				break;

			case Int32:
				result = encodeInt32(number != null ? number.intValue() : 0);
				break;

			case UInt32:
				result = encodeUnsignedInt32(number != null ? number.longValue() : 0L);
				break;

			case Int64:
				result = encodeInt64(number != null ? number.longValue() : 0L);
				break;

			case UInt64:
				try {
					result = encodeUnsignedInt64(number instanceof BigInteger ? (BigInteger) number
							: number != null ? new BigInteger(number.toString()) : BigInteger.ZERO);
				} catch ( NumberFormatException e ) {
					result = encodeInt64(0L);
				}
				break;

			case Bytes:
				try {
					result = encodeUnsignedInteger(number instanceof BigInteger ? (BigInteger) number
							: number != null ? new BigInteger(number.toString()) : BigInteger.ZERO);
				} catch ( NumberFormatException e ) {
					result = new int[] { 0 };
				}
				break;

			default:
				throw new IllegalArgumentException(
						"Data type " + dataType + " cannot be converted into a number");

		}
		return result;
	}

	/**
	 * Encode a 16-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 1}
	 */
	public static int[] encodeInt16(Short value) {
		short bits = (value != null ? value.shortValue() : (short) 0);
		return new int[] { bits };
	}

	/**
	 * Encode a 16-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 1}
	 */
	public static int[] encodeUnsignedInt16(Integer value) {
		int bits = (value != null ? value : 0);
		return new int[] { (bits & 0xFFFF) };
	}

	/**
	 * Encode a 32-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 2}
	 */
	public static int[] encodeInt32(Integer value) {
		int bits = (value != null ? value : 0);
		return new int[] { (bits >> 16) & 0xFFFF, bits & 0xFFFF };
	}

	/**
	 * Encode a 32-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 2}
	 */
	public static int[] encodeUnsignedInt32(Long value) {
		int[] words = encodeInt64(value);
		return new int[] { words[2], words[3] };
	}

	/**
	 * Encode a 64-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 4}
	 */
	public static int[] encodeInt64(Long value) {
		long bits = (value != null ? value : 0);
		return new int[] { (int) ((bits >> 48) & 0xFFFF), (int) ((bits >> 32) & 0xFFFF),
				(int) ((bits >> 16) & 0xFFFF), (int) (bits & 0xFFFF) };
	}

	/**
	 * Encode an 64-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the integer to encode
	 * @return the register values, which will have a length of {@literal 4}
	 */
	public static int[] encodeUnsignedInt64(BigInteger value) {
		byte[] bytes = value.toByteArray();

		// drop sign byte, if present and not already an even number of bytes
		if ( bytes[0] == 0 && bytes.length % 2 == 1 && bytes.length < 9 ) {
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
		}

		// we can only use up to 8 bytes
		if ( bytes.length > 8 ) {
			bytes = Arrays.copyOfRange(bytes, bytes.length - 8, bytes.length);
		}

		// ensure we have an even number of bytes
		if ( bytes.length % 2 == 1 ) {
			byte[] tmp = new byte[bytes.length + 1];
			System.arraycopy(bytes, 0, tmp, 1, bytes.length);
			bytes = tmp;
		}

		int[] unsigned = new int[4];
		int offset = (8 - bytes.length) / 2;
		for ( int i = 0; i < bytes.length; i += 2 ) {
			int v = ((bytes[i] & 0xFF) << 8);
			if ( i + 1 < bytes.length ) {
				v |= (bytes[i + 1] & 0xFF);
			}
			unsigned[offset + i / 2] = v;
		}

		return unsigned;
	}

	/**
	 * Encode an unsigned integer value into raw Modbus unsigned short register
	 * values.
	 * 
	 * @param value
	 *        the integer to encode
	 * @return the register values, which will have a length equal to the number
	 *         of registers required to store the full value
	 */
	public static int[] encodeUnsignedInteger(BigInteger value) {
		byte[] bytes = value.toByteArray();

		// drop sign byte, if present and not already an even number of bytes
		if ( bytes[0] == 0 && bytes.length % 2 == 1 ) {
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
		}

		// ensure we have an even number of bytes
		if ( bytes.length % 2 == 1 ) {
			byte[] tmp = new byte[bytes.length + 1];
			System.arraycopy(bytes, 0, tmp, 1, bytes.length);
			bytes = tmp;
		}

		int[] unsigned = new int[bytes.length / 2];
		for ( int i = 0; i < bytes.length; i += 2 ) {
			int v = ((bytes[i] & 0xFF) << 8);
			if ( i + 1 < bytes.length ) {
				v |= (bytes[i + 1] & 0xFF);
			}
			unsigned[i / 2] = v;
		}

		return unsigned;
	}

	/**
	 * Encode an IEEE-754 32-bit float value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the float to encode
	 * @return the register values, which will have a length of {@literal 2}
	 */
	public static int[] encodeFloat32(Float value) {
		int bits = Float.floatToIntBits(value != null ? value : 0f);
		return encodeInt32(bits);
	}

	/**
	 * Encode an IEEE-754 32-bit float value into raw Modbus unsigned short
	 * register values.
	 * 
	 * @param value
	 *        the float to encode
	 * @return the register values, which will have a length of {@literal 4}
	 */
	public static int[] encodeFloat64(Double value) {
		long bits = Double.doubleToLongBits(value != null ? value : 0.0);
		return encodeInt64(bits);
	}

	/**
	 * Encode an array of bytes into 16-bit raw Modbus register values.
	 * 
	 * <p>
	 * Each register value will hold up to two bytes.
	 * </p>
	 * 
	 * @param data
	 *        the data to encode
	 * @return the register values, which will have a length of
	 *         {@code data.length / 2}
	 */
	public static int[] encodeBytes(byte[] data) {
		if ( data == null || data.length < 1 ) {
			return new int[0];
		}
		int[] result = new int[(int) Math.ceil(data.length / 2.0)];
		for ( int i = 0, p = 0; i < data.length; i += 2, p += 1 ) {
			int n = ((data[i] & 0xFF) << 8);
			if ( i + 1 < data.length ) {
				n = n | (data[i + 1] & 0xFF);
			}
			result[p] = n;
		}
		return result;
	}

	/**
	 * Encode a number into raw Modbus register values.
	 * 
	 * <p>
	 * This method always returns a value, even if {@code number} is
	 * {@literal null} or {@code dataType} is not a supported type. In the event
	 * of any error, {@li
	 * 
	 * @param dataType
	 *        the desired Modbus data type
	 * @param words
	 *        an array of Modbus register values
	 * @param offset
	 *        an offset within {@code words} to start reading from
	 * @return the parsed number
	 * @throws IllegalArgumentException
	 *         if {@code dataType} is not supported
	 */
	public static Number parseNumber(ModbusDataType dataType, int[] words, int offset) {
		Number result = null;
		switch (dataType) {
			case Boolean:
				if ( offset < words.length ) {
					result = words[offset] == 0 ? 0 : 1;
				}
				break;

			case Float32:
				if ( offset + 1 < words.length ) {
					result = parseFloat32(words[offset], words[offset + 1]);
				}
				break;

			case Float64:
				if ( offset + 3 < words.length ) {
					result = parseFloat64(words[offset], words[offset + 1], words[offset + 2],
							words[offset + 3]);
				}
				break;

			case Int16:
				if ( offset < words.length ) {
					result = parseInt16(words[offset]);
				}
				break;

			case UInt16:
				if ( offset < words.length ) {
					result = parseUnsignedInt16(words[offset]);
				}
				break;

			case Int32:
				if ( offset + 1 < words.length ) {
					result = parseInt32(words[offset], words[offset + 1]);
				}
				break;

			case UInt32:
				if ( offset + 1 < words.length ) {
					result = parseUnsignedInt32(words[offset], words[offset + 1]);
				}
				break;

			case Int64:
				if ( offset + 3 < words.length ) {
					result = parseInt64(words[offset], words[offset + 1], words[offset + 2],
							words[offset + 3]);
				}
				break;

			case UInt64:
				if ( offset + 3 < words.length ) {
					result = parseUnsignedInt64(words[offset], words[offset + 1], words[offset + 2],
							words[offset + 3]);
				}
				break;

			case Bytes:
				result = parseUnsignedInteger(words, offset);
				break;

			default:
				throw new IllegalArgumentException(
						"Data type " + dataType + " cannot be converted into a number");

		}
		return result;
	}

	/**
	 * Parse a 16-bit signed integer value from a raw Modbus register value.
	 * 
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static Short parseInt16(final int lo) {
		return (short) lo;
	}

	/**
	 * Parse a 16-bit unsigned integer value from a raw Modbus register value.
	 * 
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static Integer parseUnsignedInt16(final int lo) {
		return (lo & 0xFFFF);
	}

	/**
	 * Parse a 32-bit signed integer value from raw Modbus register values.
	 * 
	 * @param hi
	 *        bits 31-16
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static Integer parseInt32(final int hi, final int lo) {
		return (((hi & 0xFFFF) << 16) | lo & 0xFFFF);
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
	 * @return the parsed integer, never {@literal null}
	 */
	public static Long parseUnsignedInt32(final int hi, final int lo) {
		return (((hi & 0xFFFFL) << 16) | lo & 0xFFFFL);
	}

	/**
	 * Parse a 64-bit signed integer value from raw Modbus register values.
	 * 
	 * @param h1
	 *        bits 63-48
	 * @param h2
	 *        bits 47-32
	 * @param l1
	 *        bits 31-16
	 * @param l2
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static Long parseInt64(final int h1, final int h2, final int l1, final int l2) {
		return (((h1 & 0xFFFFL) << 48) | ((h2 & 0xFFFFL) << 32) | ((l1 & 0xFFFFL) << 16)
				| (l2 & 0xFFFFL));
	}

	/**
	 * Construct an 64-bit unsigned integer from raw Modbus register values.
	 * 
	 * @param h1
	 *        bits 63-48
	 * @param h2
	 *        bits 47-32
	 * @param l1
	 *        bits 31-16
	 * @param l2
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static BigInteger parseUnsignedInt64(final int h1, final int h2, final int l1, final int l2) {
		int[] data = new int[] { h1, h2, l1, l2 };
		BigInteger r = new BigInteger("0");
		for ( int i = 0; i < 4; i++ ) {
			if ( i > 0 ) {
				r = r.shiftLeft(16);
			}
			r = r.add(new BigInteger(String.valueOf(data[i] & 0xFFFF)));
		}
		return r;
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
		Integer int32 = parseInt32(hi, lo);
		Float result = Float.intBitsToFloat(int32.intValue());
		if ( result.isNaN() ) {
			result = null;
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
	 * @return the parsed float, or {@literal null} if the result is {@code NaN}
	 */
	public static Double parseFloat64(final int h1, final int h2, final int l1, final int l2) {
		Long l = parseInt64(h1, h2, l1, l2);
		Double result = Double.longBitsToDouble(l);
		if ( result.isNaN() ) {
			result = null;
		}
		return result;
	}

	/**
	 * Parse any number of Modbus register values as a series of bytes.
	 * 
	 * @param words
	 *        the words to read as bytes
	 * @param offset
	 *        the word offset to start from
	 * @return the parsed bytes
	 */
	public static byte[] parseBytes(int[] words, int offset) {
		byte[] bytes = new byte[2
				* (words == null || offset >= words.length ? 0 : (words.length - offset))];
		if ( bytes.length > 0 ) {
			for ( int i = offset, p = 0; i < words.length; i++, p += 2 ) {
				bytes[p] = (byte) ((words[i] >> 8) & 0xFF);
				bytes[p + 1] = (byte) ((words[i] & 0xFF));
			}
		}
		return bytes;
	}

	/**
	 * Parse any number of Modbus register values as a series of bytes into a
	 * {@link BigInteger}.
	 * 
	 * @param words
	 *        the words to parse
	 * @param offset
	 *        the offset within words to start reading from
	 * @return the integer value
	 */
	public static BigInteger parseUnsignedInteger(int[] words, int offset) {
		byte[] bytes = parseBytes(words, offset);
		if ( bytes.length > 0 && bytes[0] != (byte) 0 ) {
			byte[] tmp = new byte[bytes.length + 1];
			System.arraycopy(bytes, 0, tmp, 1, bytes.length);
			bytes = tmp;
		}
		BigInteger result;
		try {
			result = new BigInteger(bytes);
		} catch ( NumberFormatException e ) {
			result = BigInteger.ZERO;
		}
		return result;
	}

}
