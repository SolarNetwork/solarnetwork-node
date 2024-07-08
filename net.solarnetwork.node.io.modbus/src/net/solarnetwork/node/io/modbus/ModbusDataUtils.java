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

import static net.solarnetwork.node.io.modbus.ModbusWordOrder.LeastToMostSignificant;
import static net.solarnetwork.node.io.modbus.ModbusWordOrder.MostToLeastSignificant;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import net.solarnetwork.util.Half;

/**
 * Utilities for converting to/from Modbus 16-bit register values.
 *
 * <p>
 * All Modbus register values are stored using {@code short} values, which in
 * Java are always treated as 16-bit signed integers.
 * </p>
 *
 * @author matt
 * @version 2.3
 * @since 2.6
 */
public final class ModbusDataUtils {

	private ModbusDataUtils() {
		// not available
	}

	/**
	 * Convert an array of shorts to Integer objects.
	 *
	 * @param array
	 *        the array to convert
	 * @return the converted array, or {@literal null} if {@code array} is
	 *         {@literal null}
	 */
	public static Integer[] integerArray(short[] array) {
		if ( array == null ) {
			return null;
		}
		final int count = array.length;
		final Integer[] result = new Integer[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = array[i] & 0xFFFF;
		}
		return result;
	}

	/**
	 * Convert an array of shorts to unsigned short int values.
	 *
	 * @param array
	 *        the array to convert
	 * @return the converted array, or {@literal null} if {@code array} is
	 *         {@literal null}
	 */
	public static int[] unsignedIntArray(short[] array) {
		if ( array == null ) {
			return null;
		}
		final int count = array.length;
		final int[] result = new int[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = array[i] & 0xFFFF;
		}
		return result;
	}

	/**
	 * Convert an array of ints to shorts.
	 *
	 * @param array
	 *        the array to convert
	 * @return the converted array, or {@literal null} if {@code array} is
	 *         {@literal null}
	 */
	public static short[] shortArray(int[] array) {
		if ( array == null ) {
			return null;
		}
		final int count = array.length;
		final short[] result = new short[count];
		for ( int i = 0; i < count; i++ ) {
			result[i] = (short) array[i];
		}
		return result;
	}

	/**
	 * Encode a number into raw Modbus register values.
	 *
	 * @param dataType
	 *        the desired Modbus data type
	 * @param number
	 *        the number to encode
	 * @return the encoded register values, in
	 *         {@link ModbusWordOrder#MostToLeastSignificant} word order
	 * @throws IllegalArgumentException
	 *         if {@code dataType} is not supported
	 * @see #encodeNumber(ModbusDataType, Number, ModbusWordOrder)
	 */
	public static short[] encodeNumber(ModbusDataType dataType, Number number) {
		return encodeNumber(dataType, number, MostToLeastSignificant);
	}

	/**
	 * Encode a number into raw Modbus register values.
	 *
	 * <p>
	 * This method always returns a value if {@code number} is {@literal null}.
	 * If {@code dataType} is not a supported type an,
	 * {@link IllegalArgumentException} will be thrown.
	 * </p>
	 *
	 * @param dataType
	 *        the desired Modbus data type
	 * @param number
	 *        the number to encode
	 * @param wordOrder
	 *        the desired word order
	 * @return the encoded register values
	 * @throws IllegalArgumentException
	 *         if {@code dataType} is not supported
	 * @since 1.1
	 */
	public static short[] encodeNumber(ModbusDataType dataType, Number number,
			ModbusWordOrder wordOrder) {
		short[] result = null;
		switch (dataType) {
			case Boolean:
				result = new short[] {
						(number != null && number.intValue() != 0 ? (short) 1 : (short) 0) };
				break;

			case Float16:
				result = new short[] { encodeFloat16(number instanceof Half ? (Half) number
						: number != null ? Half.valueOf(number.toString()) : null) };
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
					result = new short[] { 0 };
				}
				break;

			default:
				throw new IllegalArgumentException(
						"Data type " + dataType + " cannot be converted into a number");

		}
		if ( result != null && wordOrder == LeastToMostSignificant ) {
			swapWordOrder(result);
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
	public static short[] encodeInt16(Short value) {
		short bits = (value != null ? value.shortValue() : (short) 0);
		return new short[] { bits };
	}

	/**
	 * Encode a 16-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 1}
	 */
	public static short[] encodeUnsignedInt16(Integer value) {
		int bits = (value != null ? value : 0);
		return new short[] { (short) (bits & 0xFFFF) };
	}

	/**
	 * Encode a 32-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 2} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeInt32(Integer value) {
		return encodeInt32(value, MostToLeastSignificant);
	}

	/**
	 * Encode a 32-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of {@literal 2}
	 * @since 1.1
	 */
	public static short[] encodeInt32(Integer value, ModbusWordOrder wordOrder) {
		int bits = (value != null ? value : 0);
		short[] result = new short[] { (short) ((bits >> 16) & 0xFFFF), (short) (bits & 0xFFFF) };
		if ( wordOrder == LeastToMostSignificant ) {
			swapWordOrder(result);
		}
		return result;
	}

	/**
	 * Encode a 32-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 2} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeUnsignedInt32(Long value) {
		return encodeUnsignedInt32(value, MostToLeastSignificant);
	}

	/**
	 * Encode a 32-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of {@literal 2}
	 * @since 1.1
	 */
	public static short[] encodeUnsignedInt32(Long value, ModbusWordOrder wordOrder) {
		short[] words = encodeInt64(value, wordOrder);
		if ( wordOrder == MostToLeastSignificant ) {
			return new short[] { words[2], words[3] };
		}
		return new short[] { words[0], words[1] };
	}

	/**
	 * Encode a 64-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @return the register values, which will have a length of {@literal 4} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeInt64(Long value) {
		return encodeInt64(value, MostToLeastSignificant);
	}

	/**
	 * Encode a 64-bit signed integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the value to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of {@literal 4}
	 * @since 1.1
	 */
	public static short[] encodeInt64(Long value, ModbusWordOrder wordOrder) {
		long bits = (value != null ? value : 0);
		short[] result = new short[] { (short) ((bits >> 48) & 0xFFFF), (short) ((bits >> 32) & 0xFFFF),
				(short) ((bits >> 16) & 0xFFFF), (short) (bits & 0xFFFF) };
		if ( wordOrder == LeastToMostSignificant ) {
			swapWordOrder(result);
		}
		return result;
	}

	/**
	 * Encode an 64-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the integer to encode
	 * @return the register values, which will have a length of {@literal 4} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeUnsignedInt64(BigInteger value) {
		return encodeUnsignedInt64(value, MostToLeastSignificant);
	}

	/**
	 * Encode an 64-bit unsigned integer value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the integer to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of {@literal 4}
	 * @since 1.1
	 */
	public static short[] encodeUnsignedInt64(BigInteger value, ModbusWordOrder wordOrder) {
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

		short[] words = new short[4];
		int offset = (8 - bytes.length) / 2;
		for ( int i = 0; i < bytes.length; i += 2 ) {
			int v = ((bytes[i] & 0xFF) << 8);
			if ( i + 1 < bytes.length ) {
				v |= (bytes[i + 1] & 0xFF);
			}
			words[offset + i / 2] = (short) (v & 0xFFFF);
		}

		if ( wordOrder == LeastToMostSignificant ) {
			swapWordOrder(words);
		}

		return words;
	}

	/**
	 * Encode an unsigned integer value into raw Modbus unsigned short register
	 * values.
	 *
	 * @param value
	 *        the integer to encode
	 * @return the register values, which will have a length equal to the number
	 *         of registers required to store the full value and use
	 *         {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeUnsignedInteger(BigInteger value) {
		return encodeUnsignedInteger(value, MostToLeastSignificant);
	}

	/**
	 * Encode an unsigned integer value into raw Modbus unsigned short register
	 * values.
	 *
	 * @param value
	 *        the integer to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length equal to the number
	 *         of registers required to store the full value
	 * @since 1.1
	 */
	public static short[] encodeUnsignedInteger(BigInteger value, ModbusWordOrder wordOrder) {
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

		short[] words = new short[bytes.length / 2];
		for ( int i = 0; i < bytes.length; i += 2 ) {
			int v = ((bytes[i] & 0xFF) << 8);
			if ( i + 1 < bytes.length ) {
				v |= (bytes[i + 1] & 0xFF);
			}
			words[i / 2] = (short) v;
		}

		if ( wordOrder == LeastToMostSignificant ) {
			swapWordOrder(words);
		}

		return words;
	}

	/**
	 * Swap the order of an array of register values.
	 *
	 * <p>
	 * This essentially reverses the array. The array is modified in-place.
	 * </p>
	 *
	 * @param array
	 *        the data to swap
	 * @since 1.1
	 */
	public static void swapWordOrder(short[] array) {
		for ( int i = 0; i < array.length / 2; i++ ) {
			short temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	/**
	 * Encode an IEEE-754 16-bit float value into a raw Modbus unsigned short
	 * register value.
	 *
	 * @param value
	 *        the half to encode
	 * @return the register value
	 * @since 2.2
	 */
	public static short encodeFloat16(Half value) {
		return (value != null ? value.halfValue() : (short) 0);
	}

	/**
	 * Encode an IEEE-754 32-bit float value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the float to encode
	 * @return the register values, which will have a length of {@literal 2} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeFloat32(Float value) {
		return encodeFloat32(value, MostToLeastSignificant);
	}

	/**
	 * Encode an IEEE-754 32-bit float value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the float to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of {@literal 2}
	 * @since 1.1
	 */
	public static short[] encodeFloat32(Float value, ModbusWordOrder wordOrder) {
		int bits = Float.floatToIntBits(value != null ? value : 0f);
		return encodeInt32(bits, wordOrder);
	}

	/**
	 * Encode an IEEE-754 32-bit float value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the float to encode
	 * @return the register values, which will have a length of {@literal 4} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeFloat64(Double value) {
		return encodeFloat64(value, MostToLeastSignificant);
	}

	/**
	 * Encode an IEEE-754 32-bit float value into raw Modbus unsigned short
	 * register values.
	 *
	 * @param value
	 *        the float to encode
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of {@literal 4} and
	 *         use {@link ModbusWordOrder#MostToLeastSignificant} word order
	 * @since 1.1
	 */
	public static short[] encodeFloat64(Double value, ModbusWordOrder wordOrder) {
		long bits = Double.doubleToLongBits(value != null ? value : 0.0);
		return encodeInt64(bits, wordOrder);
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
	 *         {@code data.length / 2} and use
	 *         {@link ModbusWordOrder#MostToLeastSignificant} word order
	 */
	public static short[] encodeBytes(byte[] data) {
		return encodeBytes(data, MostToLeastSignificant);
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
	 * @param wordOrder
	 *        the resulting word order
	 * @return the register values, which will have a length of
	 *         {@code data.length / 2}
	 * @since 1.1
	 */
	public static short[] encodeBytes(byte[] data, ModbusWordOrder wordOrder) {
		if ( data == null || data.length < 1 ) {
			return new short[0];
		}
		short[] words = new short[(int) Math.ceil(data.length / 2.0)];
		for ( int i = 0, p = 0; i < data.length; i += 2, p += 1 ) {
			short n = (short) ((data[i] & 0xFF) << 8);
			if ( i + 1 < data.length ) {
				n = (short) (n | (data[i + 1] & 0xFF));
			}
			words[p] = n;
		}
		if ( wordOrder == LeastToMostSignificant ) {
			swapWordOrder(words);
		}
		return words;
	}

	/**
	 * Encode a number into raw Modbus register values.
	 *
	 * @param dataType
	 *        the desired Modbus data type
	 * @param words
	 *        an array of Modbus register values using
	 *        {@link ModbusWordOrder#MostToLeastSignificant} word order
	 * @param offset
	 *        an offset within {@code words} to start reading from
	 * @return the parsed number, or {@literal null} if {@code words} is
	 *         {@literal null} or not long enough for the requested data type
	 * @throws IllegalArgumentException
	 *         if {@code dataType} is not supported
	 */
	public static Number parseNumber(ModbusDataType dataType, short[] words, int offset) {
		return parseNumber(dataType, words, offset, MostToLeastSignificant);
	}

	/**
	 * Encode a number into raw Modbus register values.
	 *
	 * @param dataType
	 *        the desired Modbus data type
	 * @param words
	 *        an array of Modbus register values
	 * @param offset
	 *        an offset within {@code words} to start reading from
	 * @param wordOrder
	 *        the word order of {@code words}
	 * @return the parsed number, or {@literal null} if {@code words} is
	 *         {@literal null} or not long enough for the requested data type
	 * @throws IllegalArgumentException
	 *         if {@code dataType} is not supported
	 * @since 1.1
	 */
	public static Number parseNumber(ModbusDataType dataType, short[] words, int offset,
			ModbusWordOrder wordOrder) {
		Number result = null;
		switch (dataType) {
			case Boolean:
				if ( offset < words.length ) {
					result = words[offset] == 0 ? (byte) 0 : (byte) 1;
				}
				break;

			case Float16:
				if ( offset < words.length ) {
					result = parseFloat16(words[offset]);
				}
				break;

			case Float32:
				if ( offset + 1 < words.length ) {
					if ( wordOrder == MostToLeastSignificant ) {
						result = parseFloat32(words[offset], words[offset + 1]);
					} else {
						result = parseFloat32(words[offset + 1], words[offset]);
					}
				}
				break;

			case Float64:
				if ( offset + 3 < words.length ) {
					if ( wordOrder == MostToLeastSignificant ) {
						result = parseFloat64(words[offset], words[offset + 1], words[offset + 2],
								words[offset + 3]);
					} else {
						result = parseFloat64(words[offset + 3], words[offset + 2], words[offset + 1],
								words[offset]);
					}
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
					if ( wordOrder == MostToLeastSignificant ) {
						result = parseInt32(words[offset], words[offset + 1]);
					} else {
						result = parseInt32(words[offset + 1], words[offset]);
					}
				}
				break;

			case UInt32:
				if ( offset + 1 < words.length ) {
					if ( wordOrder == MostToLeastSignificant ) {
						result = parseUnsignedInt32(words[offset], words[offset + 1]);
					} else {
						result = parseUnsignedInt32(words[offset + 1], words[offset]);
					}
				}
				break;

			case Int64:
				if ( offset + 3 < words.length ) {
					if ( wordOrder == MostToLeastSignificant ) {
						result = parseInt64(words[offset], words[offset + 1], words[offset + 2],
								words[offset + 3]);
					} else {
						result = parseInt64(words[offset + 3], words[offset + 2], words[offset + 1],
								words[offset]);
					}
				}
				break;

			case UInt64:
				if ( offset + 3 < words.length ) {
					if ( wordOrder == MostToLeastSignificant ) {
						result = parseUnsignedInt64(words[offset], words[offset + 1], words[offset + 2],
								words[offset + 3]);
					} else {
						result = parseUnsignedInt64(words[offset + 3], words[offset + 2],
								words[offset + 1], words[offset]);
					}
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
	 * Convert a 16-bit signed integer value from a raw Modbus register value.
	 *
	 * <p>
	 * This method simply returns the given argument, but provided for
	 * completeness.
	 * </p>
	 *
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer
	 * @since 2.1
	 */
	public static short toInt16(final short lo) {
		return lo;
	}

	/**
	 * Parse a 16-bit signed integer value from a raw Modbus register value.
	 *
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static Short parseInt16(final short lo) {
		return lo;
	}

	/**
	 * Convert a 16-bit unsigned integer value from a raw Modbus register value.
	 *
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer
	 * @since 2.1
	 */
	public static int toUnsignedInt16(final short lo) {
		return (lo & 0xFFFF);
	}

	/**
	 * Parse a 16-bit unsigned integer value from a raw Modbus register value.
	 *
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer, never {@literal null}
	 */
	public static Integer parseUnsignedInt16(final short lo) {
		return toUnsignedInt16(lo);
	}

	/**
	 * Parse a 32-bit signed integer value from raw Modbus register values.
	 *
	 * @param hi
	 *        bits 31-16
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer
	 * @since 2.1
	 */
	public static int toInt32(final short hi, final short lo) {
		return (((hi & 0xFFFF) << 16) | lo & 0xFFFF);
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
	public static Integer parseInt32(final short hi, final short lo) {
		return toInt32(hi, lo);
	}

	/**
	 * Parse a 32-bit unsigned integer value from raw Modbus register values.
	 *
	 * <p>
	 * <b>Note</b> a {@code long} is returned to support unsigned 32-bit values.
	 * </p>
	 *
	 * @param hi
	 *        bits 31-16
	 * @param lo
	 *        bits 15-0
	 * @return the parsed integer
	 * @since 2.1
	 */
	public static long toUnsignedInt32(final short hi, final short lo) {
		return (((hi & 0xFFFFL) << 16) | lo & 0xFFFFL);
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
	public static Long parseUnsignedInt32(final short hi, final short lo) {
		return toUnsignedInt32(hi, lo);
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
	 * @return the parsed integer
	 * @since 2.1
	 */
	public static long toInt64(final short h1, final short h2, final short l1, final short l2) {
		return (((h1 & 0xFFFFL) << 48) | ((h2 & 0xFFFFL) << 32) | ((l1 & 0xFFFFL) << 16)
				| (l2 & 0xFFFFL));
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
	public static Long parseInt64(final short h1, final short h2, final short l1, final short l2) {
		return toInt64(h1, h2, l1, l2);
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
	public static BigInteger parseUnsignedInt64(final short h1, final short h2, final short l1,
			final short l2) {
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
	 * Parse an IEEE-754 16-bit float value from raw Modbus register values.
	 *
	 * @param val
	 *        the 16 bits
	 * @return the parsed half, or {@literal null} if not available or parsed
	 *         half is {@code NaN}
	 * @since 2.2
	 */
	public static Half parseFloat16(final short val) {
		Half result = Half.valueOf(val);
		if ( result.isNaN() ) {
			result = null;
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
	 * @return the parsed float
	 * @since 2.1
	 */
	public static float toFloat32(final short hi, final short lo) {
		int int32 = toInt32(hi, lo);
		return Float.intBitsToFloat(int32);
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
	public static Float parseFloat32(final short hi, final short lo) {
		Float result = toFloat32(hi, lo);
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
	 * @return the parsed double
	 * @since 2.1
	 */
	public static double toFloat64(final short h1, final short h2, final short l1, final short l2) {
		long l = toInt64(h1, h2, l1, l2);
		return Double.longBitsToDouble(l);
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
	 * @return the parsed double, or {@literal null} if the result is
	 *         {@code NaN}
	 */
	public static Double parseFloat64(final short h1, final short h2, final short l1, final short l2) {
		Double result = toFloat64(h1, h2, l1, l2);
		if ( result.isNaN() ) {
			result = null;
		}
		return result;
	}

	/**
	 * Parse any number of Modbus register values as a series of bytes.
	 *
	 * @param words
	 *        the words to read as bytes, in
	 *        {@link ModbusWordOrder#MostToLeastSignificant} word order
	 * @param offset
	 *        the word offset to start from
	 * @return the parsed bytes
	 */
	public static byte[] parseBytes(short[] words, int offset) {
		return parseBytes(words, offset, MostToLeastSignificant);
	}

	/**
	 * Parse any number of Modbus register values as a series of bytes.
	 *
	 * @param words
	 *        the words to read as bytes
	 * @param offset
	 *        the word offset to start from
	 * @param wordOrder
	 *        the word order of {@code words}
	 * @return the parsed bytes
	 * @since 1.1
	 */
	public static byte[] parseBytes(short[] words, int offset, ModbusWordOrder wordOrder) {
		byte[] bytes = new byte[2
				* (words == null || offset >= words.length ? 0 : (words.length - offset))];
		if ( bytes.length > 0 ) {
			for ( int i = offset, p = 0; i < words.length; i++, p += 2 ) {
				int idx = (wordOrder == MostToLeastSignificant ? p : bytes.length - p - 2);
				bytes[idx] = (byte) ((words[i] >> 8) & 0xFF);
				bytes[idx + 1] = (byte) ((words[i] & 0xFF));
			}
		}
		return bytes;
	}

	/**
	 * Parse any number of Modbus register values as a series of bytes into a
	 * {@link BigInteger}.
	 *
	 * @param words
	 *        the words to parse, in
	 *        {@link ModbusWordOrder#MostToLeastSignificant} word order
	 * @param offset
	 *        the offset within words to start reading from
	 * @return the integer value
	 */
	public static BigInteger parseUnsignedInteger(short[] words, int offset) {
		return parseUnsignedInteger(words, offset, MostToLeastSignificant);
	}

	/**
	 * Parse any number of Modbus register values as a series of bytes into a
	 * {@link BigInteger}.
	 *
	 * @param words
	 *        the words to parse
	 * @param offset
	 *        the offset within words to start reading from
	 * @param wordOrder
	 *        the word order of {@code words}
	 * @return the integer value
	 * @since 1.1
	 */
	public static BigInteger parseUnsignedInteger(short[] words, int offset, ModbusWordOrder wordOrder) {
		byte[] bytes = parseBytes(words, offset, wordOrder);
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

	/**
	 * Return the minimum number of 16-bit register words that can accommodate
	 * the active bits in a bit set.
	 *
	 * <p>
	 * If no bits are set, the returned value is {@literal 0}.
	 * </p>
	 *
	 * @param bits
	 *        the bit set
	 * @return the minimum number of words
	 * @since 2.3
	 */
	public static int wordSize(BitSet bits) {
		final int len = bits.length();
		return (len / 16) + (len % 16 > 0 ? 1 : 0);
	}

	/**
	 * Convert a bit set into an array of 16-bit register values.
	 *
	 * @param bits
	 *        the bit set
	 * @param count
	 *        the number of words to fill
	 * @param wordOrder
	 *        the word order for the result
	 * @return the words, never {@literal null}
	 * @since 2.3
	 */
	public static short[] shortArrayForBitSet(BitSet bits, int count, ModbusWordOrder wordOrder) {
		short[] result = new short[count];
		for ( int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1) ) {
			int pos = (wordOrder == ModbusWordOrder.LeastToMostSignificant ? i / 16
					: count - (i / 16) - 1);
			int bitPos = i % 16;
			if ( bits.get(i) ) {
				result[pos] |= 1 << bitPos;
			}
			if ( i == Integer.MAX_VALUE ) {
				break;
			}
		}
		return result;
	}

	/**
	 * Convert a bit set into an array of 16-bit register values, using
	 * {@link ModbusWordOrder#MostToLeastSignificant} word order.
	 *
	 * @param bits
	 *        the bit set
	 * @param count
	 *        the number of words to fill
	 * @return the words, never {@literal null}
	 * @since 2.3
	 */
	public static short[] shortArrayForBitSet(BitSet bits, int count) {
		return shortArrayForBitSet(bits, count, MostToLeastSignificant);
	}

	/**
	 * Convert an array of 16-bit register values into a bit set.
	 *
	 * @param words
	 *        the words
	 * @param wordOrder
	 *        the word order for the result
	 * @return the bit set, never {@literal null}
	 * @since 2.3
	 */
	public static BitSet bitSetForShortArray(short[] words, ModbusWordOrder wordOrder) {
		final int size = (words != null ? words.length : 0);
		final BitSet result = new BitSet(size);
		for ( int pos = 0; pos < words.length; pos++ ) {
			short w = words[pos];
			for ( int i = 0; i < 16; i++ ) {
				int bitPos = ((wordOrder == LeastToMostSignificant ? pos * 16 : (size - pos - 1) * 16)
						+ (i % 16));
				int bit = (w >> i) & 1;
				result.set(bitPos, bit == 1);
			}
		}
		return result;
	}

	/**
	 * Convert an array of 16-bit register values into a bit set, using
	 * {@link ModbusWordOrder#MostToLeastSignificant} word order.
	 *
	 * @param words
	 *        the words
	 * @return the bit set, never {@literal null}
	 * @since 2.3
	 */
	public static BitSet bitSetForShortArray(short[] words) {
		return bitSetForShortArray(words, MostToLeastSignificant);
	}

}
