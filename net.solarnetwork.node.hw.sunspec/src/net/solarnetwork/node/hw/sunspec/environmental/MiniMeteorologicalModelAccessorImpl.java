/* ==================================================================
 * MiniMeteorologicalModelAccessorImpl.java - 10/07/2023 7:16:18 am
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

import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link MiniMeteorologicalModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class MiniMeteorologicalModelAccessorImpl extends BaseModelAccessor
		implements MiniMeteorologicalModelAccessor {

	/** The irradiance model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 4;

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
	public MiniMeteorologicalModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@link EnvironmentalModelId} class will be used as the
	 * {@code ModelId} instance.
	 * </p>
	 * 
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public MiniMeteorologicalModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, EnvironmentalModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public Integer getGlobalHorizontalIrradiance() {
		return getIntegerValue(MiniMeteorologicalModelRegister.IrradianceGH, getBlockAddress());
	}

	@Override
	public Float getBackOfModuleTemperature() {
		Float f = getFloatValue(MiniMeteorologicalModelRegister.TemperatureBOM, getBlockAddress());
		return (f != null ? f.floatValue() / 10f : null);
	}

	@Override
	public Float getAmbientTemperature() {
		Float f = getFloatValue(MiniMeteorologicalModelRegister.TemperatureAir, getBlockAddress());
		return (f != null ? f.floatValue() / 10f : null);
	}

	@Override
	public Integer getWindSpeed() {
		return getIntegerValue(MiniMeteorologicalModelRegister.WindSpeed, getBlockAddress());
	}

}
