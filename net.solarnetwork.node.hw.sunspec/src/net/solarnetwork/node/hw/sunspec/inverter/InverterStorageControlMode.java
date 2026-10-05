/* ==================================================================
 * InverterStorageControlMode.java - 6/10/2026 8:41:19 am
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
 * Inverter storage control modes.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum InverterStorageControlMode implements Bitmaskable {

	/** Charge. */
	Charge(0, "Charge"),

	/** Discharge. */
	Discharge(1, "Discharge"),

	;

	private final int offset;
	private final String description;

	private InverterStorageControlMode(int offset, String description) {
		this.offset = offset;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return offset;
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
