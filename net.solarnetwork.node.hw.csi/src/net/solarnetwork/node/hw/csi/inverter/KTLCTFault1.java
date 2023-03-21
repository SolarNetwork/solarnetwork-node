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
 * Bitmask enumeration of fault 1 codes.
 * 
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public enum KTLCTFault1 implements KTLCTFault {

	/** Protect0190. */
	Protect0190(0, "Protect0190"),

	/** Protect0180. */
	Protect0180(1, "DCI current bias"),

	/** Protect0170. */
	Protect0170(2, "DCI High current"),

	/** IsolationErr. */
	IsolationErr(3, "Insulation impedance is too low"),

	/** GFCIErr. */
	GFCIErr(4, "Leakage current is too high"),

	/** Protect0160. */
	Protect0160(5, "Frequency selective anomaly"),

	/** PVReverse. */
	PVReverse(6, "PV reverse"),

	/** Protect0150. */
	Protect0150(7, "MCU Protect"),

	/** Protect0140. */
	Protect0140(8, "Inverter hardware over current"),

	/** GridVoltageOutsideLimit09. */
	GridVoltageOutsideLimit09(9, "Unbalanced grid voltage"),

	/** Protect0270. */
	Protect0270(10, "Protect0270"),

	/** Protect0130. */
	Protect0130(11, "Inverter current imbalance"),

	/** Protect0120. */
	Protect0120(12, "Power module protection"),

	/** ACContErr. */
	ACContErr(13, "AC cont error"),

	/** Protect0110. */
	Protect0110(14, "Bus hardware over voltage"),

	/** Protect0100. */
	Protect0100(15, "Leakage current sensor fault"),

	;

	private final int code;
	private final String description;

	private KTLCTFault1(int code, String description) {
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
		return 1;
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
	public static KTLCTFault1 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( KTLCTFault1 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("KTLCTFault1 code [" + code + "] not supported");
	}

}
