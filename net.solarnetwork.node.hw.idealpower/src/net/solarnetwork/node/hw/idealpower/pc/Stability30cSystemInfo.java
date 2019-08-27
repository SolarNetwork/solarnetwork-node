/* ==================================================================
 * Stability30cSystemInfo.java - 27/08/2019 3:14:19 pm
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
 * Enumeration of system info states.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stability30cSystemInfo implements Bitmaskable {

	Fault(0, "Fault occurred since last reset"),

	ArcFault(1, "Arc Fault Alarm"),

	GfdiFault(2, "GFDI faulting"),

	SelfTest(3, "Self-test mode"),

	SelfTestFault(4, "Self-test failure"),

	Shutdown(5, "Shutdown"),

	PllLocked(6, "PLL Locked"),

	SystemFault(7, "System faulting"),

	ImiFault(8, "IMI faulting (floating DC)"),

	Reconnecting(9, "System reconnecting"),

	PpsaConverting(10, "PPSA link converting power"),

	GridContactorFault(11, "Grid contactor Ack Fault"),

	Abort(12, "Abort active"),

	Lockdown(13, "Lockdown active"),

	EmergencyStop(14, "Emergency stop shutdown"),

	Undervoltage(15, "Undervoltage shutdown");

	private final int code;
	private final String description;

	private Stability30cSystemInfo(int code, String description) {
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
	public static Stability30cSystemInfo forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( Stability30cSystemInfo c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("Stability30cSystemInfo code [" + code + "] not supported");
	}

}
