/* ==================================================================
 * ModbusRegisterBlockType.java - 17/09/2020 4:07:04 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import net.solarnetwork.domain.CodedValue;

/**
 * Modbus register block types.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public enum ModbusRegisterBlockType implements CodedValue {

	/** Coil (toggle) type. */
	Coil(1, 1, true, false),

	/** Discrete (input) type. */
	Discrete(2, 1, true, true),

	/** Holding (output) type. */
	Holding(3, 16, false, false),

	/** Input type. */
	Input(4, 16, false, true),

	;

	private final int code;
	private final int bitCount;
	private final boolean bitType;
	private final boolean readOnly;

	private ModbusRegisterBlockType(int code, int bitCount, boolean bitType, boolean readOnly) {
		this.code = code;
		this.bitCount = bitCount;
		this.bitType = bitType;
		this.readOnly = readOnly;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the number of bits registers of this type use.
	 * 
	 * @return the bit count
	 */
	public int getBitCount() {
		return bitCount;
	}

	/**
	 * Get the read-only flag.
	 * 
	 * @return {@literal true} if registers of this type are read-only
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Get the "bit type-ness" of this register block type.
	 * 
	 * @return {@literal true} if this is a coil or discrete register block
	 */
	public boolean isBitType() {
		return bitType;
	}

	/**
	 * Get an enumeration instance for a code value.
	 * 
	 * @param code
	 *        the code value to get the enumeration for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@literal code} is not a valid value
	 */
	public static ModbusRegisterBlockType forCode(int code) {
		switch (code) {
			case 1:
				return Coil;

			case 2:
				return Discrete;

			case 3:
				return Holding;

			case 4:
				return Input;

			default:
				throw new IllegalArgumentException(
						"ModbusRegisterBlockType code [" + code + "] not supported.");
		}
	}

}
