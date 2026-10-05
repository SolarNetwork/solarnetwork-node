/* ==================================================================
 * LithiumIonModuleModelAccessorImpl.java - 5/10/2026 9:05:44 pm
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link LithiumIonModuleModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class LithiumIonModuleModelAccessorImpl extends BaseModelAccessor
		implements LithiumIonModuleModelAccessor {

	/** The lithium-ion module model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 42;

	/** The lithium-ion module model cell repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 4;

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
	public LithiumIonModuleModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public LithiumIonModuleModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
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
		return EnumSet.range(LithiumIonModuleModelRegister.StringIndex,
				LithiumIonModuleModelRegister.ScaleFactorTemperature);
	}

	@Override
	protected Collection<? extends ModbusReference> getRepeatingBlockRegisters() {
		return EnumSet.range(LithiumIonModuleModelRegister.CellVoltage,
				LithiumIonModuleModelRegister.CellStatus);
	}

	@Override
	public @Nullable Integer getStringIndex() {
		return getIntegerValue(LithiumIonModuleModelRegister.StringIndex);
	}

	@Override
	public @Nullable Integer getModuleIndex() {
		return getIntegerValue(LithiumIonModuleModelRegister.ModuleIndex);
	}

	@Override
	public @Nullable Integer getCellCount() {
		return getIntegerValue(LithiumIonModuleModelRegister.NumberOfCells);
	}

	@Override
	public @Nullable Float getStateOfCharge() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.StateOfCharge,
				LithiumIonModuleModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public @Nullable Float getDepthOfDischarge() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.DepthOfDischarge,
				LithiumIonModuleModelRegister.ScaleFactorDepthOfDischarge);
	}

	@Override
	public @Nullable Float getStateOfHealth() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.StateOfHealth,
				LithiumIonModuleModelRegister.ScaleFactorStateOfHealth);
	}

	@Override
	public @Nullable Long getCycleCount() {
		return getLongValue(LithiumIonModuleModelRegister.CycleCount);
	}

	@Override
	public @Nullable Float getDCVoltage() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.DcVoltage,
				LithiumIonModuleModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getMaximumCellVoltage() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.MaximumCellVoltage,
				LithiumIonModuleModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Integer getMaximumCellVoltageCellIndex() {
		return getIntegerValue(LithiumIonModuleModelRegister.MaximumCellVoltageCellIndex);
	}

	@Override
	public @Nullable Float getMinimumCellVoltage() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.MinimumCellVoltage,
				LithiumIonModuleModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Integer getMinimumCellVoltageCellIndex() {
		return getIntegerValue(LithiumIonModuleModelRegister.MinimumCellVoltageCellIndex);
	}

	@Override
	public @Nullable Float getAverageCellVoltage() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.AverageCellVoltage,
				LithiumIonModuleModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Float getMaximumCellTemperature() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.MaximumCellTemperature,
				LithiumIonModuleModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Integer getMaximumCellTemperatureCellIndex() {
		return getIntegerValue(LithiumIonModuleModelRegister.MaximumCellTemperatureCellIndex);
	}

	@Override
	public @Nullable Float getMinimumCellTemperature() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.MinimumCellTemperature,
				LithiumIonModuleModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Integer getMinimumCellTemperatureCellIndex() {
		return getIntegerValue(LithiumIonModuleModelRegister.MinimumCellTemperatureCellIndex);
	}

	@Override
	public @Nullable Float getAverageCellTemperature() {
		return getScaledFloatValue(LithiumIonModuleModelRegister.AverageCellTemperature,
				LithiumIonModuleModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Integer getBalancingCellCount() {
		return getIntegerValue(LithiumIonModuleModelRegister.BalancingCellCount);
	}

	@Override
	public @Nullable String getSerialNumber() {
		return getStringValue(LithiumIonModuleModelRegister.SerialNumber);
	}

	@Override
	public List<BatteryCell> getCells() {
		final int count = getRepeatingBlockInstanceCount();
		if ( count < 1 ) {
			return List.of();
		}
		final List<BatteryCell> result = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			result.add(new BatteryCellImpl(i));
		}
		return result;
	}

	private final class BatteryCellImpl implements BatteryCell {

		private final int index;
		private final int groupAddress;

		private BatteryCellImpl(int index) {
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
		public @Nullable Float getVoltage() {
			return getScaledFloatValue(LithiumIonModuleModelRegister.CellVoltage,
					LithiumIonModuleModelRegister.ScaleFactorCellVoltage, groupAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable Float getTemperature() {
			return getScaledFloatValue(LithiumIonModuleModelRegister.CellTemperature,
					LithiumIonModuleModelRegister.ScaleFactorTemperature, groupAddress,
					getBlockAddress());
		}

		@Override
		public Set<LithiumIonCellStatus> getStatus() {
			return getBitmaskableValues(LithiumIonModuleModelRegister.CellStatus, groupAddress,
					LithiumIonCellStatus.class);
		}

	}

}
