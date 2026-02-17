/* ==================================================================
 * StringCombinerAdvancedModelAccessorImpl.java - 10/09/2019 3:49:52 pm
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
 * Implementation of {@link StringCombinerAdvancedModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public class StringCombinerAdvancedModelAccessorImpl extends BaseModelAccessor
		implements StringCombinerAdvancedModelAccessor {

	/** The string combiner model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 20;

	/** The string combiner model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH_2 = 25;

	/** The inverter MPPT extension model module repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 14;

	/**
	 * The legacy inverter MPPT extension model module repeating block length.
	 */
	public static final int REPEATING_BLOCK_LENGTH_LEGACY = 13;

	private final int repeatingBlockLength;

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
	public StringCombinerAdvancedModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
		this.repeatingBlockLength = calculateRepeatingBlockInstanceLength();
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
	public StringCombinerAdvancedModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, StringCombinerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return StringCombinerModelId.BasicStringCombiner2 == getModelId() ? FIXED_BLOCK_LENGTH_2
				: FIXED_BLOCK_LENGTH;
	}

	private int calculateRepeatingBlockInstanceLength() {
		final int fixedLen = getFixedBlockLength();
		final int repeatingLen = getModelLength() - fixedLen;
		if ( repeatingLen % REPEATING_BLOCK_LENGTH == 0 ) {
			return REPEATING_BLOCK_LENGTH;
		}
		return REPEATING_BLOCK_LENGTH_LEGACY;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return this.repeatingBlockLength;
	}

	@Override
	public Float getDCCurrent() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcCurrent,
				StringCombinerAdvancedModelRegister.ScaleFactorDcCurrent);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Long getDCChargeDelivered() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcCharge,
				StringCombinerAdvancedModelRegister.ScaleFactorDcCharge);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Float getDCVoltage() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcVoltage,
				StringCombinerAdvancedModelRegister.ScaleFactorDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getTemperature() {
		return getFloatValue(StringCombinerAdvancedModelRegister.Temperature);
	}

	@Override
	public Integer getDCPower() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcPower,
				StringCombinerAdvancedModelRegister.ScaleFactorDcPower);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Long getDCEnergy() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcEnergy,
				StringCombinerAdvancedModelRegister.ScaleFactorDcEnergy);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Float getDCPerformanceRatio() {
		Float n = getFloatValue(StringCombinerAdvancedModelRegister.DcPerformanceRatio);
		return (n != null ? n.floatValue() / 100f : null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<DcInput> getDcInputs() {
		return (List) getAdvancedDcInputs();
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(StringCombinerAdvancedModelRegister.InputEventsBitmask,
				getBlockAddress());
		return StringCombinerModelEvent.forBitmask(n.longValue());
	}

	@Override
	public Set<ModelEvent> getVendorEvents() {
		Number n = getBitfield(StringCombinerAdvancedModelRegister.InputVendorEventsBitmask,
				getBlockAddress());
		return GenericModelEvent.forBitmask(n.longValue());
	}

	@Override
	public List<AdvancedDcInput> getAdvancedDcInputs() {
		Number n = getIntegerValue(StringCombinerAdvancedModelRegister.InputCount);
		final int count = (n != null ? n.intValue() : 0);
		if ( count < 1 ) {
			return Collections.emptyList();
		}
		List<AdvancedDcInput> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			result.add(new StringCombinerAdvancedDcInput(i));
		}
		return result;
	}

	private class StringCombinerAdvancedDcInput implements AdvancedDcInput {

		private final int index;

		private StringCombinerAdvancedDcInput(int index) {
			super();
			this.index = index;
		}

		@Override
		public Integer getInputId() {
			Number n = getData().getNumber(StringCombinerAdvancedModelRegister.InputId,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength);
			return (n != null ? n.intValue() : null);
		}

		@Override
		public Float getDCCurrent() {
			final StringCombinerAdvancedModelRegister scaleReg = (StringCombinerModelId.AdvancedStringCombiner2 == getModelId()
					? StringCombinerAdvancedModelRegister.ScaleFactorInputDcCurrent
					: StringCombinerAdvancedModelRegister.ScaleFactorDcCurrent);
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcCurrent, scaleReg,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength,
					getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Long getDCChargeDelivered() {
			final StringCombinerAdvancedModelRegister scaleReg = (StringCombinerModelId.AdvancedStringCombiner2 == getModelId()
					? StringCombinerAdvancedModelRegister.ScaleFactorInputDcCharge
					: StringCombinerAdvancedModelRegister.ScaleFactorInputDcCharge);
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcCharge, scaleReg,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength,
					getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public Set<ModelEvent> getEvents() {
			Number n = getBitfield(StringCombinerAdvancedModelRegister.InputEventsBitmask,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength);
			return StringCombinerModelEvent.forBitmask(n.longValue());
		}

		@Override
		public Set<ModelEvent> getVendorEvents() {
			Number n = getBitfield(StringCombinerAdvancedModelRegister.InputVendorEventsBitmask,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength);
			return GenericModelEvent.forBitmask(n.longValue());
		}

		@Override
		public Float getDCVoltage() {
			final StringCombinerAdvancedModelRegister scaleReg = (StringCombinerModelId.AdvancedStringCombiner2 == getModelId()
					? StringCombinerAdvancedModelRegister.ScaleFactorInputDcVoltage
					: StringCombinerAdvancedModelRegister.ScaleFactorDcVoltage);
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcCurrent, scaleReg,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength,
					getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Integer getDCPower() {
			final StringCombinerAdvancedModelRegister scaleReg = (StringCombinerModelId.AdvancedStringCombiner2 == getModelId()
					? StringCombinerAdvancedModelRegister.ScaleFactorInputDcPower
					: StringCombinerAdvancedModelRegister.ScaleFactorDcPower);
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcPower, scaleReg,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength,
					getBlockAddress());
			return (n != null ? n.intValue() : null);
		}

		@Override
		public Long getDCEnergy() {
			final StringCombinerAdvancedModelRegister scaleReg = (StringCombinerModelId.AdvancedStringCombiner2 == getModelId()
					? StringCombinerAdvancedModelRegister.ScaleFactorInputDcEnergy
					: StringCombinerAdvancedModelRegister.ScaleFactorDcEnergy);
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcEnergy, scaleReg,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength,
					getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public Float getDCPerformanceRatio() {
			Float n = getFloatValue(StringCombinerAdvancedModelRegister.DcPerformanceRatio,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength);
			return (n != null ? n.floatValue() / 100f : null);
		}

		@Override
		public Integer getModuleCount() {
			if ( repeatingBlockLength != REPEATING_BLOCK_LENGTH ) {
				return null;
			}
			Number n = getData().getNumber(StringCombinerAdvancedModelRegister.InputModuleCount,
					getBlockAddress() + getFixedBlockLength() + index * repeatingBlockLength);
			return (n != null ? n.intValue() : null);
		}

	}
}
