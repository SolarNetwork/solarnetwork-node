/* ==================================================================
 * Stabiliti30cFault0.java - 27/08/2019 5:11:50 pm
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

package net.solarnetwork.node.hw.idealpower.pc;

/**
 * Bitmask enumeration of fault 3 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cFault3 implements Stabiliti30cFault {

	AcSwitchBreakdownVoltageExceeded(0, "AC switch breakdown voltage exceeded"),

	DcSwitchBreakdownVoltageExceeded(1, "DC switch breakdown voltage exceeded"),

	PrechargeTimeout(2, "Precharge timeout"),

	AcToLinkPrimaryHighVoltage(3, "AC to link primary high voltage"),

	AcLineToLineInstantaneousOverVoltage(4, "AC line to line instantaneous over voltage"),

	LicenseFault(5, "License fault"),

	GridAckTimeout(6, "Grid_Ack timeout");

	private final int code;
	private final String description;

	private Stabiliti30cFault3(int code, String description) {
		this.code = code;
		this.description = description;
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

	@Override
	public int getFaultGroup() {
		return 3;
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
	public static Stabiliti30cFault3 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( Stabiliti30cFault3 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("Stabiliti30cFault3 code [" + code + "] not supported");
	}

}
