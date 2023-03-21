/* ==================================================================
 * KTLCTPermanentWarn.java - 26/03/2019 11:37:21 am
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

package net.solarnetwork.node.hw.csi.inverter;

/**
 * Bitmask enumeration of warning codes.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public enum KTLCTWarn implements KTLCTFault {

	/** Warn0010. */
	Warn0010(0, "External fan alarm"),

	/** Warn0020. */
	Warn0020(1, "Internal fan alarm"),

	/** CommErr. */
	CommErr(2, "Comm error"),

	/** Warn0030. */
	Warn0030(3, "EEPROM fault"),

	/** Warn0040. */
	Warn0040(4, "DC side lightning protection exception"),

	/** Warn0050. */
	Warn0050(5, "Temperature sensor anomaly"),

	/** Warn0060. */
	Warn0060(6, "Warn0060"),

	/** Warn0070. */
	Warn0070(7, "AC side MOV anomaly"),

	/** Warn0080. */
	Warn0080(8, "Warn0080"),

	/** Warn0090. */
	Warn0090(9, "Warn0090"),

	/** Warn0100. */
	Warn0100(10, "Warn0100"),

	/** Warn0110. */
	Warn0110(11, "Warn0110"),

	/** Warn0120. */
	Warn0120(12, "Warn0120"),

	/** Warn0130. */
	Warn0130(13, "Warn0130"),

	/** Warn0140. */
	Warn0140(14, "Warn0140"),

	/** Warn0150. */
	Warn0150(15, "Warn0150"),

	;

	private final int code;
	private final String description;

	private KTLCTWarn(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Get the code for this condition.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	@Override
	public int getGroupIndex() {
		return 0;
	}

	@Override
	public String getDescription() {
		return description;
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
	public static KTLCTWarn forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( KTLCTWarn c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("KTLCTWarn code [" + code + "] not supported");
	}

}
