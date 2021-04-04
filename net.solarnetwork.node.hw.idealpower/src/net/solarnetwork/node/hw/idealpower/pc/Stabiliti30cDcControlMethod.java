/* ==================================================================
 * Stabiliti30cDcControlMethod.java - 27/08/2019 4:25:54 pm
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
 * Enumeration of DC control methods.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cDcControlMethod {

	Idle(0x0000, "Idle"),

	Net(0x0001, "Net"),

	Mppt(0x0002, "MPP2"),

	DcCurrent(0x0301, "DC current"),

	DcPower(0x0401, "DC power");

	private final int code;
	private final String description;

	private Stabiliti30cDcControlMethod(int code, String description) {
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
	public static Stabiliti30cDcControlMethod forCode(int code) {
		for ( Stabiliti30cDcControlMethod c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException(
				"Stabiliti30cDcControlMethod code [" + code + "] not supported");
	}

}
