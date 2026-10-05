/* ==================================================================
 * BatteryType.java - 5/10/2026 7:58:02 pm
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

package net.solarnetwork.node.hw.sunspec.storage;

import net.solarnetwork.domain.CodedValue;

/**
 * Battery type.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum BatteryType implements CodedValue {

	/** Not applicable or unknown. */
	Unknown(0, "Not applicable or unknown"),

	/** Lead acid. */
	LeadAcid(1, "Lead acid"),

	/** Nickel metal hydride. */
	NickelMetalHydride(2, "Nickel metal hydride"),

	/** Nickel cadmium. */
	NickelCadmium(3, "Nickel cadmium"),

	/** Lithium-ion. */
	LithiumIon(4, "Lithium-ion"),

	/** Carbon zinc. */
	CarbonZinc(5, "Carbon zinc"),

	/** Zinc chloride. */
	ZincChloride(6, "Zinc chloride"),

	/** Alkaline. */
	Alkaline(7, "Alkaline"),

	/** Rechargeable alkaline. */
	RechargeableAlkaline(8, "Rechargeable alkaline"),

	/** Sodium sulfur. */
	SodiumSulfur(9, "Sodium sulfur"),

	/** Flow. */
	Flow(10, "Flow"),

	/** Other. */
	Other(99, "Other"),

	;

	private final int code;
	private final String description;

	private BatteryType(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the type.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
