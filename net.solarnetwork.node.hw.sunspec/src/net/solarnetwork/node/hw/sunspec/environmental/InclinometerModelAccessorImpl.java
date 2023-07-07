/* ==================================================================
 * InclinometerModelAccessorImpl.java - 8/07/2023 8:30:24 am
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
 * Implementatino of {@link InclinometerModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class InclinometerModelAccessorImpl extends BaseModelAccessor
		implements InclinometerModelAccessor {

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
	public InclinometerModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public InclinometerModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, EnvironmentalModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return 0;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return 6;
	}

	@Override
	public List<Incline> getInclines() {
		final int count = getModelLength();
		final int baseAddr = getBlockAddress();
		final List<Incline> inclines = new ArrayList<>(count);
		final Double[] data = new Double[3];
		for ( int i = 0; i < count; i += 6 ) {
			final int blockAddr = baseAddr + i;
			for ( int j = 0; j < 3; j++ ) {
				Number d = getIntegerValue(InclinometerModelRegister.values()[j], blockAddr);
				if ( d != null ) {
					data[j] = (d.doubleValue() / 100.);
				} else {
					data[j] = null;
				}
			}
			inclines.add(new Incline(data));
		}
		return inclines;
	}

}
