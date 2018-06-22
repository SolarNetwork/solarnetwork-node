/* ==================================================================
 * ION6200VoltsMode.java - 15/05/2018 10:54:38 AM
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

package net.solarnetwork.node.hw.schneider.meter;

/**
 * Enumeration of the "volts mode" configuration on an ION6200 meter.
 * 
 * @author matt
 * @version 1.0
 */
public enum ION6200VoltsMode {

	FourWire(0, "4-Wire WYE"),

	Delta(1, "Delta"),

	TwoWire(2, "Single Phase"),

	Demonstration(3, "Demonstration"),

	ThreeWire(4, "3-Wire WYE"),

	DeltaDirect(5, "Delta direct");

	private final int code;
	private final String description;

	private ION6200VoltsMode(int code, String description) {
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
	 * Get a volts mode enumeration for a code value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static ION6200VoltsMode forCode(int code) {
		for ( ION6200VoltsMode e : ION6200VoltsMode.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Code [" + code + "] not supported");
	}

}
