/* ==================================================================
 * BacnetDataType.java - 7/11/2022 6:42:30 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet;

import net.solarnetwork.domain.CodedValue;

/**
 * BACnet data type enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum BacnetDataType implements CodedValue {

	/** Null. */
	Null(0),

	/** Boolean. */
	Boolean(1),

	/** Unsigned integer. */
	UnsignedInteger(2),

	/** Signed integer. */
	SignedInteger(3),

	/** 32-bit floating point. */
	Real(4),

	/** 64-bit floating point. */
	Double(5),

	/** Octet (byte) string. */
	OctetString(6),

	/** Character string. */
	CharacterString(7),

	/** Bit string. */
	BitString(8),

	/** Enumeration. */
	Enumerated(9),

	/** Date. */
	Date(10),

	/** Time. */
	Time(11),

	/** Object identifier. */
	ObjectIdentifier(12),

	;

	private int id;

	private BacnetDataType(int id) {
		this.id = id;
	}

	@Override
	public int getCode() {
		return id;
	}

	/**
	 * Get the object ID.
	 * 
	 * <p>
	 * This is an alias for {@link #getCode()}.
	 * </p>
	 * 
	 * @return the ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get an enumeration value for a string key.
	 * 
	 * @param value
	 *        the value to parse into an enumeration value; can be either an
	 *        integer {@code code} or an enumeration name
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if the value cannot be parsed into an enumeration value
	 * @see CodedValue#forCodeValue(int, Class, Enum)
	 */
	public static BacnetDataType forKey(String value) {
		try {
			int code = Integer.parseInt(value);
			BacnetDataType result = CodedValue.forCodeValue(code, BacnetDataType.class, null);
			if ( result != null ) {
				return result;
			}
			throw new IllegalArgumentException(
					String.format("Unsupported BacnetDataTypes value [%s]", value));
		} catch ( NumberFormatException e ) {
			// ignore and try by name
			return BacnetDataType.valueOf(value);
		}
	}

}
