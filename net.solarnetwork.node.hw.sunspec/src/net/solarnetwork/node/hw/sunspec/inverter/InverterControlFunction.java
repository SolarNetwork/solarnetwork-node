/* ==================================================================
 * InverterControlFunction.java - 6/10/2026 7:33:05 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.inverter;

import net.solarnetwork.domain.Bitmaskable;

/**
 * Inverter control functions.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum InverterControlFunction implements Bitmaskable {

	/** Fixed active power. */
	FixedActivePower(0, "Fixed active power"),

	/** Fixed reactive power. */
	FixedReactivePower(1, "Fixed reactive power"),

	/** Fixed power factor. */
	FixedPowerFactor(2, "Fixed power factor"),

	/** Volt-var function. */
	VoltVar(3, "Volt-var function"),

	/** Parameterized frequency-watt function. */
	ParameterizedFrequencyWatt(4, "Parameterized frequency-watt function"),

	/** Curve-based frequency-watt function. */
	CurveBasedFrequencyWatt(5, "Curve-based frequency-watt function"),

	/** Dynamic reactive current function. */
	DynamicReactiveCurrent(6, "Dynamic reactive current function"),

	/** Low voltage ride-through. */
	LowVoltageRideThrough(7, "Low voltage ride-through"),

	/** High voltage ride-through. */
	HighVoltageRideThrough(8, "High voltage ride-through"),

	/** Watt-power factor function. */
	WattPowerFactor(9, "Watt-power factor function"),

	/** Volt-watt function. */
	VoltWatt(10, "Volt-watt function"),

	/** Scheduling. */
	Scheduled(12, "Scheduling"),

	/** Low frequency ride-through. */
	LowFrequencyRideThrough(13, "Low frequency ride-through"),

	/** High frequency ride-through. */
	HighFrequencyRideThrough(14, "High frequency ride-through"),

	;

	private final int offset;
	private final String description;

	private InverterControlFunction(int offset, String description) {
		this.offset = offset;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return offset;
	}

	/**
	 * Get a description of the function.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
