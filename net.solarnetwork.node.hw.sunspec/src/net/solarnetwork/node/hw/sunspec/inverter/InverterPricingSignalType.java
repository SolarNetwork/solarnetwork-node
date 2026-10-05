/* ==================================================================
 * InverterPricingSignalType.java - 6/10/2026 9:15:26 am
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

import net.solarnetwork.domain.CodedValue;

/**
 * Inverter pricing signal type, which defines the meaning of a pricing signal.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum InverterPricingSignalType implements CodedValue {

	/** An unknown or other type of value. */
	Unknown(0, "Unknown"),

	/**
	 * An absolute price in the local rate, for example {@literal 23}
	 * (cents/kWh).
	 */
	Absolute(1, "Absolute price"),

	/**
	 * A relative price in the local rate, for example {@literal -5}
	 * (cents/kWh).
	 */
	Relative(2, "Relative price"),

	/**
	 * A price multiplier percentage, for example {@literal 15} for a 15% uplift
	 * in the rate.
	 */
	Multiplier(3, "Price multiplier"),

	/**
	 * A price level, for example from {@literal 0} for the lowest to
	 * {@literal 4} for the highest.
	 */
	Level(4, "Price level"),

	;

	private final int code;
	private final String description;

	private InverterPricingSignalType(int code, String description) {
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
