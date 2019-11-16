/* ==================================================================
 * NmeaMode.java - 12/11/2019 6:13:54 am
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

package net.solarnetwork.node.io.gpsd.domain;

/** A NMEA mode. */
public enum NmeaMode {

	/** Unknown fix. */
	Unknown(0),

	/** No fix available. */
	NoFix(1),

	/** Two dimensional fix. */
	TwoDimensional(2),

	/** Three dimensional fix. */
	ThreeDimensional(3);

	private final int code;

	private NmeaMode(int code) {
		this.code = code;
	}

	/**
	 * Get the raw mode code value.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get an enumeration instance from a code value.
	 * 
	 * @param code
	 *        the raw code value to get an enum for
	 * @return the mode, never {@literal null}
	 */
	public static NmeaMode forCode(final int code) {
		switch (code) {
			case 1:
				return NoFix;
			case 2:
				return TwoDimensional;
			case 3:
				return ThreeDimensional;
			default:
				return Unknown;
		}
	}

}
