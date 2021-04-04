/* ==================================================================
 * SmaReleaseType.java - 11/09/2020 9:06:15 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.domain;

import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of SMA release type codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaReleaseType implements CodedValue {

	Unversioned(0, "N"),

	Experimental(1, "E"),

	Alpha(2, "A"),

	Beta(3, "B"),

	Release(4, "R"),

	SpecialRelease(5, "S"),

	None(-1, "");

	private int code;
	private String key;

	private SmaReleaseType(int code, String key) {
		this.code = code;
		this.key = key;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the release type key.
	 * 
	 * <p>
	 * The key value is for use in things like human-visible version numbers.
	 * </p>
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the enumeration, never {@literal null} and set to {@link #None}
	 *         if not any other valid code
	 */
	public static SmaReleaseType forCode(int code) {
		final byte c = (byte) code;
		for ( SmaReleaseType v : values() ) {
			if ( v.code == c ) {
				return v;
			}
		}
		return SmaReleaseType.None;
	}
}
