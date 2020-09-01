/* ==================================================================
 * Register.java - 31/08/2020 4:19:06 PM
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

package net.solarnetwork.node.hw.ams.lux.tsl2591;

import net.solarnetwork.domain.CodedValue;

/**
 * I2C registers.
 * 
 * @author matt
 * @version 1.0
 */
public enum Register implements CodedValue {

	Enable(0x00),

	Control(0x01),

	PackageId(0x11),

	DeviceId(0x12),

	Status(0x13),

	Channel0(0x14, 2),

	Channel1(0x16, 2),

	;

	/**
	 * Bitmask for a "normal" command.
	 */
	public static final int COMMAND_NORMAL = 0xA0;

	private final int code;
	private final int len;

	private Register(int code) {
		this(code, 1);
	}

	private Register(int code, int len) {
		this.code = code;
		this.len = len;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the number of 8-bit registers used by this value.
	 * 
	 * @return the number of registers
	 */
	public int getLength() {
		return len;
	}

	/**
	 * Get the command value.
	 * 
	 * @return the command value
	 */
	public int commandValue() {
		return COMMAND_NORMAL | code;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the status, never {@literal null} and set to {@link #Medium} if
	 *         not any other valid code
	 */
	public static Register forCode(int code) {
		final byte c = (byte) code;
		for ( Register v : values() ) {
			if ( v.code == c ) {
				return v;
			}
		}
		return Register.Enable;
	}
}
