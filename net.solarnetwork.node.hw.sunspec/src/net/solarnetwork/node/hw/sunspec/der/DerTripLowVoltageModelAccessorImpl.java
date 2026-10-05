/* ==================================================================
 * DerTripLowVoltageModelAccessorImpl.java - 5/10/2026 6:24:51 pm
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

import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link DerTripLowVoltageModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerTripLowVoltageModelAccessorImpl extends BaseDerTripModelAccessor
		implements DerTripLowVoltageModelAccessor {

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
	public DerTripLowVoltageModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId, DerTripModelRegister.PointVoltage,
				DerTripModelRegister.ScaleFactorVoltage, DerTripModelRegister.PointVoltageTime);
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
	public DerTripLowVoltageModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

}
