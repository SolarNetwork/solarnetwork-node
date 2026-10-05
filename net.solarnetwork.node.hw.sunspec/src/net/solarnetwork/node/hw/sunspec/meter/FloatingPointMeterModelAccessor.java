/* ==================================================================
 * FloatingPointMeterModelAccessor.java - 6/10/2026 4:31:05 pm
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

package net.solarnetwork.node.hw.sunspec.meter;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;

/**
 * Data object for a floating point meter model.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class FloatingPointMeterModelAccessor extends BaseModelAccessor implements MeterModelAccessor {

	/** The floating point meter model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 124;

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
	public FloatingPointMeterModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link MeterModelId} class will be used as the {@code ModelId}
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
	public FloatingPointMeterModelAccessor(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, MeterModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(FloatingPointMeterModelRegister.class);
	}

	@Override
	public MeterModelAccessor accessorForPhase(AcPhase phase) {
		if ( phase == AcPhase.Total ) {
			return this;
		}
		return new PhaseMeterModelAccessor(phase);
	}

	@Override
	public @Nullable Float getFrequency() {
		return getFloatValue(FloatingPointMeterModelRegister.Frequency);
	}

	@Override
	public @Nullable Float getCurrent() {
		return getFloatValue(FloatingPointMeterModelRegister.CurrentTotal);
	}

	@Override
	public @Nullable Float getNeutralCurrent() {
		// not supported in SunSpec
		return null;
	}

	@Override
	public @Nullable Float getVoltage() {
		return getFloatValue(FloatingPointMeterModelRegister.VoltageLineNeutralAverage);
	}

	@Override
	public @Nullable Float getLineVoltage() {
		return getFloatValue(FloatingPointMeterModelRegister.VoltageLineLineAverage);
	}

	@Override
	public @Nullable Float getPowerFactor() {
		return getFloatValue(FloatingPointMeterModelRegister.PowerFactorAverage);
	}

	@Override
	public @Nullable Integer getActivePower() {
		return getIntegerValue(FloatingPointMeterModelRegister.ActivePowerTotal);
	}

	@Override
	public @Nullable Integer getApparentPower() {
		return getIntegerValue(FloatingPointMeterModelRegister.ApparentPowerTotal);
	}

	@Override
	public @Nullable Integer getReactivePower() {
		return getIntegerValue(FloatingPointMeterModelRegister.ReactivePowerTotal);
	}

	@Override
	public @Nullable Long getActiveEnergyImported() {
		return getLongValue(FloatingPointMeterModelRegister.ActiveEnergyImportedTotal);
	}

	@Override
	public @Nullable Long getActiveEnergyExported() {
		return getLongValue(FloatingPointMeterModelRegister.ActiveEnergyExportedTotal);
	}

	@Override
	public @Nullable Long getReactiveEnergyImported() {
		return sum(getLongValue(FloatingPointMeterModelRegister.ReactiveEnergyImportedQ1Total),
				getLongValue(FloatingPointMeterModelRegister.ReactiveEnergyImportedQ2Total));
	}

	@Override
	public @Nullable Long getReactiveEnergyExported() {
		return sum(getLongValue(FloatingPointMeterModelRegister.ReactiveEnergyExportedQ3Total),
				getLongValue(FloatingPointMeterModelRegister.ReactiveEnergyExportedQ4Total));
	}

	@Override
	public @Nullable Long getApparentEnergyImported() {
		return getLongValue(FloatingPointMeterModelRegister.ApparentEnergyImportedTotal);
	}

	@Override
	public @Nullable Long getApparentEnergyExported() {
		return getLongValue(FloatingPointMeterModelRegister.ApparentEnergyExportedTotal);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(FloatingPointMeterModelRegister.EventsBitmask);
		return MeterModelEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public @Nullable Long getActiveEnergyDelivered() {
		return getActiveEnergyImported();
	}

	@Override
	public @Nullable Long getActiveEnergyReceived() {
		return getActiveEnergyExported();
	}

	@Override
	public @Nullable Long getApparentEnergyDelivered() {
		return getApparentEnergyImported();
	}

	@Override
	public @Nullable Long getApparentEnergyReceived() {
		return getApparentEnergyExported();
	}

	@Override
	public @Nullable Long getReactiveEnergyDelivered() {
		return getReactiveEnergyImported();
	}

	@Override
	public @Nullable Long getReactiveEnergyReceived() {
		return getReactiveEnergyExported();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return getData().getDeviceInfo();
	}

	/**
	 * Add two reactive energy quadrant values.
	 *
	 * @param a
	 *        the first value
	 * @param b
	 *        the second value
	 * @return the sum, or {@code null} if both values are {@code null}
	 */
	private static @Nullable Long sum(@Nullable Long a, @Nullable Long b) {
		if ( a == null && b == null ) {
			return null;
		}
		return (a != null ? a.longValue() : 0L) + (b != null ? b.longValue() : 0L);
	}

	/**
	 * Accessor for the measurements of a single phase.
	 */
	private class PhaseMeterModelAccessor implements MeterModelAccessor {

		private final AcPhase phase;

		/**
		 * Constructor.
		 *
		 * @param phase
		 *        the phase, one of {@code PhaseA}, {@code PhaseB}, or
		 *        {@code PhaseC}
		 */
		private PhaseMeterModelAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		/**
		 * Get the register for this accessor's phase.
		 *
		 * @param phaseA
		 *        the phase A register
		 * @param phaseB
		 *        the phase B register
		 * @param phaseC
		 *        the phase C register
		 * @return the register
		 */
		private FloatingPointMeterModelRegister register(FloatingPointMeterModelRegister phaseA,
				FloatingPointMeterModelRegister phaseB, FloatingPointMeterModelRegister phaseC) {
			switch (phase) {
				case PhaseA:
					return phaseA;

				case PhaseB:
					return phaseB;

				default:
					return phaseC;
			}
		}

		@Override
		public IntRange[] getAddressRanges(int maxRangeLength) {
			return FloatingPointMeterModelAccessor.this.getAddressRanges(maxRangeLength);
		}

		@Override
		public IntRange getAddressRange(int address, int maxRangeLength) {
			return FloatingPointMeterModelAccessor.this.getAddressRange(address, maxRangeLength);
		}

		@Override
		public List<IntRange> getUnsplittableAddressRanges() {
			return FloatingPointMeterModelAccessor.this.getUnsplittableAddressRanges();
		}

		@Override
		public @Nullable Instant getDataTimestamp() {
			return FloatingPointMeterModelAccessor.this.getDataTimestamp();
		}

		@Override
		public int getBaseAddress() {
			return FloatingPointMeterModelAccessor.this.getBaseAddress();
		}

		@Override
		public int getFixedBlockLength() {
			return FloatingPointMeterModelAccessor.this.getFixedBlockLength();
		}

		@Override
		public int getBlockAddress() {
			return FloatingPointMeterModelAccessor.this.getBlockAddress();
		}

		@Override
		public ModelId getModelId() {
			return FloatingPointMeterModelAccessor.this.getModelId();
		}

		@Override
		public int getModelLength() {
			return FloatingPointMeterModelAccessor.this.getModelLength();
		}

		@Override
		public int getRepeatingBlockInstanceLength() {
			return FloatingPointMeterModelAccessor.this.getRepeatingBlockInstanceLength();
		}

		@Override
		public int getRepeatingBlockInstanceCount() {
			return FloatingPointMeterModelAccessor.this.getRepeatingBlockInstanceCount();
		}

		@Override
		public MeterModelAccessor accessorForPhase(AcPhase phase) {
			return FloatingPointMeterModelAccessor.this.accessorForPhase(phase);
		}

		@Override
		public @Nullable Float getFrequency() {
			return FloatingPointMeterModelAccessor.this.getFrequency();
		}

		@Override
		public @Nullable Float getCurrent() {
			return getFloatValue(register(FloatingPointMeterModelRegister.CurrentPhaseA,
					FloatingPointMeterModelRegister.CurrentPhaseB,
					FloatingPointMeterModelRegister.CurrentPhaseC));
		}

		@Override
		public @Nullable Float getNeutralCurrent() {
			return null;
		}

		@Override
		public @Nullable Float getVoltage() {
			return getFloatValue(register(FloatingPointMeterModelRegister.VoltagePhaseANeutral,
					FloatingPointMeterModelRegister.VoltagePhaseBNeutral,
					FloatingPointMeterModelRegister.VoltagePhaseCNeutral));
		}

		@Override
		public @Nullable Float getLineVoltage() {
			return getFloatValue(register(FloatingPointMeterModelRegister.VoltagePhaseAPhaseB,
					FloatingPointMeterModelRegister.VoltagePhaseBPhaseC,
					FloatingPointMeterModelRegister.VoltagePhaseCPhaseA));
		}

		@Override
		public @Nullable Float getPowerFactor() {
			return getFloatValue(register(FloatingPointMeterModelRegister.PowerFactorPhaseA,
					FloatingPointMeterModelRegister.PowerFactorPhaseB,
					FloatingPointMeterModelRegister.PowerFactorPhaseC));
		}

		@Override
		public @Nullable Integer getActivePower() {
			return getIntegerValue(register(FloatingPointMeterModelRegister.ActivePowerPhaseA,
					FloatingPointMeterModelRegister.ActivePowerPhaseB,
					FloatingPointMeterModelRegister.ActivePowerPhaseC));
		}

		@Override
		public @Nullable Integer getApparentPower() {
			return getIntegerValue(register(FloatingPointMeterModelRegister.ApparentPowerPhaseA,
					FloatingPointMeterModelRegister.ApparentPowerPhaseB,
					FloatingPointMeterModelRegister.ApparentPowerPhaseC));
		}

		@Override
		public @Nullable Integer getReactivePower() {
			return getIntegerValue(register(FloatingPointMeterModelRegister.ReactivePowerPhaseA,
					FloatingPointMeterModelRegister.ReactivePowerPhaseB,
					FloatingPointMeterModelRegister.ReactivePowerPhaseC));
		}

		@Override
		public @Nullable Long getActiveEnergyImported() {
			return getLongValue(register(FloatingPointMeterModelRegister.ActiveEnergyImportedPhaseA,
					FloatingPointMeterModelRegister.ActiveEnergyImportedPhaseB,
					FloatingPointMeterModelRegister.ActiveEnergyImportedPhaseC));
		}

		@Override
		public @Nullable Long getActiveEnergyExported() {
			return getLongValue(register(FloatingPointMeterModelRegister.ActiveEnergyExportedPhaseA,
					FloatingPointMeterModelRegister.ActiveEnergyExportedPhaseB,
					FloatingPointMeterModelRegister.ActiveEnergyExportedPhaseC));
		}

		@Override
		public @Nullable Long getReactiveEnergyImported() {
			return sum(
					getLongValue(register(FloatingPointMeterModelRegister.ReactiveEnergyImportedQ1PhaseA,
							FloatingPointMeterModelRegister.ReactiveEnergyImportedQ1PhaseB,
							FloatingPointMeterModelRegister.ReactiveEnergyImportedQ1PhaseC)),
					getLongValue(register(FloatingPointMeterModelRegister.ReactiveEnergyImportedQ2PhaseA,
							FloatingPointMeterModelRegister.ReactiveEnergyImportedQ2PhaseB,
							FloatingPointMeterModelRegister.ReactiveEnergyImportedQ2PhaseC)));
		}

		@Override
		public @Nullable Long getReactiveEnergyExported() {
			return sum(
					getLongValue(register(FloatingPointMeterModelRegister.ReactiveEnergyExportedQ3PhaseA,
							FloatingPointMeterModelRegister.ReactiveEnergyExportedQ3PhaseB,
							FloatingPointMeterModelRegister.ReactiveEnergyExportedQ3PhaseC)),
					getLongValue(register(FloatingPointMeterModelRegister.ReactiveEnergyExportedQ4PhaseA,
							FloatingPointMeterModelRegister.ReactiveEnergyExportedQ4PhaseB,
							FloatingPointMeterModelRegister.ReactiveEnergyExportedQ4PhaseC)));
		}

		@Override
		public @Nullable Long getApparentEnergyImported() {
			return getLongValue(register(FloatingPointMeterModelRegister.ApparentEnergyImportedPhaseA,
					FloatingPointMeterModelRegister.ApparentEnergyImportedPhaseB,
					FloatingPointMeterModelRegister.ApparentEnergyImportedPhaseC));
		}

		@Override
		public @Nullable Long getApparentEnergyExported() {
			return getLongValue(register(FloatingPointMeterModelRegister.ApparentEnergyExportedPhaseA,
					FloatingPointMeterModelRegister.ApparentEnergyExportedPhaseB,
					FloatingPointMeterModelRegister.ApparentEnergyExportedPhaseC));
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return FloatingPointMeterModelAccessor.this.getEvents();
		}

		@Override
		public @Nullable Long getActiveEnergyDelivered() {
			return getActiveEnergyImported();
		}

		@Override
		public @Nullable Long getActiveEnergyReceived() {
			return getActiveEnergyExported();
		}

		@Override
		public @Nullable Long getApparentEnergyDelivered() {
			return getApparentEnergyImported();
		}

		@Override
		public @Nullable Long getApparentEnergyReceived() {
			return getApparentEnergyExported();
		}

		@Override
		public @Nullable Long getReactiveEnergyDelivered() {
			return getReactiveEnergyImported();
		}

		@Override
		public @Nullable Long getReactiveEnergyReceived() {
			return getReactiveEnergyExported();
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return FloatingPointMeterModelAccessor.this.getDeviceInfo();
		}

	}

}
