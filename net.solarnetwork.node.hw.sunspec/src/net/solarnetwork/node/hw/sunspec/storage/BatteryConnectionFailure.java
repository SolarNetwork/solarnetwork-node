/* ==================================================================
 * BatteryConnectionFailure.java - 5/10/2026 9:05:44 pm
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
 * Battery string connection failure reason.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum BatteryConnectionFailure implements CodedValue {

	/** No failure. */
	None(0, "No failure"),

	/** Button pushed. */
	ButtonPushed(1, "Button pushed"),

	/** Ground fault. */
	GroundFault(2, "Ground fault"),

	/** Outside voltage range. */
	OutsideVoltageRange(3, "Outside voltage range"),

	/** Not enabled. */
	NotEnabled(4, "Not enabled"),

	/** Fuse open. */
	FuseOpen(5, "Fuse open"),

	/** Contactor failure. */
	ContactorFailure(6, "Contactor failure"),

	/** Precharge failure. */
	PrechargeFailure(7, "Precharge failure"),

	/** Fault. */
	Fault(8, "Fault"),

	;

	private final int code;
	private final String description;

	private BatteryConnectionFailure(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the reason.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
