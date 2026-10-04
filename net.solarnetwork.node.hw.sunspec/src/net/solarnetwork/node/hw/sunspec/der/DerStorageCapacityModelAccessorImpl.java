/* ==================================================================
 * DerStorageCapacityModelAccessorImpl.java - 5/10/2026 10:18:41 am
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
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link DerStorageCapacityModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerStorageCapacityModelAccessorImpl extends BaseModelAccessor
		implements DerStorageCapacityModelAccessor {

	/** The DER storage capacity model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 7;

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public DerStorageCapacityModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link DerModelId} class will be used as the {@code ModelId}
	 * instance.
	 * </p>
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public DerStorageCapacityModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Long getEnergyRating() {
		return getScaledLongValue(DerStorageCapacityModelRegister.EnergyRating,
				DerStorageCapacityModelRegister.ScaleFactorEnergy);
	}

	@Override
	public @Nullable Long getEnergyAvailable() {
		return getScaledLongValue(DerStorageCapacityModelRegister.EnergyAvailable,
				DerStorageCapacityModelRegister.ScaleFactorEnergy);
	}

	@Override
	public @Nullable Float getStateOfCharge() {
		return getScaledFloatValue(DerStorageCapacityModelRegister.StateOfCharge,
				DerStorageCapacityModelRegister.ScaleFactorPercent);
	}

	@Override
	public @Nullable Float getStateOfHealth() {
		return getScaledFloatValue(DerStorageCapacityModelRegister.StateOfHealth,
				DerStorageCapacityModelRegister.ScaleFactorPercent);
	}

	@Override
	public @Nullable DerStorageStatus getStorageStatus() {
		return getCodedValue(DerStorageCapacityModelRegister.Status, DerStorageStatus.class);
	}

}
