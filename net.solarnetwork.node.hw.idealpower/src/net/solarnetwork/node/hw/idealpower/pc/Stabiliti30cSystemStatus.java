/* ==================================================================
 * Stabiliti30cSystemStatus.java - 27/08/2019 3:14:19 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.idealpower.pc;

import net.solarnetwork.domain.Bitmaskable;

/**
 * Enumeration of system status states.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cSystemStatus implements Bitmaskable {

	SelfTest(0, "Self-test mode"),

	ReconnectTimer0(1, "Reconnect timer 0 countdown"),

	ReconnectTimer1(2, "Reconnect timer 1 countdown"),

	ReconnectTimer2(3, "Reconnect timer 2 countdown"),

	Precharge(4, "Precharge active"),

	BadControlMethod(5, "Bad control method"),

	Ac1RotationError(6, "AC1 rotation error"),

	MpptEnabledVoltage(7, "MPPT enabled based on voltage"),

	MpptEnabledTime(8, "MPPT enabled based on time"),

	PpsaConverting(9, "PPSA link converting power"),

	Shutdown(10, "Shutdown active"),

	Lockdown(11, "System lockdown"),

	Abort(12, "Abort active"),

	GfdiFault(15, "GFDI fault");

	private final int code;
	private final String description;

	private Stabiliti30cSystemStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get the code for this condition.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get a description.
	 * 
	 * @return the description
	 */
	public final String getDescription() {
		return description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	/**
	 * Get an enum for a code value.
	 * 
	 * @param code
	 *        the code to get an enum for
	 * @return the enum with the given {@code code}, or {@literal null} if
	 *         {@code code} is {@literal 0}
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static Stabiliti30cSystemStatus forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( Stabiliti30cSystemStatus c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("Stabiliti30cSystemStatus code [" + code + "] not supported");
	}

}
