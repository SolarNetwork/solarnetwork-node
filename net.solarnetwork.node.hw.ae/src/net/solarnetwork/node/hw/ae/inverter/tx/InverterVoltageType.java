/* ==================================================================
 * InverterVoltageType.java - 27/07/2018 2:18:29 PM
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

package net.solarnetwork.node.hw.ae.inverter.tx;

/**
 * Inverter voltage type enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum InverterVoltageType {

	AC_208(0x001, "AC 208 volts"),

	AC_240(0x002, "AC 240 volts"),

	AC_480(0x004, "AC 480 volts"),

	AC_600(0x200, "AC 600 volts");

	private final int code;
	private final String description;

	private InverterVoltageType(int value, String description) {
		this.code = value;
		this.description = description;
	}

	/**
	 * Get the type value encoding.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the type.
	 * 
	 * @return a description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for the power scale in a given register value.
	 * 
	 * <p>
	 * The register value is the raw data read from Modbus and contains a
	 * bitmask of configuration data.
	 * </p>
	 * 
	 * @param word
	 *        the value to extract the voltage type from
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code word} does not contain a supported value
	 */
	public static InverterVoltageType forRegisterValue(int word) {
		for ( InverterVoltageType e : values() ) {
			if ( (word & e.code) == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unsupported voltage type value: " + word);
	}

	/**
	 * Get an enumeration for a given code value.
	 * 
	 * @param value
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static InverterVoltageType forCode(int value) {
		for ( InverterVoltageType s : values() ) {
			if ( s.code == value ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported voltage type value: " + value);
	}
}
