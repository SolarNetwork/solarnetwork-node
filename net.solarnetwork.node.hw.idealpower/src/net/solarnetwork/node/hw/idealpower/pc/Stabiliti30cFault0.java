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

import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Abort1;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Abort2;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Info;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Lockdown;

/**
 * Bitmask enumeration of fault 0 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cFault0 implements Stabiliti30cFault {

	GfdiFault(0, "GFDI fault (grounded DC)", Lockdown),

	ImiFault(1, "IMI fault (floating DC)", Lockdown),

	PowerModuleHeatsinkTemperatureFault(2, "Power module heatsink temperature fault", Abort1),

	ControlBoardTemperatureFault(3, "Control board temperature fault", Abort1),

	AuxSupplyUnderVoltage(4, "24V auxiliary supply under voltage", Abort1),

	FanFault(5, "Fan fault", Lockdown),

	DcDiffOverVoltage(6, "DC differential over voltage", Abort1),

	DcDiffUnderVoltage(7, "DC differential under voltage", Abort1),

	LinkOverVoltage(8, "Link over voltage", Abort2),

	LinkStarving(9, "Link starving", Abort2),

	LinkOverCurrent(10, "Link over current", Abort2),

	IgbtVcesOverVoltage1(11, "IGBT VCES over voltage 1", Abort2),

	IgbtVcesOverVoltage2(12, "IGBT VCES over voltage 2", Abort2),

	IgbtVcesOverVoltage3(13, "IGBT VCES over voltage 3", Abort2),

	IgbtVcesOverVoltage4(14, "IGBT VCES over voltage 4", Abort2),

	AcAbHardSwitch(15, "AC A-B hard switch", Abort2);

	private final int code;
	private final String description;
	private final Stabiliti30cFaultSeverity severity;

	private Stabiliti30cFault0(int code, String description) {
		this(code, description, Info);
	}

	private Stabiliti30cFault0(int code, String description, Stabiliti30cFaultSeverity severity) {
		this.code = code;
		this.description = description;
		this.severity = severity;
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
	public Stabiliti30cFaultSeverity getSeverity() {
		return severity;
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
