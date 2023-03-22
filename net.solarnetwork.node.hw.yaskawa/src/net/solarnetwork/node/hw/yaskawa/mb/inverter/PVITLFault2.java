/* ==================================================================
 * PVITLPermanentFault.java - 22/03/2023 4:12:23 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.yaskawa.mb.inverter;

/**
 * Fault1 enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PVITLFault2 implements PVITLFault {

	/** Internal hardware error. */
	InternalHardware(1, "Internal hardware error"),

	/** Input/output power mismatch. */
	IoPowerMismatch(2, "Input/output power mismatch"),

	/** PV2 input reverse connection. */
	Pv2InputReverseConnection(3, "PV2 input reverse connection"),

	/** PV2 over current. */
	Pv2OverCurrent(4, "PV2 over current"),

	/** PV2 over voltage. */
	Pv2OverVoltage(5, "PV2 over voltage"),

	/** PV abnormal input. */
	PvAbnormalInput(6, "PV abnormal input"),

	/** Inverter open-loop self-test error. */
	InverterOpenLoopSelfTest(7, "Inverter open-loop self-test error"),

	/** PV1 input reverse connection. */
	Pv1InputReverseConnection(9, "PV1 input reverse connection"),

	/** PV1 over voltage. */
	Pv1OverVoltage(10, "PV1 over voltage"),

	/** Arcboad abnormal. */
	ArcboardAbnormal(13, "Arcboad abnormal"),

	/** Static GFI protect. */
	StaticGfiProtect(14, "Static GFI protect"),

	/** Arc protection. */
	ArcProtection(15, "Arc protection"),

	;

	private final int code;
	private final String description;

	private PVITLFault2(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	@Override
	public int getGroupIndex() {
		return 3;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
