/* ==================================================================
 * InverterModelId.java - 5/10/2018 4:21:00 PM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec inverter model IDs.
 * 
 * @author matt
 * @version 1.1
 */
public enum InverterModelId implements ModelId {

	SinglePhaseInverterInteger(101, "Single phase inverter"),

	SplitPhaseInverterInteger(102, "Split phase inverter"),

	ThreePhaseInverterInteger(103, "3-phase inverter"),

	MultipleMpptInverterExtension(
			160,
			"Multiple MPPT Inverter Extension Model",
			InverterMpptExtensionModelAccessor.class);

	private final int id;
	private final String description;
	private final Class<? extends ModelAccessor> accessorType;

	private InverterModelId(int id, String description) {
		this(id, description, InverterModelAccessor.class);
	}

	private InverterModelId(int id, String description, Class<? extends ModelAccessor> accessorType) {
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
	public static InverterModelId forId(int id) {
		for ( InverterModelId e : InverterModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
