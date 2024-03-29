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
 * Bitmask enumeration of fault 2 codes.
 * 
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public enum KTLCTFault2 implements KTLCTFault {

	/** Protect0200. */
	Protect0200(0, "Protect0200"),

	/** Protect0210. */
	Protect0210(1, "Internal hardware exception"),

	/** Protect0220. */
	Protect0220(2, "Input / output power mismatch"),

	/** PV2Reverse. */
	PV2Reverse(3, "PV2 reverse connection"),

	/** Protect0240. */
	Protect0240(4, "PV2 input over current"),

	/** PV2VoltageOver. */
	PV2VoltageOver(5, "PV2 voltage is too high"),

	/** Protect0260. */
	Protect0260(6, "Anomal PV source input"),

	/** Protect0230. */
	Protect0230(7, "Turn on the inverter Open loop detection"),

	/** GFDIErr. */
	GFDIErr(8, "GFDI error"),

	/** PV1Reverse. */
	PV1Reverse(9, "PV1 input reverse connection"),

	/** PV1VoltageOver. */
	PV1VoltageOver(10, "PV1 voltage is too high"),

	/** PV3Reverse. */
	PV3Reverse(11, "PV3 input reverse connection"),

	/** PV3VoltageOver. */
	PV3VoltageOver(12, "PV3 voltage is too high"),

	/** Protect0300. */
	Protect0300(13, "Protect0300"),

	/** Protect0290. */
	Protect0290(14, "Protect0290"),

	/** EmergencyStp. */
	EmergencyStp(15, "Emergency stop"),

	;

	private final int code;
	private final String description;

	private KTLCTFault2(int code, String description) {
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
		return 2;
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
	public static KTLCTFault2 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( KTLCTFault2 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("KTLCTFault2 code [" + code + "] not supported");
	}

}
