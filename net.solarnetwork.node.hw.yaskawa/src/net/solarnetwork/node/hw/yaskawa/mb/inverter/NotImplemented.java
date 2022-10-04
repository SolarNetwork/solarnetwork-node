/* ==================================================================
 * NotImplemented.java - 21/08/2018 7:35:20 AM
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

package net.solarnetwork.node.hw.yaskawa.mb.inverter;

import net.solarnetwork.node.io.modbus.ModbusDataType;

/**
 * Constants for "not implemented" in various Modbus data types.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public enum NotImplemented {

	/** Int16. */
	Int16(0x800),

	/** UInt16. */
	UInt16(0xFFFF),

	/** Int32. */
	Int32(0x8000000),

	/** UInt32. */
	UInt32(0xFFFFFFFF),

	/** String. */
	String(0x00),

	;

	private final int value;

	private NotImplemented(int value) {
		this.value = value;
	}

	/**
	 * Test if a value is a "not implemented" constant for a given data type.
	 * 
	 * @param dataType
	 *        the data type of {@code value}
	 * @param value
	 *        the data value
	 * @return {@literal true} if {@code value} is equal to the "not
	 *         implemented" constant for the given data type
	 */
	public boolean isNotImplemented(ModbusDataType dataType, Object value) {
		if ( value == null ) {
			return false;
		}
		if ( value instanceof Number ) {
			int v = ((Number) value).intValue();
			switch (dataType) {
				case Int16:
					return Int16.value == v;

				case UInt16:
					return UInt16.value == v;

				case Int32:
					return Int32.value == v;

				case UInt32:
					return UInt32.value == v;

				default:
					return false;
			}
		} else if ( (dataType == ModbusDataType.StringAscii || dataType == ModbusDataType.StringUtf8)
				&& value instanceof String && ((String) value).length() > 0
				&& ((String) value).charAt(0) == String.value ) {
			return true;
		}
		return false;
	}

	/**
	 * Get the "not implemented" constant value.
	 * 
	 * @return the value, as an int
	 */
	public int getValue() {
		return value;
	}

}
