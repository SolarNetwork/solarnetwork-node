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
 * Bitmask enumeration of fault 0 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cFault0 implements Stabiliti30cFault {

	GfdiFault(0, "GFDI fault (grounded DC)"),

	ImiFault(1, "IMI fault (floating DC)"),

	PowerModuleHeatsinkTemperatureFault(2, "Power module heatsink temperature fault"),

	ControlBoardTemperatureFault(3, "Control board temperature fault"),

	AuxSupplyUnderVoltage(4, "24V auxiliary supply under voltage"),

	FanFault(5, "Fan fault"),

	DcDiffOverVoltage(6, "DC differential over voltage"),

	DcDiffUnderVoltage(7, "DC differential under voltage"),

	LinkOverVoltage(8, "Link over voltage"),

	LinkStarving(9, "Link starving"),

	LinkOverCurrent(10, "Link over current"),

	IgbtVcesOverVoltage1(11, "IGBT VCES over voltage 1"),

	IgbtVcesOverVoltage2(12, "IGBT VCES over voltage 2"),

	IgbtVcesOverVoltage3(13, "IGBT VCES over voltage 3"),

	IgbtVcesOverVoltage4(14, "IGBT VCES over voltage 4"),

	AcAbHardSwitch(15, "AC A-B hard switch");

	private final int code;
	private final String description;

	private Stabiliti30cFault0(int code, String description) {
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
		return 0;
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
	public static Stabiliti30cFault0 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( Stabiliti30cFault0 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("Stabiliti30cFault0 code [" + code + "] not supported");
	}

}
