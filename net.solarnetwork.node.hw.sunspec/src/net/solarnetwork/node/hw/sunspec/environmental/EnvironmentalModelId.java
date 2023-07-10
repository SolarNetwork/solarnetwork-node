/* ==================================================================
 * EnvironmentalModelId.java - 5/07/2023 8:24:25 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec environmental model IDs.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public enum EnvironmentalModelId implements ModelId {

	/** Irradiance. */
	Irradiance(302, "Irradiance", IrradianceModelAccessor.class),

	/** Back of module temperature. */
	BackOfModuleTemperature(303, "Back of module temperature", BomTemperatureModelAccessor.class),

	/** Inclinometer. */
	Inclinometer(304, "Inclinometer", InclinometerModelAccessor.class),

	/** Global positioning system. */
	GPS(305, "GPS", GpsModelAccessor.class),

	/** Reference point. */
	ReferencePoint(306, "Reference Point", ReferencePointModelAccessor.class),

	/** Base meteorolgical. */
	BaseMeteorolgical(307, "Base meteorolgical", MeteorologicalModelAccessor.class),

	/** Mini meteorolgical. */
	MiniMeteorolgical(308, "Mini meteorolgical", MiniMeteorologicalModelAccessor.class),

	;

	private final int id;
	private final String description;
	private final Class<? extends ModelAccessor> accessorType;

	private EnvironmentalModelId(int id, String description,
			Class<? extends ModelAccessor> accessorType) {
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
	public static EnvironmentalModelId forId(int id) {
		for ( EnvironmentalModelId e : EnvironmentalModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
