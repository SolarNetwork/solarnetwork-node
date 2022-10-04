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

import net.solarnetwork.domain.Bitmaskable;

/**
 * Bitmask enumeration of fault 0 codes.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public enum KTLCTFault0 implements Bitmaskable {

	/** Protect0090. */
	Protect0090(0),

	/** Protect0080. */
	Protect0080(1),

	/** Protect0070. */
	Protect0070(2),

	/** Protect0060. */
	Protect0060(3),

	/** Protect0050. */
	Protect0050(4),

	/** PVVoltageOver. */
	PVVoltageOver(5),

	/** Protect0040. */
	Protect0040(6),

	/** GridVoltageOutsideLimit07. */
	GridVoltageOutsideLimit07(7),

	/** GridVoltageOutsideLimit08. */
	GridVoltageOutsideLimit08(8),

	/** Protect0030. */
	Protect0030(9),

	/** GridFrequencyOutsideLimit. */
	GridFrequencyOutsideLimit(10),

	/** GridVoltageOutsideLimit11. */
	GridVoltageOutsideLimit11(11),

	/** GridVoltageOutsideLimit12. */
	GridVoltageOutsideLimit12(12),

	/** Protect0020. */
	Protect0020(13),

	/** TempOver. */
	TempOver(14),

	/** Protect0010. */
	Protect0010(15),

	;

	private final int code;

	private KTLCTFault0(int code) {
		this.code = code;
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
	public static KTLCTFault0 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( KTLCTFault0 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("KTLCTFault0 code [" + code + "] not supported");
	}

}
