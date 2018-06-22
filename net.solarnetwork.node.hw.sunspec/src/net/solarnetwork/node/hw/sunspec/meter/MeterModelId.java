/* ==================================================================
 * MeterModelId.java - 21/05/2018 8:00:22 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.meter;

import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec meter model IDs.
 * 
 * @author matt
 * @version 1.0
 */
public enum MeterModelId implements ModelId {

	SinglePhaseMeterInteger(201, "Single phase (A-N or A-B) meter"),

	SplitSinglePhaseMeterInteger(202, "Split single phase (A-B-N) meter"),

	WyeConnectThreePhaseMeterInteger(203, "WYE connect 3-phase (ABCN) meter"),

	DeltaConnectThreePhaseMeterInteger(204, "Delta connect 3-phase (ABC) meter"),

	SinglePhaseMeterFloat(211, "Single phase (A-N or A-B) meter"),

	SplitSinglePhaseMeterFLoat(212, "Split single phase (A-B-N) meter"),

	WyeConnectThreePhaseMeterFloat(213, "WYE connect 3-phase (ABCN) meter"),

	DeltaConnectThreePhaseMeterFloat(214, "Delta connect 3-phase (ABC) meter");

	final private int id;
	final private String description;

	private MeterModelId(int id, String description) {
		this.id = id;
		this.description = description;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for an ID value.
	 * 
	 * @param id
	 *        the ID to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code id} is not supported
	 */
	public static MeterModelId forId(int id) {
		for ( MeterModelId e : MeterModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
