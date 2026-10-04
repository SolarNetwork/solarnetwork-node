/* ==================================================================
 * DerControlMode.java - 5/10/2026 9:32:29 am
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

package net.solarnetwork.node.hw.sunspec.der;

import net.solarnetwork.domain.Bitmaskable;

/**
 * DER control mode functions.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerControlMode implements Bitmaskable {

	/** Limit maximum active power. */
	MaxActivePower(0, "Limit maximum active power"),

	/** Fixed active power. */
	FixedActivePower(1, "Fixed active power"),

	/** Fixed reactive power. */
	FixedReactivePower(2, "Fixed reactive power"),

	/** Fixed power factor. */
	FixedPowerFactor(3, "Fixed power factor"),

	/** Volt-var function. */
	VoltVar(4, "Volt-var function"),

	/** Frequency-watt function. */
	FrequencyWatt(5, "Frequency-watt function"),

	/** Dynamic reactive current function. */
	DynamicReactiveCurrent(6, "Dynamic reactive current function"),

	/** Low voltage trip. */
	LowVoltageTrip(7, "Low voltage trip"),

	/** High voltage trip. */
	HighVoltageTrip(8, "High voltage trip"),

	/** Watt-var function. */
	WattVar(9, "Watt-var function"),

	/** Volt-watt function. */
	VoltWatt(10, "Volt-watt function"),

	/** Scheduling. */
	Scheduled(11, "Scheduling"),

	/** Low frequency trip. */
	LowFrequencyTrip(12, "Low frequency trip"),

	/** High frequency trip. */
	HighFrequencyTrip(13, "High frequency trip"),

	;

	private final int offset;
	private final String description;

	private DerControlMode(int offset, String description) {
		this.offset = offset;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return offset;
	}

	/**
	 * Get a description of the control mode.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
