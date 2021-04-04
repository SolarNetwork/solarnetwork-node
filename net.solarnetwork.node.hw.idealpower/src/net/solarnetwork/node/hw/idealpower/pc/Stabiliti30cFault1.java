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

import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Abort0;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Abort2;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFaultSeverity.Info;

/**
 * Bitmask enumeration of fault 1 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cFault1 implements Stabiliti30cFault {

	AcBcHardSwitch(0, "AC B-C hard switch", Abort2),

	AcCaHardSwitch(1, "AC C-A hard switch", Abort2),

	Dc2InputHardSwitch(2, "DC2 input hard switch", Abort2),

	Dc2OutputHardSwitch(3, "DC2 output hard switch", Abort2),

	Dc3InputHardSwitch(4, "DC3 input hard switch", Abort2),

	Dc3OutputHardSwitch(5, "DC3 output hard switch", Abort2),

	LinkStateTimerCheck(6, "Link state timer check", Abort2),

	BadLinkStart(7, "Bad link start", Abort2),

	InvalidSettings(8, "Invalid method/settings", Abort2),

	IslandDetected(9, "Island detected", Abort0),

	AcUnderVoltageLevel1(10, "AC under voltage level 1 trip", Abort0),

	AcUnderVoltageLevel2(11, "AC under voltage level 2 trip", Abort0),

	AcUnderVoltageLevel3(12, "AC under voltage level 3 trip", Abort0),

	AcUnderVoltageLevel4(13, "AC under voltage level 4 trip", Abort0),

	AcOverVoltageLevel1(14, "AC over voltage level 1 trip", Abort0),

	AcOverVoltageLevel2(15, "AC over voltage level 2 trip", Abort0);

	private final int code;
	private final String description;
	private final Stabiliti30cFaultSeverity severity;

	private Stabiliti30cFault1(int code, String description) {
		this(code, description, Info);
	}

	private Stabiliti30cFault1(int code, String description, Stabiliti30cFaultSeverity severity) {
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
		return 1;
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
	public static Stabiliti30cFault1 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( Stabiliti30cFault1 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("Stabiliti30cFault1 code [" + code + "] not supported");
	}

}
