/* ==================================================================
 * DerReactivePowerSetpointMode.java - 5/10/2026 2:41:17 pm
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

import net.solarnetwork.domain.CodedValue;

/**
 * DER reactive power setpoint mode.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerReactivePowerSetpointMode implements CodedValue {

	/** Percentage of maximum active power. */
	MaximumActivePowerPercent(0, "Percentage of maximum active power"),

	/** Percentage of maximum reactive power. */
	MaximumReactivePowerPercent(1, "Percentage of maximum reactive power"),

	/** Percentage of available reactive power. */
	AvailableReactivePowerPercent(2, "Percentage of available reactive power"),

	/** Percentage of maximum apparent power. */
	MaximumApparentPowerPercent(3, "Percentage of maximum apparent power"),

	/** Vars. */
	Vars(4, "Vars"),

	;

	private final int code;
	private final String description;

	private DerReactivePowerSetpointMode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the mode.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
