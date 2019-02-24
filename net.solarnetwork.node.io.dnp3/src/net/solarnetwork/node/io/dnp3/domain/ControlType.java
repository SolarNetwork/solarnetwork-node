/* ==================================================================
 * ControlType.java - 22/02/2019 5:22:50 pm
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

package net.solarnetwork.node.io.dnp3.domain;

/**
 * A DNP3 control type.
 * 
 * @author matt
 * @version 1.0
 */
public enum ControlType {

	Analog('A'),

	Binary('B');

	private final char code;

	private ControlType(char code) {
		this.code = code;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code
	 */
	public char getCode() {
		return code;
	}

	/**
	 * Get an enum from a code value.
	 * 
	 * @param code
	 *        the code of the enum to get
	 * @return the code
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static ControlType forCode(char code) {
		for ( ControlType t : values() ) {
			if ( t.code == code ) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unsupported ControlType [" + code + "]");
	}

}
