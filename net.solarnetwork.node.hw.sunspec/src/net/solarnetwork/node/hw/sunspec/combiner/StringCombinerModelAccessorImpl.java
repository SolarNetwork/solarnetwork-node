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
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Data access object for an string combiner model.
 *
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public class StringCombinerModelAccessorImpl extends BaseModelAccessor
		implements StringCombinerModelAccessor {

	/** The string combiner model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 14;

	/** The string combiner model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH_2 = 16;

	/** The inverter MPPT extension model module repeating block length. */
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

	@Override
	public int getFixedBlockLength() {
		return StringCombinerModelId.BasicStringCombiner2 == getModelId() ? FIXED_BLOCK_LENGTH_2
				: FIXED_BLOCK_LENGTH;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return REPEATING_BLOCK_LENGTH;
	}

	@Override
	public Float getDCCurrent() {
		Number n = getScaledValue(StringCombinerModelRegister.DcCurrent,
				StringCombinerModelRegister.ScaleFactorDcCurrent);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Long getDCChargeDelivered() {
		Number n = getScaledValue(StringCombinerModelRegister.DcCharge,
				StringCombinerModelRegister.ScaleFactorDcCharge);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Float getDCVoltage() {
		Number n = getScaledValue(StringCombinerModelRegister.DcVoltage,
				StringCombinerModelRegister.ScaleFactorDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getTemperature() {
		Number n = getData().getNumber(StringCombinerModelRegister.Temperature, getBlockAddress());
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public List<DcInput> getDcInputs() {
		Number n = getData().getNumber(StringCombinerModelRegister.InputCount, getBlockAddress());
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
		Number n = getBitfield(StringCombinerModelRegister.InputEventsBitmask, getBlockAddress());
		return StringCombinerModelEvent.forBitmask(n.longValue());
	}

	@Override
	public Set<ModelEvent> getVendorEvents() {
		Number n = getBitfield(StringCombinerModelRegister.InputVendorEventsBitmask, getBlockAddress());
		return GenericModelEvent.forBitmask(n.longValue());
	}

	private class StringCombinerDcInput implements DcInput {

		private final int index;

		private StringCombinerDcInput(int index) {
			super();
			this.index = index;
		}

		@Override
		public Integer getInputId() {
			Number n = getData().getNumber(StringCombinerModelRegister.InputId,
					getBlockAddress() + index * REPEATING_BLOCK_LENGTH);
			return (n != null ? n.intValue() : null);
		}

		@Override
		public Float getDCCurrent() {
			final StringCombinerModelRegister scaleReg = (StringCombinerModelId.BasicStringCombiner2 == getModelId()
					? StringCombinerModelRegister.ScaleFactorInputDcCurrent
					: StringCombinerModelRegister.ScaleFactorDcCurrent);
			Number n = getScaledValue(StringCombinerModelRegister.InputDcCurrent, scaleReg,
					getBlockAddress() + index * REPEATING_BLOCK_LENGTH, getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Long getDCChargeDelivered() {
			final StringCombinerModelRegister scaleReg = (StringCombinerModelId.BasicStringCombiner2 == getModelId()
					? StringCombinerModelRegister.ScaleFactorInputDcCharge
					: StringCombinerModelRegister.ScaleFactorInputDcCharge);
			Number n = getScaledValue(StringCombinerModelRegister.InputDcCharge, scaleReg,
					getBlockAddress() + index * REPEATING_BLOCK_LENGTH, getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public Set<ModelEvent> getEvents() {
			Number n = getBitfield(StringCombinerModelRegister.InputEventsBitmask,
					getBlockAddress() + index * REPEATING_BLOCK_LENGTH);
			return StringCombinerModelEvent.forBitmask(n.longValue());
		}

		@Override
		public Set<ModelEvent> getVendorEvents() {
			Number n = getBitfield(StringCombinerModelRegister.InputVendorEventsBitmask,
					getBlockAddress() + index * REPEATING_BLOCK_LENGTH);
			return GenericModelEvent.forBitmask(n.longValue());
		}

	}
}
