/* ==================================================================
 * EnergyStorageDatum.java - 16/02/2016 7:37:54 pm
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.datum;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;

/**
 * Standardized API for energy storage system related datum to implement.
 *
 * @author matt
 * @version 1.2
 */
public interface EnergyStorageDatum
		extends net.solarnetwork.domain.datum.EnergyStorageDatum, MutableNodeDatum {

	/**
	 * Set the percentage of energy capacity available in the storage.
	 *
	 * @param value
	 *        the available energy as a percentage of the total capacity of the
	 *        storage
	 */
	default void setAvailableEnergyPercentage(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, STATE_OF_CHARGE_PERCENTAGE_KEY, value);
	}

	/**
	 * Set a percentage of storage "health" in terms of practical total capacity
	 * right now versus theoretical total capacity when the storage was
	 * manufactured.
	 *
	 * @param value
	 *        the total energy capacity now as a percentage of the theoretical
	 *        total capacity of the storage when manufactured
	 * @since 1.1
	 */
	default void setStateOfHealthPercentage(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, STATE_OF_HEALTH_PERCENTAGE_KEY, value);
	}

	/**
	 * Get the available energy of the storage system, in Wh.
	 *
	 * @param value
	 *        the available energy of the storage
	 */
	default void setAvailableEnergy(Long value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, AVAILABLE_WATT_HOURS_KEY, value);
	}

	/**
	 * Set the energy capacity of the storage system, in Wh.
	 *
	 * @param value
	 *        the energy capacity of the storage
	 * @since 2.1
	 */
	default void setEnergyCapacity(Long value) {
		asMutableSampleOperations().putSampleValue(Status, CAPACITY_WATT_HOURS_KEY, value);
	}

}
