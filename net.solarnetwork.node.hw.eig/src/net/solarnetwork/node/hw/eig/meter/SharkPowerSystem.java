/* ==================================================================
 * SharkPowerSystem.java - 26/07/2018 2:09:59 PM
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

package net.solarnetwork.node.hw.eig.meter;

/**
 * Enumeration of the "power system" wiring configuration on a Shark meter.
 * 
 * @author matt
 * @version 1.0
 */
public enum SharkPowerSystem {

	ThreeElementWye(0, "3 element, Wye"),

	TwoCtDelta(1, "2 CT delta"),

	TwoFiveElementWye(3, "2.5 element Wye");

	private final int code;
	private final String description;

	private SharkPowerSystem(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for a code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static SharkPowerSystem forCode(int code) {
		for ( SharkPowerSystem e : SharkPowerSystem.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Code [" + code + "] not supported");
	}

	/**
	 * Get an enumeration for the power system in a given register value.
	 * 
	 * <p>
	 * The register value is the raw data read from Modbus and contains a
	 * bitmask of data where the lower 4 bits represent a power system code.
	 * </p>
	 * 
	 * @param word
	 *        the value to get the power system enumeration for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code word} is not a supported value
	 */
	public static SharkPowerSystem forRegisterValue(int word) {
		int v = (word & 0xF);
		return forCode(v);
	}
}
