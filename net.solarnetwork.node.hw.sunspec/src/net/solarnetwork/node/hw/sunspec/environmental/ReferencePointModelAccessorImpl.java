/* ==================================================================
 * ReferencePointModelAccessorImpl.java - 9/07/2023 4:36:56 pm
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
 * Implementation of {@link ReferencePointModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class ReferencePointModelAccessorImpl extends BaseModelAccessor
		implements ReferencePointModelAccessor {

	/** The model repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 7;

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
	public ReferencePointModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public ReferencePointModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
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
	public List<ReferencePoint> getReferencePoints() {
		final int count = getModelLength();
		final int baseAddr = getBlockAddress();
		final List<ReferencePoint> points = new ArrayList<>(count);
		final int propCount = ReferencePointModelRegister.values().length;
		final Integer[] data = new Integer[propCount];
		for ( int i = 0; i < count; i += REPEATING_BLOCK_LENGTH ) {
			final int blockAddr = baseAddr + i;
			for ( int j = 0; j < propCount; j++ ) {
				data[j] = getIntegerValue(ReferencePointModelRegister.values()[j], blockAddr);
			}
			points.add(new ReferencePoint(data));
		}
		return points;
	}

}
