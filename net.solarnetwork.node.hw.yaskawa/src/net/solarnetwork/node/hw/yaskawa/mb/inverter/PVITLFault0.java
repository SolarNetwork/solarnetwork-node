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
 * Fault0 enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum PVITLFault0 implements PVITLFault {

	/** Bus (sum) over voltage. */
	BusOverVoltage(0, "Bus (sum) over voltage"),

	/** Bus (sum) low voltage. */
	BusUnderVoltage(1, "Bus (sum) low voltage"),

	/** Bus imbalance. */
	BusImbalance(2, "Bus imbalance"),

	/** Bus soft start timeout. */
	BusSoftStartTimeout(3, "Bus soft start timeout"),

	/** Inverter soft start timeout. */
	InverterSoftStartTimeout(4, "Inverter soft start timeout"),

	/** PV1 over current. */
	Pv1OverCurrent(6, "PV1 over current"),

	/** Grid line voltage out of range. */
	GridLineVoltage(7, "Grid line voltage out of range"),

	/** Grid phase voltage out of range. */
	GridPhaseVoltage(8, "Grid phase voltage out of range"),

	/** Inverter over current. */
	InverterOverCurrent(9, "Inverter over current"),

	/** Grid over frequency. */
	GridOverFrequency(10, "Grid over frequency"),

	/** Grid under frequency. */
	GridUnderFrequency(11, "Grid under frequency"),

	/** Loss of main. */
	LossOfMain(12, "Loss of main"),

	/** Grid relay. */
	GridRelay(13, "Grid relay"),

	/** Over temperature protection. */
	OverTemperature(14, "Over temperature protection"),

	/** Sampling offset of output current. */
	OutputCurrentSamplingOffset(15, "Sampling offset of output current"),

	;

	private final int code;
	private final String description;

	private PVITLFault0(int code, String description) {
		this.code = code;
		this.description = description;
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

}
