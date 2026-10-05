/* ==================================================================
 * StorageModelId.java - 5/10/2026 7:58:02 pm
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

import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * SunSpec energy storage model IDs (800 series).
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum StorageModelId implements ModelId {

	/** Energy storage base, deprecated by SunSpec. */
	EnergyStorageBase(801, "Energy storage base (deprecated)"),

	/** Battery base. */
	BatteryBase(802, "Battery base", BatteryBaseModelAccessor.class),

	/** Lithium-ion battery bank. */
	LithiumIonBank(803, "Lithium-ion battery bank"),

	/** Lithium-ion string. */
	LithiumIonString(804, "Lithium-ion string"),

	/** Lithium-ion module. */
	LithiumIonModule(805, "Lithium-ion module"),

	/** Flow battery. */
	FlowBattery(806, "Flow battery"),

	/** Flow battery string. */
	FlowBatteryString(807, "Flow battery string"),

	/** Flow battery module. */
	FlowBatteryModule(808, "Flow battery module"),

	/** Flow battery stack. */
	FlowBatteryStack(809, "Flow battery stack"),

	;

	private final int id;
	private final String description;
	private final Class<? extends ModelAccessor> accessorType;

	private StorageModelId(int id, String description) {
		this(id, description, ModelAccessor.class);
	}

	private StorageModelId(int id, String description, Class<? extends ModelAccessor> accessorType) {
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
	public static StorageModelId forId(int id) {
		for ( StorageModelId e : StorageModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
