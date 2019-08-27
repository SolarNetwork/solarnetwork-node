/* ==================================================================
 * Stability30cAcControlMethod.java - 27/08/2019 4:11:12 pm
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
 * Enumeration of AC control methods.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stability30cAcControlMethod {

	Idle(0x0000, "Idle"),

	Net(0x0001, "Net"),

	GridPower(0x0402, "Grid power (GPWR)"),

	FacilityPower(0x0502, "Facility power (FPWR)");

	private final int code;
	private final String description;

	private Stability30cAcControlMethod(int code, String description) {
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
	public static Stability30cAcControlMethod forCode(int code) {
		for ( Stability30cAcControlMethod c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException(
				"Stability30cAcControlMethod code [" + code + "] not supported");
	}

}
