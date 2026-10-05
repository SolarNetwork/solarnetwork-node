/* ==================================================================
 * DerFrequencyDroopModelAccessorImpl.java - 5/10/2026 7:12:33 pm
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

package net.solarnetwork.node.hw.sunspec.der;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link DerFrequencyDroopModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerFrequencyDroopModelAccessorImpl extends BaseModelAccessor
		implements DerFrequencyDroopModelAccessor {

	/** The DER frequency droop model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 12;

	/** The DER frequency droop model control repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 10;

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
	public DerFrequencyDroopModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link DerModelId} class will be used as the {@code ModelId}
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
	public DerFrequencyDroopModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
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
		return EnumSet.range(DerFrequencyDroopModelRegister.Enabled,
				DerFrequencyDroopModelRegister.ScaleFactorResponseTime);
	}

	@Override
	protected Collection<? extends ModbusReference> getRepeatingBlockRegisters() {
		return EnumSet.range(DerFrequencyDroopModelRegister.ControlOverFrequencyDeadband,
				DerFrequencyDroopModelRegister.ControlReadOnly);
	}

	@Override
	public @Nullable Boolean isEnabled() {
		return getBooleanValue(DerFrequencyDroopModelRegister.Enabled);
	}

	@Override
	public void setEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, DerFrequencyDroopModelRegister.Enabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Integer getControlCount() {
		return getIntegerValue(DerFrequencyDroopModelRegister.NumberOfControls);
	}

	@Override
	public @Nullable Integer getAdoptControlRequest() {
		return getIntegerValue(DerFrequencyDroopModelRegister.AdoptControlRequest);
	}

	@Override
	public void adoptControl(ModbusConnection conn, int index) throws IOException {
		final Integer count = getControlCount();
		if ( index < 0 || (count != null && index > count) ) {
			throw new IllegalArgumentException(String.format(
					"The control index %d is not valid: it must be between 0 and the control count %s.",
					index, count));
		}
		writeValue(conn, DerFrequencyDroopModelRegister.AdoptControlRequest, index);
	}

	@Override
	public @Nullable DerAdoptResult getAdoptControlResult() {
		return getCodedValue(DerFrequencyDroopModelRegister.AdoptControlResult, DerAdoptResult.class);
	}

	@Override
	public @Nullable Long getReversionTime() {
		return getLongValue(DerFrequencyDroopModelRegister.ReversionTime);
	}

	@Override
	public void setReversionTime(ModbusConnection conn, long seconds) throws IOException {
		writeValue(conn, DerFrequencyDroopModelRegister.ReversionTime, seconds);
	}

	@Override
	public @Nullable Long getReversionTimeRemaining() {
		return getLongValue(DerFrequencyDroopModelRegister.ReversionTimeRemaining);
	}

	@Override
	public @Nullable Integer getReversionControl() {
		return getIntegerValue(DerFrequencyDroopModelRegister.ReversionControl);
	}

	@Override
	public void setReversionControl(ModbusConnection conn, int index) throws IOException {
		final Integer count = getControlCount();
		if ( index < 1 || (count != null && index > count) ) {
			throw new IllegalArgumentException(String.format(
					"The control index %d is not valid: it must be between 1 and the control count %s.",
					index, count));
		}
		writeValue(conn, DerFrequencyDroopModelRegister.ReversionControl, index);
	}

	@Override
	public List<FrequencyDroopControl> getControls() {
		final int instanceCount = getRepeatingBlockInstanceCount();
		final Integer controlCount = getControlCount();
		final int count = (controlCount != null ? Math.min(controlCount, instanceCount) : instanceCount);
		if ( count < 1 ) {
			return List.of();
		}
		final List<FrequencyDroopControl> result = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			result.add(new FrequencyDroopControlImpl(i));
		}
		return result;
	}

	private final class FrequencyDroopControlImpl implements FrequencyDroopControl {

		private final int index;
		private final int controlAddress;

		private FrequencyDroopControlImpl(int index) {
			super();
			this.index = index;
			this.controlAddress = getBlockAddress() + FIXED_BLOCK_LENGTH
					+ (index - 1) * REPEATING_BLOCK_LENGTH;
		}

		private void requireWritable() {
			if ( Boolean.TRUE.equals(isReadOnly()) ) {
				throw new UnsupportedOperationException(
						String.format("Control %d of model %s is read-only.", index, getModelId()));
			}
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public @Nullable Boolean isReadOnly() {
			return getBooleanValue(DerFrequencyDroopModelRegister.ControlReadOnly, controlAddress);
		}

		@Override
		public @Nullable Float getOverFrequencyDeadband() {
			return getScaledFloatValue(DerFrequencyDroopModelRegister.ControlOverFrequencyDeadband,
					DerFrequencyDroopModelRegister.ScaleFactorDeadband, controlAddress,
					getBlockAddress());
		}

		@Override
		public void setOverFrequencyDeadband(ModbusConnection conn, float hertz) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerFrequencyDroopModelRegister.ControlOverFrequencyDeadband,
					DerFrequencyDroopModelRegister.ScaleFactorDeadband, controlAddress,
					getBlockAddress(), hertz);
		}

		@Override
		public @Nullable Float getUnderFrequencyDeadband() {
			return getScaledFloatValue(DerFrequencyDroopModelRegister.ControlUnderFrequencyDeadband,
					DerFrequencyDroopModelRegister.ScaleFactorDeadband, controlAddress,
					getBlockAddress());
		}

		@Override
		public void setUnderFrequencyDeadband(ModbusConnection conn, float hertz) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerFrequencyDroopModelRegister.ControlUnderFrequencyDeadband,
					DerFrequencyDroopModelRegister.ScaleFactorDeadband, controlAddress,
					getBlockAddress(), hertz);
		}

		@Override
		public @Nullable Float getOverFrequencyChangeRatio() {
			return getScaledFloatValue(DerFrequencyDroopModelRegister.ControlOverFrequencyChangeRatio,
					DerFrequencyDroopModelRegister.ScaleFactorChangeRatio, controlAddress,
					getBlockAddress());
		}

		@Override
		public void setOverFrequencyChangeRatio(ModbusConnection conn, float ratio) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerFrequencyDroopModelRegister.ControlOverFrequencyChangeRatio,
					DerFrequencyDroopModelRegister.ScaleFactorChangeRatio, controlAddress,
					getBlockAddress(), ratio);
		}

		@Override
		public @Nullable Float getUnderFrequencyChangeRatio() {
			return getScaledFloatValue(DerFrequencyDroopModelRegister.ControlUnderFrequencyChangeRatio,
					DerFrequencyDroopModelRegister.ScaleFactorChangeRatio, controlAddress,
					getBlockAddress());
		}

		@Override
		public void setUnderFrequencyChangeRatio(ModbusConnection conn, float ratio) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerFrequencyDroopModelRegister.ControlUnderFrequencyChangeRatio,
					DerFrequencyDroopModelRegister.ScaleFactorChangeRatio, controlAddress,
					getBlockAddress(), ratio);
		}

		@Override
		public @Nullable Float getOpenLoopResponseTime() {
			return getScaledFloatValue(DerFrequencyDroopModelRegister.ControlOpenLoopResponseTime,
					DerFrequencyDroopModelRegister.ScaleFactorResponseTime, controlAddress,
					getBlockAddress());
		}

		@Override
		public void setOpenLoopResponseTime(ModbusConnection conn, float seconds) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerFrequencyDroopModelRegister.ControlOpenLoopResponseTime,
					DerFrequencyDroopModelRegister.ScaleFactorResponseTime, controlAddress,
					getBlockAddress(), seconds);
		}

		@Override
		public @Nullable Integer getMinimumActivePower() {
			return getIntegerValue(DerFrequencyDroopModelRegister.ControlMinimumActivePower,
					controlAddress);
		}

		@Override
		public void setMinimumActivePower(ModbusConnection conn, int percent) throws IOException {
			if ( percent < -100 || percent > 100 ) {
				throw new IllegalArgumentException(String.format(
						"The minimum active power %d%% is not valid: it must be between -100 and 100.",
						percent));
			}
			requireWritable();
			writeValue(conn, DerFrequencyDroopModelRegister.ControlMinimumActivePower, controlAddress,
					percent);
		}

	}

}
