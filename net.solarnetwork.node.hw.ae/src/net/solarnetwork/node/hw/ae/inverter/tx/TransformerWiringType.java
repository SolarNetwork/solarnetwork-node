/* ==================================================================
 * TransformerTapType.java - 27/07/2018 2:28:35 PM
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
 * Enumeration of transformer tap position configuration.
 * 
 * @author matt
 * @version 1.0
 */
public enum TransformerWiringType {

	/** Wye wiring. */
	Wye(0x0010, "Wye wiring"),

	/** Delta wiring. */
	Delta(0x0000, "Delta wiring"),

	;

	private final int code;
	private final String description;

	private TransformerWiringType(int value, String description) {
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
	 * bitmask of configuration data. The encoding is an either/or bit flag
	 * only, so either the {@link #Delta} bit is set or the default of
	 * {@link #Wye} is assumed.
	 * </p>
	 * 
	 * @param word
	 *        the value to extract the voltage type from
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code word} does not contain a supported value
	 */
	public static TransformerWiringType forRegisterValue(int word) {
		if ( (TransformerWiringType.Delta.code & word) == TransformerWiringType.Delta.code ) {
			return TransformerWiringType.Delta;
		}
		return TransformerWiringType.Wye;
	}

	/**
	 * Get an enumeration for a given code value.
	 * 
	 * <p>
	 * As this is an either/or bit flag, if {@code value} is the same value as
	 * {@link #Delta} that is returned, otherwise {@link #Wye} is.
	 * </p>
	 * 
	 * @param value
	 *        the code to get the enum value for
	 * @return the enumeration value
	 */
	public static TransformerWiringType forCode(int value) {
		return (value == TransformerWiringType.Delta.code ? TransformerWiringType.Delta
				: TransformerWiringType.Wye);
	}

}
