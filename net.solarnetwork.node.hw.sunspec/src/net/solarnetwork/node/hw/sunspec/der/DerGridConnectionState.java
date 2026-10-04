/* ==================================================================
 * DerGridConnectionState.java - 5/10/2026 8:27:22 am
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
 * DER grid connection state.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerGridConnectionState implements CodedValue {

	/** Disconnected from the grid. */
	Disconnected(0, "Disconnected from the grid"),

	/** Connected to the grid. */
	Connected(1, "Connected to the grid"),

	;

	private final int code;
	private final String description;

	private DerGridConnectionState(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a description of the state.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
