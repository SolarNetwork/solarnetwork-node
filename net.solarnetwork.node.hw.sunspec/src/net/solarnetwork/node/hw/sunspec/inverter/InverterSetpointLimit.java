/* ==================================================================
 * InverterSetpointLimit.java - 6/10/2026 7:31:40 am
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
 * Inverter setpoint limits.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum InverterSetpointLimit implements Bitmaskable {

	/** Maximum active power. */
	MaximumActivePower(0, "Maximum active power"),

	/** Maximum apparent power. */
	MaximumApparentPower(1, "Maximum apparent power"),

	/** Available reactive power. */
	AvailableReactivePower(2, "Available reactive power"),

	/** Maximum reactive power in quadrant 1. */
	MaximumReactivePowerQ1(3, "Maximum reactive power in quadrant 1"),

	/** Maximum reactive power in quadrant 2. */
	MaximumReactivePowerQ2(4, "Maximum reactive power in quadrant 2"),

	/** Maximum reactive power in quadrant 3. */
	MaximumReactivePowerQ3(5, "Maximum reactive power in quadrant 3"),

	/** Maximum reactive power in quadrant 4. */
	MaximumReactivePowerQ4(6, "Maximum reactive power in quadrant 4"),

	/** Minimum power factor in quadrant 1. */
	MinimumPowerFactorQ1(7, "Minimum power factor in quadrant 1"),

	/** Minimum power factor in quadrant 2. */
	MinimumPowerFactorQ2(8, "Minimum power factor in quadrant 2"),

	/** Minimum power factor in quadrant 3. */
	MinimumPowerFactorQ3(9, "Minimum power factor in quadrant 3"),

	/** Minimum power factor in quadrant 4. */
	MinimumPowerFactorQ4(10, "Minimum power factor in quadrant 4"),

	;

	private final int offset;
	private final String description;

	private InverterSetpointLimit(int offset, String description) {
		this.offset = offset;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return offset;
	}

	/**
	 * Get a description of the limit.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
