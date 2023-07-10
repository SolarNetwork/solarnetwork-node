/* ==================================================================
 * BackOfModuleTemperatureAccessorImpl.java - 5/07/2023 10:34:55 am
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

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link BomTemperatureModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class BomTemperatureModelAccessorImpl extends BaseModelAccessor
		implements BomTemperatureModelAccessor {

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
	public BomTemperatureModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public BomTemperatureModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, EnvironmentalModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return 0;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return 1;
	}

	@Override
	public List<Float> getBackOfModuleTemperatures() {
		final int count = getModelLength();
		final int baseAddr = getBlockAddress();
		final List<Float> temps = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			Number t = getIntegerValue(BomTemperatureModelRegister.TemperatureBOM, baseAddr + i);
			if ( t != null ) {
				temps.add(t.floatValue() / 10f);
			} else {
				temps.add(null);
			}
		}
		return temps;
	}

}
