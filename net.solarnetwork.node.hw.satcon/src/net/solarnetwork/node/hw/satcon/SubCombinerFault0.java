/* ==================================================================
 * SubCombinerFault0.java - 11/09/2019 11:06:27 am
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

package net.solarnetwork.node.hw.satcon;

/**
 * Bitmask enumeration of combiner fault 0 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum SubCombinerFault0 implements Fault {

	InternalTemperatureOutsideBounds(0, "The internal box temperature is out of bounds."),

	Aux1InternalTemperatureOutsideBounds(1, "Auxillary 1 internal box temperature is out of bounds."),

	Aux2InternalTemperatureOutsideBounds(2, "Auxillary 1 internal box temperature is out of bounds.");

	private final int code;
	private final String description;

	private SubCombinerFault0(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getFaultGroup() {
		return 0;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDescription() {
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
	public static SubCombinerFault0 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( SubCombinerFault0 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("SubCombinerFault0 code [" + code + "] not supported");
	}

}
