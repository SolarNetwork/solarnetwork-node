/* ==================================================================
 * RegisterBlockType.java - 17/09/2020 4:07:04 PM
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

package net.solarnetwork.node.io.modbus.server.domain;

import net.solarnetwork.domain.CodedValue;

/**
 * Modbus register block types.
 * 
 * @author matt
 * @version 1.0
 */
public enum RegisterBlockType implements CodedValue {

	Coil(1, 1, false),

	Discrete(2, 1, true),

	Holding(3, 16, false),

	Input(4, 16, true),

	;

	private final int code;
	private final int bitCount;
	private final boolean readOnly;

	private RegisterBlockType(int code, int bitCount, boolean readOnly) {
		this.code = code;
		this.bitCount = bitCount;
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
	 * Get an enumeration instance for a code value.
	 * 
	 * @param code
	 *        the code value to get the enumeration fo
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@literal code} is not a valid value
	 */
	public static RegisterBlockType forCode(int code) {
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
						"RegisterBlockType code [" + code + "] not supported.");
		}
	}

}
