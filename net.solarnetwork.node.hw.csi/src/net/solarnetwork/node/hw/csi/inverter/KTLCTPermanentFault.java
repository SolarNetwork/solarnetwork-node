/* ==================================================================
 * KTLCTPermanentFault.java - 26/03/2019 11:37:21 am
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
 * Bitmask enumeration of permanent fault codes.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public enum KTLCTPermanentFault implements KTLCTFault {

	/** Fault0130. */
	Fault0130(0, "Bus sum high fault"),

	/** Fault0120. */
	Fault0120(1, "3.3V voltage low"),

	/** Fault0110. */
	Fault0110(2, "Bus differential high fault"),

	/** Fault0100. */
	Fault0100(3, "Relay fault"),

	/** Fault0090. */
	Fault0090(4, "Steady state GFCI fault"),

	/** Fault0080. */
	Fault0080(5, "Bst hardware overcurrent fault"),

	/** Fault0070. */
	Fault0070(6, "DCI too high fault"),

	/** Fault0060. */
	Fault0060(7, "Bus hardware overvoltage fault"),

	/** Fault0050. */
	Fault0050(8, "Inverter hardware overcurrent fault"),

	/** Fault0040. */
	Fault0040(9, "Permanent fault of driver source"),

	/** Fault0030. */
	Fault0030(10, "Fault0030"),

	/** Fault0020. */
	Fault0020(11, "Fault0020"),

	/** Fault0010. */
	Fault0010(12, "Permanent fault of power module"),

	/** Fault0140. */
	Fault0140(13, "Internal hardware failure"),

	/** Fault0150. */
	Fault0150(14, "Open loop self detection failure"),

	/** Fault0160. */
	Fault0160(15, "Control board voltage and drive power failure"),

	;

	private final int code;
	private final String description;

	private KTLCTPermanentFault(int code, String description) {
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
	public static KTLCTPermanentFault forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( KTLCTPermanentFault c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("KTLCTPermanentFault code [" + code + "] not supported");
	}

}
