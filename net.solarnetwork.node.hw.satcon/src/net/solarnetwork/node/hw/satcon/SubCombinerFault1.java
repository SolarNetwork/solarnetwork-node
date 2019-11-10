/* ==================================================================
 * SubCombinerFault1.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of combiner fault 1 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum SubCombinerFault1 implements Fault {

	CurrentOutsideBounds1(
			0,
			"String 1 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds2(
			1,
			"String 2 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds3(
			2,
			"String 3 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds4(
			3,
			"String 4 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds5(
			4,
			"String 5 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds6(
			5,
			"String 6 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds7(
			6,
			"String 7 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds8(
			7,
			"String 8 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds9(
			8,
			"String 9 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds10(
			9,
			"String 10 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds11(
			10,
			"String 11 current is above Current Deviation Minimum and beyond Current Deviation Threshold."),

	CurrentOutsideBounds12(
			11,
			"String 12 current is above Current Deviation Minimum and beyond Current Deviation Threshold.");

	private final int code;
	private final String description;

	private SubCombinerFault1(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getFaultGroup() {
		return 1;
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
	public static SubCombinerFault1 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( SubCombinerFault1 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("SubCombinerFault1 code [" + code + "] not supported");
	}

}
