/* ==================================================================
 * DerOperationalCharacteristic.java - 5/10/2026 8:27:22 am
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
 * DER operational characteristics.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerOperationalCharacteristic implements Bitmaskable {

	/** The DER is operating as part of a larger grid. */
	GridFollowing(0, "Grid following"),

	/** The DER is providing the grid. */
	GridForming(1, "Grid forming"),

	/** The PV output is clipped. */
	PvClipped(2, "PV output clipped"),

	;

	private final int offset;
	private final String description;

	private DerOperationalCharacteristic(int offset, String description) {
		this.offset = offset;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return offset;
	}

	/**
	 * Get a description of the characteristic.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
