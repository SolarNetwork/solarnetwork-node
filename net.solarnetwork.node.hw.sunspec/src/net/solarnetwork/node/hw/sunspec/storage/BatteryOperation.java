/* ==================================================================
 * BatteryOperation.java - 5/10/2026 7:58:02 pm
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
 * Battery bank operations.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum BatteryOperation implements CodedValue {

	/** Connect the battery bank. */
	Connect(1, "Connect the battery bank"),

	/** Disconnect the battery bank. */
	Disconnect(2, "Disconnect the battery bank"),

	;

	private final int code;
	private final String description;

	private BatteryOperation(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the operation.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
