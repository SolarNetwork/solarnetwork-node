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

	/** The model repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 6;

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
		return REPEATING_BLOCK_LENGTH;
	}

	@Override
	public List<Incline> getInclines() {
		final int count = getModelLength();
		final int baseAddr = getBlockAddress();
		final List<Incline> inclines = new ArrayList<>(count);
		final int propCount = InclinometerModelRegister.values().length;
		final Integer[] data = new Integer[propCount];
		for ( int i = 0; i < count; i += REPEATING_BLOCK_LENGTH ) {
			final int blockAddr = baseAddr + i;
			for ( int j = 0; j < propCount; j++ ) {
				data[j] = getIntegerValue(InclinometerModelRegister.values()[j], blockAddr);
			}
			inclines.add(new Incline(data));
		}
		return inclines;
	}

}
