/* ==================================================================
 * DerModelId.java - 5/10/2026 8:27:22 am
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

import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec distributed energy resource (DER) model IDs.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerModelId implements ModelId {

	/** DER AC measurement. */
	AcMeasurement(701, "DER AC measurement", DerAcMeasurementModelAccessor.class),

	/** DER capacity. */
	Capacity(702, "DER capacity", DerCapacityModelAccessor.class),

	/** DER enter service. */
	EnterService(703, "DER enter service", DerEnterServiceModelAccessor.class),

	/** DER AC controls. */
	AcControls(704, "DER AC controls", DerAcControlsModelAccessor.class),

	/** DER volt-var. */
	VoltVar(705, "DER volt-var"),

	/** DER volt-watt. */
	VoltWatt(706, "DER volt-watt"),

	/** DER trip low voltage. */
	TripLowVoltage(707, "DER trip low voltage"),

	/** DER trip high voltage. */
	TripHighVoltage(708, "DER trip high voltage"),

	/** DER trip low frequency. */
	TripLowFrequency(709, "DER trip low frequency"),

	/** DER trip high frequency. */
	TripHighFrequency(710, "DER trip high frequency"),

	/** DER frequency droop. */
	FrequencyDroop(711, "DER frequency droop"),

	/** DER watt-var. */
	WattVar(712, "DER watt-var"),

	/** DER storage capacity. */
	StorageCapacity(713, "DER storage capacity", DerStorageCapacityModelAccessor.class),

	/** DER DC measurement. */
	DcMeasurement(714, "DER DC measurement", DerDcMeasurementModelAccessor.class),

	/** DER control. */
	Control(715, "DER control", DerControlModelAccessor.class),

	;

	private final int id;
	private final String description;
	private final Class<? extends ModelAccessor> accessorType;

	private DerModelId(int id, String description) {
		this(id, description, ModelAccessor.class);
	}

	private DerModelId(int id, String description, Class<? extends ModelAccessor> accessorType) {
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
	public static DerModelId forId(int id) {
		for ( DerModelId e : DerModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
