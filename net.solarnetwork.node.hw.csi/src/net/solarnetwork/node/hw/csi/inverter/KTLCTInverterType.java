/* ==================================================================
 * KTLCTInverterType.java - 2/08/2018 9:51:04 AM
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

package net.solarnetwork.node.hw.csi.inverter;

/**
 * Enumeration of CSI inverter types.
 * 
 * @author matt
 * @version 1.0
 */
public enum KTLCTInverterType {

	/** 50 kW KTL CT. */
	CSI_50KTL_CT(0x4031, "50 kW KTL CT"),

	/** 60 kW KTL CT. */
	CSI_60KTL_CT(0x4032, "60 kW KTL CT"),

	/** 60 kW KTL CT v2. */
	CSI_60KTL_CT_V2(0x4033, "60 kW KTL CT v2"),

	;

	private final int code;
	private final String description;

	private KTLCTInverterType(int value, String description) {
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
	 * Get an enumeration for a given code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static KTLCTInverterType forCode(int code) {
		for ( KTLCTInverterType s : values() ) {
			if ( s.code == code ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported work mode value: " + code);
	}

}
