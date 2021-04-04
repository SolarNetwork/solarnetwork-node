/* ==================================================================
 * Stabiliti30cFaultSeverity.java - 28/08/2019 10:32:47 am
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

/**
 * Enumeration of fault severity values.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cFaultSeverity {

	Info(0b000, "Info: increments fault counter only"),

	Alert(0b001, "Alert: increments fault counter only"),

	Alarm(0b010, "Alarm: fault logged"),

	Abort0(0b011, "Abort 0: fault logged; unit stopped; restart via reconnect timer 0"),

	Abort1(0b100, "Abort 1: fault logged; unit stopped; restart via reconnect timer 1"),

	Abort2(0b101, "Abort 2: fault logged; unit stopped; restart via reconnect timer 2"),

	Lockdown(0b110, "Lockdown: fault logged; unit stops processing power; requires reset");

	private final int code;
	private final String description;

	private Stabiliti30cFaultSeverity(int code, String description) {
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

	/**
	 * Get an enum for a code value.
	 * 
	 * @param code
	 *        the code to get an enum for
	 * @return the enum with the given {@code code}
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static Stabiliti30cFaultSeverity forCode(int code) {
		for ( Stabiliti30cFaultSeverity c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException(
				"Stabiliti30cFaultSeverity code [" + code + "] not supported");
	}

}
