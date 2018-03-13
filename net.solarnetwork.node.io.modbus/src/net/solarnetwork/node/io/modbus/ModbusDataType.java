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
 * @version 1.0
 * @since 2.5
 */
public enum ModbusDataType {

	/** Boolean bit. */
	Boolean(1),

	/** Unsigned 16-bit integer. */
	UInt16(1),

	/** Signed 16-bit integer. */
	Int16(1),

	/** Signed 32-bit integer. */
	Int32(2),

	/** Unsigned 32-bit integer. */
	UInt32(2),

	/** Signed 64-bit integer. */
	Int64(4),

	/** Unsigned 64-bit integer. */
	UInt64(4),

	/** 32-bit floating point. */
	Float32(2),

	/** 64-bit floating point. */
	Float64(4),

	/** Raw bytes. */
	Bytes(-1),

	/** Bytes interpreted as a UTF-8 encoded string. */
	StringUtf8(-1),

	/** Bytes interpreted as an ASCII encoded string. */
	StringAscii(-1);

	final private int wordLength;

	private ModbusDataType(int wordLength) {
		this.wordLength = wordLength;
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

}
