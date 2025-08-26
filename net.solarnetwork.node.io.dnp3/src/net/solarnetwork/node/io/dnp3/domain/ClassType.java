/* ==================================================================
 * ClassType.java - 18/08/2025 4:53:07 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
 * Enumeration of DNP3 classes.
 *
 * @author matt
 * @version 1.0
 */
public enum ClassType {

	/** Class 0 static data. */
	Static(0, "Class 0 - Static data"),

	/** Class 1 event data. */
	Event1(1, "Class 1 Events"),

	/** Class 2 event data. */
	Event2(2, "Class 1 Events"),

	/** Class 3 event data. */
	Event3(3, "Class 1 Events"),

	;

	private final byte code;
	private final String title;

	private ClassType(int code, String title) {
		this.code = (byte) code;
		this.title = title;
	}

	/**
	 * Get the code value.
	 *
	 * @return the code
	 */
	public byte getCode() {
		return code;
	}

	/**
	 * Get the title.
	 *
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Get an enum from a code value.
	 *
	 * @param code
	 *        the code of the enum to get
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static ClassType forCode(int code) {
		for ( ClassType t : values() ) {
			if ( t.code == code ) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unsupported ClassType [" + code + "]");
	}

	/**
	 * Get an enum from a code or name value.
	 *
	 * @param value
	 *        the code or name of the enum to get
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code value} is not supported
	 */
	public static ClassType forValue(String value) {
		try {
			return forCode(Byte.parseByte(value));
		} catch ( NumberFormatException e ) {
			// ignore and treat as name
		}
		for ( ClassType t : values() ) {
			if ( value.equalsIgnoreCase(t.name()) ) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unsupported ClassType [" + value + "]");
	}

}
