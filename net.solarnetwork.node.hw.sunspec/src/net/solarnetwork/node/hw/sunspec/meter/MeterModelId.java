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

import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec meter model IDs.
 * 
 * @author matt
 * @version 1.1
 */
public enum MeterModelId implements ModelId {

	/** Single phase (A-N or A-B) meter. */
	SinglePhaseMeterInteger(201, "Single phase (A-N or A-B) meter"),

	/** Split single phase (A-B-N) meter. */
	SplitSinglePhaseMeterInteger(202, "Split single phase (A-B-N) meter"),

	/** WYE connect 3-phase (ABCN) meter. */
	WyeConnectThreePhaseMeterInteger(203, "WYE connect 3-phase (ABCN) meter"),

	/** Delta connect 3-phase (ABC) meter. */
	DeltaConnectThreePhaseMeterInteger(204, "Delta connect 3-phase (ABC) meter"),

	/** Single phase (A-N or A-B) meter. */
	SinglePhaseMeterFloat(211, "Single phase (A-N or A-B) meter"),

	/** Split single phase (A-B-N) meter. */
	SplitSinglePhaseMeterFLoat(212, "Split single phase (A-B-N) meter"),

	/** WYE connect 3-phase (ABCN) meter. */
	WyeConnectThreePhaseMeterFloat(213, "WYE connect 3-phase (ABCN) meter"),

	/** Delta connect 3-phase (ABC) meter. */
	DeltaConnectThreePhaseMeterFloat(214, "Delta connect 3-phase (ABC) meter");

	private final int id;
	private final String description;
	private final Class<? extends ModelAccessor> accessorType;

	private MeterModelId(int id, String description) {
		this(id, description, MeterModelAccessor.class);
	}

	private MeterModelId(int id, String description, Class<? extends ModelAccessor> accessorType) {
		this.id = id;
		this.description = description;
		this.accessorType = accessorType;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Class<? extends ModelAccessor> getModelAccessorType() {
		return accessorType;
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
