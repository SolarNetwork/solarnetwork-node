/* ==================================================================
 * ModbusDataType.java - 20/12/2017 1:59:32 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

/**
 * An enumeration of common Modbus data types.
 * 
 * @author matt
 * @version 1.1
 * @since 2.5
 */
public enum ModbusDataType {

	/** Boolean bit. */
	Boolean("bit", 1),

	/**
	 * 16-bit floating point.
	 * 
	 * @since 1.1
	 */
	Float16("f16", 1),

	/** 32-bit floating point. */
	Float32("f32", 2),

	/** 64-bit floating point. */
	Float64("f64", 4),

	/** Signed 16-bit integer. */
	Int16("i16", 1),

	/** Unsigned 16-bit integer. */
	UInt16("u16", 1),

	/** Signed 32-bit integer. */
	Int32("i32", 2),

	/** Unsigned 32-bit integer. */
	UInt32("u32", 2),

	/** Signed 64-bit integer. */
	Int64("i64", 4),

	/** Unsigned 64-bit integer. */
	UInt64("u64", 4),

	/** Raw bytes. */
	Bytes("b", -1),

	/** Bytes interpreted as a UTF-8 encoded string. */
	StringUtf8("s", -1),

	/** Bytes interpreted as an ASCII encoded string. */
	StringAscii("a", -1);

	final private String key;
	final private int wordLength;

	private ModbusDataType(String key, int wordLength) {
		this.key = key;
		this.wordLength = wordLength;
	}

	/**
	 * Get the key value for this enum.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the number of Modbus words (16-bit register values) this data type
	 * requires.
	 * 
	 * @return the number of words, or {@literal -1} for an unknown length (for
	 *         example for strings)
	 */
	public int getWordLength() {
		return wordLength;
	}

	/**
	 * Get an enum instance for a key value.
	 * 
	 * @param key
	 *        the key
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not a valid value
	 */
	public static ModbusDataType forKey(String key) {
		for ( ModbusDataType e : ModbusDataType.values() ) {
			if ( key.equals(e.key) ) {
				return e;
			}
		}

		throw new IllegalArgumentException("Unknown ModbusDataType key [" + key + "]");
	}

	/**
	 * Get a friendly display string for this data type.
	 * 
	 * @return the display string
	 */
	public String toDisplayString() {
		switch (this) {
			case Boolean:
				return "Bit (on/off)";

			case Bytes:
				return "Bytes, 8-bit (two per register)";

			case Float16:
				return "16-bit floating point (1 register)";

			case Float32:
				return "32-bit floating point (2 registers)";

			case Float64:
				return "64-bit floating point (4 registers)";

			case Int16:
				return "16-bit signed integer (1 register)";

			case Int32:
				return "32-bit signed integer (2 registers)";

			case Int64:
				return "64-bit signed integer (4 registers)";

			case StringAscii:
				return "String (ASCII)";

			case StringUtf8:
				return "String (UTF-8)";

			case UInt16:
				return "16-bit unsigned integer (1 register)";

			case UInt32:
				return "32-bit unsigned integer (2 registers)";

			case UInt64:
				return "64-bit unsigned integer (4 registers)";

			default:
				return this.toString();

		}
	}

}
