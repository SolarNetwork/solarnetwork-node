/* ==================================================================
 * LithiumIonBankModelAccessorImpl.java - 5/10/2026 9:05:44 pm
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

package net.solarnetwork.node.hw.sunspec.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link LithiumIonBankModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class LithiumIonBankModelAccessorImpl extends BaseModelAccessor
		implements LithiumIonBankModelAccessor {

	/** The lithium-ion battery bank model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 26;

	/** The lithium-ion battery bank model string repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 32;

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
	public LithiumIonBankModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link StorageModelId} class will be used as the {@code ModelId}
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
	public LithiumIonBankModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, StorageModelId.forId(modelId));
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
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.range(LithiumIonBankModelRegister.NumberOfStrings,
				LithiumIonBankModelRegister.ScaleFactorVoltage);
	}

	@Override
	protected Collection<? extends ModbusReference> getRepeatingBlockRegisters() {
		return EnumSet.range(LithiumIonBankModelRegister.StringModuleCount,
				LithiumIonBankModelRegister.StringConnectOperation);
	}

	@Override
	public @Nullable Integer getStringCount() {
		return getIntegerValue(LithiumIonBankModelRegister.NumberOfStrings);
	}

	@Override
	public @Nullable Integer getConnectedStringCount() {
		return getIntegerValue(LithiumIonBankModelRegister.NumberOfConnectedStrings);
	}

	@Override
	public @Nullable Float getMaximumModuleTemperature() {
		return getScaledFloatValue(LithiumIonBankModelRegister.MaximumModuleTemperature,
				LithiumIonBankModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	public @Nullable Integer getMaximumModuleTemperatureStringIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MaximumModuleTemperatureStringIndex);
	}

	@Override
	public @Nullable Integer getMaximumModuleTemperatureModuleIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MaximumModuleTemperatureModuleIndex);
	}

	@Override
	public @Nullable Float getMinimumModuleTemperature() {
		return getScaledFloatValue(LithiumIonBankModelRegister.MinimumModuleTemperature,
				LithiumIonBankModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	public @Nullable Integer getMinimumModuleTemperatureStringIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MinimumModuleTemperatureStringIndex);
	}

	@Override
	public @Nullable Integer getMinimumModuleTemperatureModuleIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MinimumModuleTemperatureModuleIndex);
	}

	@Override
	public @Nullable Float getAverageModuleTemperature() {
		return getScaledFloatValue(LithiumIonBankModelRegister.AverageModuleTemperature,
				LithiumIonBankModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	public @Nullable Float getMaximumStringVoltage() {
		return getScaledFloatValue(LithiumIonBankModelRegister.MaximumStringVoltage,
				LithiumIonBankModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Integer getMaximumStringVoltageStringIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MaximumStringVoltageStringIndex);
	}

	@Override
	public @Nullable Float getMinimumStringVoltage() {
		return getScaledFloatValue(LithiumIonBankModelRegister.MinimumStringVoltage,
				LithiumIonBankModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Integer getMinimumStringVoltageStringIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MinimumStringVoltageStringIndex);
	}

	@Override
	public @Nullable Float getAverageStringVoltage() {
		return getScaledFloatValue(LithiumIonBankModelRegister.AverageStringVoltage,
				LithiumIonBankModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getMaximumStringCurrent() {
		return getScaledFloatValue(LithiumIonBankModelRegister.MaximumStringCurrent,
				LithiumIonBankModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Integer getMaximumStringCurrentStringIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MaximumStringCurrentStringIndex);
	}

	@Override
	public @Nullable Float getMinimumStringCurrent() {
		return getScaledFloatValue(LithiumIonBankModelRegister.MinimumStringCurrent,
				LithiumIonBankModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Integer getMinimumStringCurrentStringIndex() {
		return getIntegerValue(LithiumIonBankModelRegister.MinimumStringCurrentStringIndex);
	}

	@Override
	public @Nullable Float getAverageStringCurrent() {
		return getScaledFloatValue(LithiumIonBankModelRegister.AverageStringCurrent,
				LithiumIonBankModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Integer getBalancingCellCount() {
		return getIntegerValue(LithiumIonBankModelRegister.BalancingCellCount);
	}

	@Override
	public List<BatteryString> getStrings() {
		final int instanceCount = getRepeatingBlockInstanceCount();
		final Integer stringCount = getStringCount();
		final int count = (stringCount != null ? Math.min(stringCount, instanceCount) : instanceCount);
		if ( count < 1 ) {
			return List.of();
		}
		final List<BatteryString> result = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			result.add(new BatteryStringImpl(i));
		}
		return result;
	}

	private final class BatteryStringImpl implements BatteryString {

		private final int index;
		private final int groupAddress;

		private BatteryStringImpl(int index) {
			super();
			this.index = index;
			this.groupAddress = getBlockAddress() + FIXED_BLOCK_LENGTH
					+ (index - 1) * REPEATING_BLOCK_LENGTH;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public @Nullable Integer getModuleCount() {
			return getIntegerValue(LithiumIonBankModelRegister.StringModuleCount, groupAddress);
		}

		@Override
		public Set<BatteryConnectionStatus> getStatus() {
			return getBitmaskableValues(LithiumIonBankModelRegister.StringStatus, groupAddress,
					BatteryConnectionStatus.class);
		}

		@Override
		public @Nullable BatteryConnectionFailure getConnectionFailure() {
			return getCodedValue(LithiumIonBankModelRegister.StringConnectionFailure, groupAddress,
					BatteryConnectionFailure.class);
		}

		@Override
		public @Nullable Float getStateOfCharge() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringStateOfCharge,
					LithiumIonBankModelRegister.ScaleFactorStateOfCharge, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Float getStateOfHealth() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringStateOfHealth,
					LithiumIonBankModelRegister.ScaleFactorStateOfHealth, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Float getDCCurrent() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringDcCurrent,
					LithiumIonBankModelRegister.ScaleFactorCurrent, groupAddress, getBlockAddress());
		}

		@Override
		public @Nullable Float getMaximumCellVoltage() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringMaximumCellVoltage,
					LithiumIonBankModelRegister.ScaleFactorCellVoltage, groupAddress, getBlockAddress());
		}

		@Override
		public @Nullable Integer getMaximumCellVoltageModuleIndex() {
			return getIntegerValue(LithiumIonBankModelRegister.StringMaximumCellVoltageModuleIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getMinimumCellVoltage() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringMinimumCellVoltage,
					LithiumIonBankModelRegister.ScaleFactorCellVoltage, groupAddress, getBlockAddress());
		}

		@Override
		public @Nullable Integer getMinimumCellVoltageModuleIndex() {
			return getIntegerValue(LithiumIonBankModelRegister.StringMinimumCellVoltageModuleIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getAverageCellVoltage() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringAverageCellVoltage,
					LithiumIonBankModelRegister.ScaleFactorCellVoltage, groupAddress, getBlockAddress());
		}

		@Override
		public @Nullable Float getMaximumModuleTemperature() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringMaximumModuleTemperature,
					LithiumIonBankModelRegister.ScaleFactorModuleTemperature, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Integer getMaximumModuleTemperatureModuleIndex() {
			return getIntegerValue(LithiumIonBankModelRegister.StringMaximumModuleTemperatureModuleIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getMinimumModuleTemperature() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringMinimumModuleTemperature,
					LithiumIonBankModelRegister.ScaleFactorModuleTemperature, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Integer getMinimumModuleTemperatureModuleIndex() {
			return getIntegerValue(LithiumIonBankModelRegister.StringMinimumModuleTemperatureModuleIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getAverageModuleTemperature() {
			return getScaledFloatValue(LithiumIonBankModelRegister.StringAverageModuleTemperature,
					LithiumIonBankModelRegister.ScaleFactorModuleTemperature, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable BatteryDisabledReason getDisabledReason() {
			return getCodedValue(LithiumIonBankModelRegister.StringDisabledReason, groupAddress,
					BatteryDisabledReason.class);
		}

		@Override
		public Set<Integer> getClosedContactors() {
			return getBitfieldIndexes(LithiumIonBankModelRegister.StringContactorStatus, groupAddress);
		}

		@Override
		public Set<? extends ModelEvent> getEvents() {
			Number n = getBitfield(LithiumIonBankModelRegister.StringEventsBitmask, groupAddress);
			return LithiumIonStringEvent.forBitmask(n != null ? n.longValue() : 0L);
		}

		@Override
		public BitSet getVendorEvents() {
			return getBitfieldBits(groupAddress, LithiumIonBankModelRegister.StringVendorEventsBitmask,
					LithiumIonBankModelRegister.StringVendorEvents2Bitmask);
		}

		@Override
		public @Nullable BatteryEnableOperation getEnableOperation() {
			return getCodedValue(LithiumIonBankModelRegister.StringEnableOperation, groupAddress,
					BatteryEnableOperation.class);
		}

		@Override
		public void setEnableOperation(ModbusConnection conn, BatteryEnableOperation operation)
				throws IOException {
			writeValue(conn, LithiumIonBankModelRegister.StringEnableOperation, groupAddress,
					operation.getCode());
		}

		@Override
		public @Nullable BatteryOperation getConnectOperation() {
			return getCodedValue(LithiumIonBankModelRegister.StringConnectOperation, groupAddress,
					BatteryOperation.class);
		}

		@Override
		public void setConnectOperation(ModbusConnection conn, BatteryOperation operation)
				throws IOException {
			writeValue(conn, LithiumIonBankModelRegister.StringConnectOperation, groupAddress,
					operation.getCode());
		}

	}

}
