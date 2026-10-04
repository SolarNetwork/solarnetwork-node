/* ==================================================================
 * DerStorageCapacityModelAccessor.java - 5/10/2026 10:18:41 am
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

import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing SunSpec DER storage capacity model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>713</b>.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerStorageCapacityModelAccessor extends ModelAccessor {

	/**
	 * Get the energy rating of the storage.
	 *
	 * @return the energy rating, in Wh, or {@code null} if not available
	 */
	@Nullable
	Long getEnergyRating();

	/**
	 * Get the energy available in the storage.
	 *
	 * <p>
	 * SunSpec defines this as the energy rating multiplied by the state of
	 * charge and the state of health.
	 * </p>
	 *
	 * @return the energy available, in Wh, or {@code null} if not available
	 */
	@Nullable
	Long getEnergyAvailable();

	/**
	 * Get the state of charge.
	 *
	 * <p>
	 * SunSpec defines the state of charge as {@literal 0} for a DER without
	 * storage capabilities.
	 * </p>
	 *
	 * @return the state of charge, as a percentage (0 - 100), or {@code null}
	 *         if not available
	 */
	@Nullable
	Float getStateOfCharge();

	/**
	 * Get the state of health.
	 *
	 * @return the state of health, as a percentage (0 - 100), or {@code null}
	 *         if not available
	 */
	@Nullable
	Float getStateOfHealth();

	/**
	 * Get the storage status.
	 *
	 * @return the status, or {@code null} if not available
	 */
	@Nullable
	DerStorageStatus getStorageStatus();

}
