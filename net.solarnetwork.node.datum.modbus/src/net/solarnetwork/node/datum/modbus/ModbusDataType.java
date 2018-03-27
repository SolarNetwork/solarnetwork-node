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

package net.solarnetwork.node.datum.modbus;

/**
 * An enumeration of supported Modbus data types.
 * 
 * @author matt
 * @version 1.0
 * @deprecated use {@link net.solarnetwork.node.io.modbus.ModbusDataType}
 */
@Deprecated
public enum ModbusDataType {

	Boolean(1),

	Int16(1),

	SignedInt16(1),

	Int32(2),

	Int64(4),

	Float32(2),

	Float64(4),

	Bytes(-1),

	StringUtf8(-1),

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

	/**
	 * Convert this to the (new) standard {@code ModbusDataType}.
	 * 
	 * @return the converted data type
	 */
	public net.solarnetwork.node.io.modbus.ModbusDataType toModbusDataType() {
		switch (this) {
			case Boolean:
				return net.solarnetwork.node.io.modbus.ModbusDataType.Boolean;

			case Int16:
				return net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;

			case SignedInt16:
				return net.solarnetwork.node.io.modbus.ModbusDataType.Int16;

			case Int32:
				return net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;

			case Int64:
				return net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;

			case Float32:
				return net.solarnetwork.node.io.modbus.ModbusDataType.Float32;

			case Float64:
				return net.solarnetwork.node.io.modbus.ModbusDataType.Float64;

			case Bytes:
				return net.solarnetwork.node.io.modbus.ModbusDataType.Bytes;

			case StringUtf8:
				return net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;

			case StringAscii:
				return net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;

			default:
				throw new IllegalArgumentException("Unsupported type: " + this);
		}
	}

	/**
	 * Convert the (new) standard {@code ModbusDataType} to this enum.
	 * 
	 * @return the converted data type
	 */
	public ModbusDataType forModbusDataType(net.solarnetwork.node.io.modbus.ModbusDataType type) {
		switch (type) {
			case Boolean:
				return Boolean;

			case Bytes:
				return Bytes;

			case Float32:
				return Float32;

			case Float64:
				return Float64;

			case Int16:
				return SignedInt16;

			case UInt16:
				return Int16;

			case UInt32:
				return Int32;

			case UInt64:
				return Int64;

			case StringAscii:
				return StringAscii;

			case StringUtf8:
				return StringUtf8;

			default:
				throw new IllegalArgumentException("Unsupported type: " + type);
		}
	}

}
