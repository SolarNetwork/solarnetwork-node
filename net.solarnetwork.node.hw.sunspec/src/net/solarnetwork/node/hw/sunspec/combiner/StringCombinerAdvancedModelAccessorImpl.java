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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link StringCombinerAdvancedModelAccessor}.
 *
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public class StringCombinerAdvancedModelAccessorImpl extends BaseModelAccessor
		implements StringCombinerAdvancedModelAccessor {

	/** The advanced string combiner (402) model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 20;

	/** The advanced string combiner v2 (404) model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH_2 = 25;

	/** The advanced string combiner model input repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 14;

	/**
	 * The legacy advanced string combiner model input repeating block length.
	 */
	public static final int REPEATING_BLOCK_LENGTH_LEGACY = 13;

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

	private boolean isVersion2() {
		return StringCombinerModelId.AdvancedStringCombiner2 == getModelId();
	}

	@Override
	public int getFixedBlockLength() {
		return isVersion2() ? FIXED_BLOCK_LENGTH_2 : FIXED_BLOCK_LENGTH;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This returns {@link #REPEATING_BLOCK_LENGTH_LEGACY} if the model length
	 * fits that length but not {@link #REPEATING_BLOCK_LENGTH}.
	 * </p>
	 */
	@Override
	public int getRepeatingBlockInstanceLength() {
		final int repeatingLen = getModelLength() - getFixedBlockLength();
		if ( repeatingLen % REPEATING_BLOCK_LENGTH != 0
				&& repeatingLen % REPEATING_BLOCK_LENGTH_LEGACY == 0 ) {
			return REPEATING_BLOCK_LENGTH_LEGACY;
		}
		return REPEATING_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Float getDCCurrent() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcCurrent,
				StringCombinerAdvancedModelRegister.ScaleFactorDcCurrent);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public @Nullable Long getDCChargeDelivered() {
		Number n = getScaledValue(
				isVersion2() ? StringCombinerAdvancedModelRegister.DcChargeV2
						: StringCombinerAdvancedModelRegister.DcCharge,
				StringCombinerAdvancedModelRegister.ScaleFactorDcCharge);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Float getDCVoltage() {
		Number n = getScaledValue(
				isVersion2() ? StringCombinerAdvancedModelRegister.DcVoltageV2
						: StringCombinerAdvancedModelRegister.DcVoltage,
				StringCombinerAdvancedModelRegister.ScaleFactorDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public @Nullable Float getTemperature() {
		return getFloatValue(StringCombinerAdvancedModelRegister.Temperature);
	}

	@Override
	public @Nullable Integer getDCPower() {
		Number n = getScaledValue(StringCombinerAdvancedModelRegister.DcPower,
				StringCombinerAdvancedModelRegister.ScaleFactorDcPower);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public @Nullable Long getDCEnergy() {
		Number n = getScaledValue(
				isVersion2() ? StringCombinerAdvancedModelRegister.DcEnergyV2
						: StringCombinerAdvancedModelRegister.DcEnergy,
				StringCombinerAdvancedModelRegister.ScaleFactorDcEnergy);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Float getDCPerformanceRatio() {
		Float n = getFloatValue(isVersion2() ? StringCombinerAdvancedModelRegister.DcPerformanceRatioV2
				: StringCombinerAdvancedModelRegister.DcPerformanceRatio);
		return (n != null ? n.floatValue() / 100f : null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<DcInput> getDcInputs() {
		return (List) getAdvancedDcInputs();
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(StringCombinerAdvancedModelRegister.EventsBitmask);
		return StringCombinerModelEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public Set<ModelEvent> getVendorEvents() {
		Number n = getBitfield(StringCombinerAdvancedModelRegister.VendorEventsBitmask);
		return GenericModelEvent.forBitmask(n != null ? n.longValue() : 0L);
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

		private int inputAddress() {
			return getBlockAddress() + getFixedBlockLength() + index * getRepeatingBlockInstanceLength();
		}

		@Override
		public @Nullable Integer getInputId() {
			return getIntegerValue(StringCombinerAdvancedModelRegister.InputId, inputAddress());
		}

		@Override
		public @Nullable Float getDCCurrent() {
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcCurrent,
					isVersion2() ? StringCombinerAdvancedModelRegister.ScaleFactorInputDcCurrent
							: StringCombinerAdvancedModelRegister.ScaleFactorDcCurrent,
					inputAddress(), getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public @Nullable Long getDCChargeDelivered() {
			Number n = isVersion2()
					? getScaledValue(StringCombinerAdvancedModelRegister.InputDcChargeV2,
							StringCombinerAdvancedModelRegister.ScaleFactorInputDcCharge, inputAddress(),
							getBlockAddress())
					: getScaledValue(StringCombinerAdvancedModelRegister.InputDcCharge,
							StringCombinerAdvancedModelRegister.ScaleFactorDcCharge, inputAddress(),
							getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public Set<ModelEvent> getEvents() {
			Number n = getBitfield(StringCombinerAdvancedModelRegister.InputEventsBitmask,
					inputAddress());
			return StringCombinerModelEvent.forBitmask(n != null ? n.longValue() : 0L);
		}

		@Override
		public Set<ModelEvent> getVendorEvents() {
			Number n = getBitfield(StringCombinerAdvancedModelRegister.InputVendorEventsBitmask,
					inputAddress());
			return GenericModelEvent.forBitmask(n != null ? n.longValue() : 0L);
		}

		@Override
		public @Nullable Float getDCVoltage() {
			Number n = isVersion2()
					? getScaledValue(StringCombinerAdvancedModelRegister.InputDcVoltageV2,
							StringCombinerAdvancedModelRegister.ScaleFactorInputDcVoltage,
							inputAddress(), getBlockAddress())
					: getScaledValue(StringCombinerAdvancedModelRegister.InputDcVoltage,
							StringCombinerAdvancedModelRegister.ScaleFactorDcVoltage, inputAddress(),
							getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public @Nullable Integer getDCPower() {
			// model 402 defines the input power scale factor as DCWh_SF
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcPower,
					isVersion2() ? StringCombinerAdvancedModelRegister.ScaleFactorInputDcPower
							: StringCombinerAdvancedModelRegister.ScaleFactorDcEnergy,
					inputAddress(), getBlockAddress());
			return (n != null ? n.intValue() : null);
		}

		@Override
		public @Nullable Long getDCEnergy() {
			if ( !isVersion2() ) {
				// model 402 does not define an input energy scale factor
				return getLongValue(StringCombinerAdvancedModelRegister.InputDcEnergy, inputAddress());
			}
			Number n = getScaledValue(StringCombinerAdvancedModelRegister.InputDcEnergyV2,
					StringCombinerAdvancedModelRegister.ScaleFactorInputDcEnergy, inputAddress(),
					getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public @Nullable Float getDCPerformanceRatio() {
			Float n = getFloatValue(StringCombinerAdvancedModelRegister.InputDcPerformanceRatio,
					inputAddress());
			return (n != null ? n.floatValue() / 100f : null);
		}

		@Override
		public @Nullable Integer getModuleCount() {
			if ( getRepeatingBlockInstanceLength() != REPEATING_BLOCK_LENGTH ) {
				return null;
			}
			return getIntegerValue(StringCombinerAdvancedModelRegister.InputModuleCount, inputAddress());
		}

	}
}
