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
 * Bitmask enumeration of fault 0 codes.
 * 
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public enum KTLCTFault0 implements KTLCTFault {

	/** Protect0090. */
	Protect0090(0, "BUS sum high"),

	/** Protect0080. */
	Protect0080(1, "Protect0080"),

	/** Protect0070. */
	Protect0070(2, "BUS difference is high"),

	/** Protect0060. */
	Protect0060(3, "Bus Soft start time out"),

	/** Protect0050. */
	Protect0050(4, "Inverter Soft start time out"),

	/** PVVoltageOver. */
	PVVoltageOver(5, "PV voltage over limit"),

	/** Protect0040. */
	Protect0040(6, "PV1 High current"),

	/** GridVoltageOutsideLimit07. */
	GridVoltageOutsideLimit07(7, "Grid line voltage over limit"),

	/** GridVoltageOutsideLimit08. */
	GridVoltageOutsideLimit08(8, "Grid phase voltage over limit"),

	/** Protect0030. */
	Protect0030(9, "Inverter current too high"),

	/** GridFrequencyOutsideLimit. */
	GridFrequencyOutsideLimit(10, "High frequency of power grid"),

	/** GridVoltageOutsideLimit11. */
	GridVoltageOutsideLimit11(11, "Low frequency of power grid"),

	/** GridVoltageOutsideLimit12. */
	GridVoltageOutsideLimit12(12, "Out of phase"),

	/** Protect0020. */
	Protect0020(13, "Grid connected relay protection"),

	/** TempOver. */
	TempOver(14, "Over temperature protection"),

	/** Protect0010. */
	Protect0010(15, "Inverter current bias"),

	;

	private final int code;
	private final String description;

	private KTLCTFault0(int code, String description) {
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
