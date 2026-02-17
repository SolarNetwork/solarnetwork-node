/* ==================================================================
 * InverterMpptExtensionModelAccessorImpl.java - 6/09/2019 5:24:30 pm
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

package net.solarnetwork.node.hw.sunspec.inverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.OperatingState;

/**
 * Data access object for an inverter MPPT extensions model.
 *
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public class InverterMpptExtensionModelAccessorImpl extends BaseModelAccessor
		implements InverterMpptExtensionModelAccessor {

	/** The inverter MPPT extension model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 8;

	/** The inverter MPPT extension model module repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 20;

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
	public InverterMpptExtensionModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link InverterModelId} class will be used as the {@code ModelId}
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
	public InverterMpptExtensionModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return REPEATING_BLOCK_LENGTH;
	}

	@Override
	public List<DcModule> getDcModules() {
		Integer n = getIntegerValue(InverterMpptExtensionModelRegister.ModuleCount);
		final int count = (n != null ? n.intValue() : 0);
		if ( count < 1 ) {
			return Collections.emptyList();
		}
		List<DcModule> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			result.add(new InverterMpptExtensionDcModule(i));
		}
		return result;
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(InverterMpptExtensionModelRegister.EventsBitmask, getBlockAddress());
		return InverterMpptExtensionModelEvent.forBitmask(n.longValue());
	}

	@Override
	public Integer getTimestampPeriod() {
		return getIntegerValue(InverterMpptExtensionModelRegister.TimestampPeriod);
	}

	private class InverterMpptExtensionDcModule implements DcModule {

		private final int index;

		private InverterMpptExtensionDcModule(int index) {
			super();
			this.index = index;
		}

		@Override
		public Integer getInputId() {
			return getIntegerValue(InverterMpptExtensionModelRegister.ModuleInputId,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH);
		}

		@Override
		public String getInputName() {
			return getData().getLatin1String(InverterMpptExtensionModelRegister.ModuleName,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH, true);
		}

		@Override
		public Float getDCCurrent() {
			Number n = getScaledValue(InverterMpptExtensionModelRegister.ModuleDcCurrent,
					InverterMpptExtensionModelRegister.ScaleFactorDcCurrent,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH,
					getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Float getDCVoltage() {
			Number n = getScaledValue(InverterMpptExtensionModelRegister.ModuleDcVoltage,
					InverterMpptExtensionModelRegister.ScaleFactorDcVoltage,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH,
					getBlockAddress());
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Integer getDCPower() {
			Number n = getScaledValue(InverterMpptExtensionModelRegister.ModuleDcPower,
					InverterMpptExtensionModelRegister.ScaleFactorDcVoltage,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH,
					getBlockAddress());
			return (n != null ? n.intValue() : null);
		}

		@Override
		public Long getDCEnergyDelivered() {
			Number n = getScaledValue(InverterMpptExtensionModelRegister.ModuleLifetimeEnergy,
					InverterMpptExtensionModelRegister.ScaleFactorDcEnergy,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH,
					getBlockAddress());
			return (n != null ? n.longValue() : null);
		}

		@Override
		public Long getDataTimestamp() {
			return getLongValue(InverterMpptExtensionModelRegister.ModuleTimestamp,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH);
		}

		@Override
		public Float getTemperature() {
			return getFloatValue(InverterMpptExtensionModelRegister.ModuleTemperature,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH);
		}

		@Override
		public OperatingState getOperatingState() {
			Integer n = getIntegerValue(InverterMpptExtensionModelRegister.ModuleOperatingState,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH);
			if ( n == null ) {
				return null;
			}
			return InverterOperatingState.forCode(n.intValue());
		}

		@Override
		public Set<ModelEvent> getEvents() {
			Number n = getBitfield(InverterMpptExtensionModelRegister.ModuleEventsBitmask,
					getBlockAddress() + getFixedBlockLength() + index * REPEATING_BLOCK_LENGTH);
			return InverterMpptExtensionModelEvent.forBitmask(n.longValue());
		}

	}

}
