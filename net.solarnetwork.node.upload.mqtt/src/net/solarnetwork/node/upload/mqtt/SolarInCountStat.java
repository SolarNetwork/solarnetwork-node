/* ==================================================================
 * SolarInCountStat.java - 5/05/2021 3:35:33 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.mqtt;

/**
 * SolarIn/MQTT statistic types.
 *
 * @author matt
 * @version 1.1
 * @since 1.8
 */
public enum SolarInCountStat {

	/** Posted datum. */
	NodeDatumPosted("node datum posted"),

	/** Posted location datum. */
	LocationDatumPosted("location datum posted"),

	/** Posted instruction statuses. */
	InstructionStatusPosted("instruction status posted"),

	/** Received instructions. */
	InstructionsReceived("instructions received"),

	;

	private final String description;

	private SolarInCountStat(String description) {
		this.description = description;
	}

	/**
	 * Get a description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
