/* ==================================================================
 * LithiumIonStringModelAccessorImpl.java - 5/10/2026 9:05:44 pm
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
 * Implementation of {@link LithiumIonStringModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class LithiumIonStringModelAccessorImpl extends BaseModelAccessor
		implements LithiumIonStringModelAccessor {

	/** The lithium-ion string model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 46;

	/** The lithium-ion string model module repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 16;

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
	public LithiumIonStringModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public LithiumIonStringModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
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
		return EnumSet.range(LithiumIonStringModelRegister.StringIndex,
				LithiumIonStringModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	protected Collection<? extends ModbusReference> getRepeatingBlockRegisters() {
		return EnumSet.range(LithiumIonStringModelRegister.ModuleCellCount,
				LithiumIonStringModelRegister.ModuleAverageCellTemperature);
	}

	@Override
	public @Nullable Integer getStringIndex() {
		return getIntegerValue(LithiumIonStringModelRegister.StringIndex);
	}

	@Override
	public @Nullable Integer getModuleCount() {
		return getIntegerValue(LithiumIonStringModelRegister.NumberOfModules);
	}

	@Override
	public Set<BatteryConnectionStatus> getStatus() {
		return getBitmaskableValues(LithiumIonStringModelRegister.Status, BatteryConnectionStatus.class);
	}

	@Override
	public @Nullable BatteryConnectionFailure getConnectionFailure() {
		return getCodedValue(LithiumIonStringModelRegister.ConnectionFailure,
				BatteryConnectionFailure.class);
	}

	@Override
	public @Nullable Integer getBalancingCellCount() {
		return getIntegerValue(LithiumIonStringModelRegister.BalancingCellCount);
	}

	@Override
	public @Nullable Float getStateOfCharge() {
		return getScaledFloatValue(LithiumIonStringModelRegister.StateOfCharge,
				LithiumIonStringModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public @Nullable Float getDepthOfDischarge() {
		return getScaledFloatValue(LithiumIonStringModelRegister.DepthOfDischarge,
				LithiumIonStringModelRegister.ScaleFactorDepthOfDischarge);
	}

	@Override
	public @Nullable Long getCycleCount() {
		return getLongValue(LithiumIonStringModelRegister.CycleCount);
	}

	@Override
	public @Nullable Float getStateOfHealth() {
		return getScaledFloatValue(LithiumIonStringModelRegister.StateOfHealth,
				LithiumIonStringModelRegister.ScaleFactorStateOfHealth);
	}

	@Override
	public @Nullable Float getDCCurrent() {
		return getScaledFloatValue(LithiumIonStringModelRegister.DcCurrent,
				LithiumIonStringModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Float getDCVoltage() {
		return getScaledFloatValue(LithiumIonStringModelRegister.DcVoltage,
				LithiumIonStringModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getMaximumCellVoltage() {
		return getScaledFloatValue(LithiumIonStringModelRegister.MaximumCellVoltage,
				LithiumIonStringModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Integer getMaximumCellVoltageModuleIndex() {
		return getIntegerValue(LithiumIonStringModelRegister.MaximumCellVoltageModuleIndex);
	}

	@Override
	public @Nullable Float getMinimumCellVoltage() {
		return getScaledFloatValue(LithiumIonStringModelRegister.MinimumCellVoltage,
				LithiumIonStringModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Integer getMinimumCellVoltageModuleIndex() {
		return getIntegerValue(LithiumIonStringModelRegister.MinimumCellVoltageModuleIndex);
	}

	@Override
	public @Nullable Float getAverageCellVoltage() {
		return getScaledFloatValue(LithiumIonStringModelRegister.AverageCellVoltage,
				LithiumIonStringModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Float getMaximumModuleTemperature() {
		return getScaledFloatValue(LithiumIonStringModelRegister.MaximumModuleTemperature,
				LithiumIonStringModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	public @Nullable Integer getMaximumModuleTemperatureModuleIndex() {
		return getIntegerValue(LithiumIonStringModelRegister.MaximumModuleTemperatureModuleIndex);
	}

	@Override
	public @Nullable Float getMinimumModuleTemperature() {
		return getScaledFloatValue(LithiumIonStringModelRegister.MinimumModuleTemperature,
				LithiumIonStringModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	public @Nullable Integer getMinimumModuleTemperatureModuleIndex() {
		return getIntegerValue(LithiumIonStringModelRegister.MinimumModuleTemperatureModuleIndex);
	}

	@Override
	public @Nullable Float getAverageModuleTemperature() {
		return getScaledFloatValue(LithiumIonStringModelRegister.AverageModuleTemperature,
				LithiumIonStringModelRegister.ScaleFactorModuleTemperature);
	}

	@Override
	public Set<Integer> getClosedContactors() {
		return getBitfieldIndexes(LithiumIonStringModelRegister.ContactorStatus);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(LithiumIonStringModelRegister.EventsBitmask);
		return LithiumIonStringEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public BitSet getVendorEvents() {
		return getBitfieldBits(LithiumIonStringModelRegister.VendorEventsBitmask,
				LithiumIonStringModelRegister.VendorEvents2Bitmask);
	}

	@Override
	public @Nullable BatteryEnableOperation getEnableOperation() {
		return getCodedValue(LithiumIonStringModelRegister.EnableOperation,
				BatteryEnableOperation.class);
	}

	@Override
	public void setEnableOperation(ModbusConnection conn, BatteryEnableOperation operation)
			throws IOException {
		writeValue(conn, LithiumIonStringModelRegister.EnableOperation, operation.getCode());
	}

	@Override
	public @Nullable BatteryOperation getConnectOperation() {
		return getCodedValue(LithiumIonStringModelRegister.ConnectOperation, BatteryOperation.class);
	}

	@Override
	public void setConnectOperation(ModbusConnection conn, BatteryOperation operation)
			throws IOException {
		writeValue(conn, LithiumIonStringModelRegister.ConnectOperation, operation.getCode());
	}

	@Override
	public List<BatteryModule> getModules() {
		final int instanceCount = getRepeatingBlockInstanceCount();
		final Integer moduleCount = getModuleCount();
		final int count = (moduleCount != null ? Math.min(moduleCount, instanceCount) : instanceCount);
		if ( count < 1 ) {
			return List.of();
		}
		final List<BatteryModule> result = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			result.add(new BatteryModuleImpl(i));
		}
		return result;
	}

	private final class BatteryModuleImpl implements BatteryModule {

		private final int index;
		private final int groupAddress;

		private BatteryModuleImpl(int index) {
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
		public @Nullable Integer getCellCount() {
			return getIntegerValue(LithiumIonStringModelRegister.ModuleCellCount, groupAddress);
		}

		@Override
		public @Nullable Float getStateOfCharge() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleStateOfCharge,
					LithiumIonStringModelRegister.ScaleFactorStateOfCharge, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Float getStateOfHealth() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleStateOfHealth,
					LithiumIonStringModelRegister.ScaleFactorStateOfHealth, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Float getMaximumCellVoltage() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleMaximumCellVoltage,
					LithiumIonStringModelRegister.ScaleFactorCellVoltage, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Integer getMaximumCellVoltageCellIndex() {
			return getIntegerValue(LithiumIonStringModelRegister.ModuleMaximumCellVoltageCellIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getMinimumCellVoltage() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleMinimumCellVoltage,
					LithiumIonStringModelRegister.ScaleFactorCellVoltage, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Integer getMinimumCellVoltageCellIndex() {
			return getIntegerValue(LithiumIonStringModelRegister.ModuleMinimumCellVoltageCellIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getAverageCellVoltage() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleAverageCellVoltage,
					LithiumIonStringModelRegister.ScaleFactorCellVoltage, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Float getMaximumCellTemperature() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleMaximumCellTemperature,
					LithiumIonStringModelRegister.ScaleFactorModuleTemperature, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Integer getMaximumCellTemperatureCellIndex() {
			return getIntegerValue(LithiumIonStringModelRegister.ModuleMaximumCellTemperatureCellIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getMinimumCellTemperature() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleMinimumCellTemperature,
					LithiumIonStringModelRegister.ScaleFactorModuleTemperature, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Integer getMinimumCellTemperatureCellIndex() {
			return getIntegerValue(LithiumIonStringModelRegister.ModuleMinimumCellTemperatureCellIndex,
					groupAddress);
		}

		@Override
		public @Nullable Float getAverageCellTemperature() {
			return getScaledFloatValue(LithiumIonStringModelRegister.ModuleAverageCellTemperature,
					LithiumIonStringModelRegister.ScaleFactorModuleTemperature, groupAddress,
					getBlockAddress());
		}

	}

}
