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
 * Permanent fault enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PVITLPermanentFault implements PVITLFault {

	/** Bus (sum) over voltage. */
	PermanentBusOverVoltage(0, "Permanent bus (sum) over voltage"),

	/** Bus (sum) low voltage. */
	PermanentBusUnderVoltage(1, "Permanent bus (sum) low voltage"),

	/** Bus imbalance. */
	PermanentBusImbalance(2, "Permanent bus imbalance"),

	/** Grid relay. */
	PermanentGridRelay(3, "Permanent grid relay"),

	/** Static GFCI. */
	StaticGfci(4, "Static GFCI"),

	/** DCI. */
	Dci(6, "DCI"),

	/** Hardware over current. */
	HardwareOverCurrent(8, "Hardware over current"),

	/** Power module. */
	PowerModule(12, "Power module"),

	/** Internal hardware. */
	InternalHardware(13, "Internal hardware"),

	/** Inverter open-loop self-test. */
	PermanentInverterOpenLoopSelfTest(14, "Permanent inverter open-loop self-test"),

	/** 15V control board low. */
	PermanentControlBoard15VLow(15, "15V control board low"),

	;

	private final int code;
	private final String description;

	private PVITLPermanentFault(int code, String description) {
		this.code = code;
		this.description = description;
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

}
