/* ==================================================================
 * StringCombinerModelAccessorImpl.java - 10/09/2019 7:03:23 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.combiner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Data access object for an string combiner model.
 *
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public class StringCombinerModelAccessorImpl extends BaseModelAccessor
		implements StringCombinerModelAccessor {

	/** The basic string combiner (401) model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 14;

	/** The basic string combiner v2 (403) model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH_2 = 16;

	/** The basic string combiner model input repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 8;

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
	public StringCombinerModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link StringCombinerModelId} class will be used as the
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
	public StringCombinerModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, StringCombinerModelId.forId(modelId));
	}

	private boolean isVersion2() {
		return StringCombinerModelId.BasicStringCombiner2 == getModelId();
	}

	@Override
	public int getFixedBlockLength() {
		return isVersion2() ? FIXED_BLOCK_LENGTH_2 : FIXED_BLOCK_LENGTH;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return REPEATING_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Float getDCCurrent() {
		Number n = getScaledValue(StringCombinerModelRegister.DcCurrent,
				StringCombinerModelRegister.ScaleFactorDcCurrent);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public @Nullable Long getDCChargeDelivered() {
		Number n = getScaledValue(
				isVersion2() ? StringCombinerModelRegister.DcChargeV2
						: StringCombinerModelRegister.DcCharge,
				StringCombinerModelRegister.ScaleFactorDcCharge);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Float getDCVoltage() {
		Number n = getScaledValue(
				isVersion2() ? StringCombinerModelRegister.DcVoltageV2
						: StringCombinerModelRegister.DcVoltage,
				StringCombinerModelRegister.ScaleFactorDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public @Nullable Float getTemperature() {
		return getFloatValue(StringCombinerModelRegister.Temperature);
	}

	@Override
	public List<DcInput> getDcInputs() {
		Integer n = getIntegerValue(StringCombinerModelRegister.InputCount);
		final int count = (n != null ? n.intValue() : 0);
		if ( count < 1 ) {
			return Collections.emptyList();
		}
		List<DcInput> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			result.add(new StringCombinerDcInput(i));
		}
		return result;
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(StringCombinerModelRegister.EventsBitmask);
		return StringCombinerModelEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public Set<ModelEvent> getVendorEvents() {
		Number n = getBitfield(StringCombinerModelRegister.VendorEventsBitmask);
		return GenericModelEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	private class StringCombinerDcInput implements DcInput {

		private final int index;

		private StringCombinerDcInput(int index) {
			super();
			this.index = index;
		}

		private int inputAddress() {
			return getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH;
		}

		@Override
		public @Nullable Integer getInputId() {
			return getIntegerValue(StringCombinerModelRegister.InputId, inputAddress());
		}

		@Override
		public @Nullable Float getDCCurrent() {
			Number n = getScaledValue(StringCombinerModelRegister.InputDcCurrent,
					isVersion2() ? StringCombinerModelRegister.ScaleFactorInputDcCurrent
							: StringCombinerModelRegister.ScaleFactorDcCurrent,
					inputAddress(), getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public @Nullable Long getDCChargeDelivered() {
			Number n = isVersion2()
					? getScaledValue(StringCombinerModelRegister.InputDcChargeV2,
							StringCombinerModelRegister.ScaleFactorInputDcCharge, inputAddress(),
							getBlockAddress())
					: getScaledValue(StringCombinerModelRegister.InputDcCharge,
							StringCombinerModelRegister.ScaleFactorDcCharge, inputAddress(),
							getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public Set<ModelEvent> getEvents() {
			Number n = getBitfield(StringCombinerModelRegister.InputEventsBitmask, inputAddress());
			return StringCombinerModelEvent.forBitmask(n != null ? n.longValue() : 0L);
		}

		@Override
		public Set<ModelEvent> getVendorEvents() {
			Number n = getBitfield(StringCombinerModelRegister.InputVendorEventsBitmask, inputAddress());
			return GenericModelEvent.forBitmask(n != null ? n.longValue() : 0L);
		}

	}
}
