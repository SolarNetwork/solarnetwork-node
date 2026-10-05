/* ==================================================================
 * InverterRideThrough.java - 6/10/2026 7:34:21 am
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
 * Inverter ride-through functions.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum InverterRideThrough implements Bitmaskable {

	/** Low voltage ride-through. */
	LowVoltage(0, "Low voltage ride-through"),

	/** High voltage ride-through. */
	HighVoltage(1, "High voltage ride-through"),

	/** Low frequency ride-through. */
	LowFrequency(2, "Low frequency ride-through"),

	/** High frequency ride-through. */
	HighFrequency(3, "High frequency ride-through"),

	;

	private final int offset;
	private final String description;

	private InverterRideThrough(int offset, String description) {
		this.offset = offset;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return offset;
	}

	/**
	 * Get a description of the ride-through.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
