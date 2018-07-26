/* ==================================================================
 * SharkPowerScale.java - 26/07/2018 11:30:48 AM
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
 * Enumeration of scale factors used by Shark meters.
 * 
 * @author matt
 * @version 1.0
 */
public enum SharkScale {

	Unit(0),

	Kilo(3),

	Mega(6),

	Auto(8);

	private final int code;

	private SharkScale(int value) {
		this.code = value;
	}

	/**
	 * Get the scale value encoding.
	 * 
	 * @return
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get a scale factor to use with this enumeration.
	 * 
	 * @return the scale factor
	 */
	public int getScaleFactor() {
		switch (this) {
			case Kilo:
				return 1000;

			case Mega:
				return 1000000;

			default:
				return 1;
		}
	}

	/**
	 * Get an enumeration for the power scale in a given register value.
	 * 
	 * <p>
	 * The register value is the raw data read from Modbus and contains a
	 * bitmask of data in the form {@literal pppp--nn-eee-ddd} where the
	 * {@literal pppp} part represents the power scale.
	 * </p>
	 * 
	 * @param word
	 *        the value to get the power scale enumeration for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code word} is not a supported value
	 */
	public static SharkScale forPowerRegisterValue(int word) {
		int v = (word >> 12) & 0xF;
		return forCode(v);
	}

	/**
	 * Get an enumeration for the energy scale in a given register value.
	 * 
	 * <p>
	 * The register value is the raw data read from Modbus and contains a
	 * bitmask of data in the form {@literal pppp--nn-eee-ddd} where the
	 * {@literal eee} part represents the energy scale.
	 * </p>
	 * 
	 * @param word
	 *        the value to get the power scale enumeration for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code word} is not a supported value
	 */
	public static SharkScale forEnergyRegisterValue(int word) {
		int v = (word >> 4) & 0x7;
		return forCode(v);
	}

	/**
	 * Get an enumeration for a given code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static SharkScale forCode(int value) {
		for ( SharkScale s : values() ) {
			if ( s.code == value ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported scale value: " + value);
	}

}
