/* ==================================================================
 * IntegerMeterModelAccessor.java - 22/05/2018 6:31:57 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

import java.util.Set;
import bak.pcj.set.IntRange;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Data object for an integer meter model.
 * 
 * @author matt
 * @version 1.1
 */
public class IntegerMeterModelAccessor extends BaseModelAccessor implements MeterModelAccessor {

	/** The integer meter model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 105;

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
	public IntegerMeterModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	public Float getFrequencyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorFrequency);
		return (n != null ? n.floatValue() : null);
	}

	public Float getCurrentValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorCurrent);
		return (n != null ? n.floatValue() : null);
	}

	public Float getVoltageValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorVoltage);
		return (n != null ? n.floatValue() : null);
	}

	public Float getPowerFactorValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorPowerFactor);
		return (n != null ? n.floatValue() : null);
	}

	public Integer getActivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorActivePower);
		return (n != null ? n.intValue() : null);
	}

	public Integer getApparentPowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorApparentPower);
		return (n != null ? n.intValue() : null);
	}

	public Integer getReactivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorReactivePower);
		return (n != null ? n.intValue() : null);
	}

	public Long getActiveEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorActiveEnergy);
		return (n != null ? n.longValue() : null);
	}

	public Long getApparentEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorApparentEnergy);
		return (n != null ? n.longValue() : null);
	}

	public Long getReactiveEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorReactiveEnergy);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public MeterModelAccessor accessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		return new PhaseMeterModelAccessor(phase);
	}

	@Override
	public Float getFrequency() {
		return getFrequencyValue(IntegerMeterModelRegister.Frequency);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(IntegerMeterModelRegister.CurrentTotal);
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(IntegerMeterModelRegister.VoltageLineNeutralAverage);
	}

	@Override
	public Float getPowerFactor() {
		return getPowerFactorValue(IntegerMeterModelRegister.PowerFactorAverage);
	}

	@Override
	public Integer getActivePower() {
		return getActivePowerValue(IntegerMeterModelRegister.ActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getApparentPowerValue(IntegerMeterModelRegister.ApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getReactivePowerValue(IntegerMeterModelRegister.ReactivePowerTotal);
	}

	@Override
	public Long getActiveEnergyImported() {
		return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyImportedTotal);
	}

	@Override
	public Long getActiveEnergyExported() {
		return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyExportedTotal);
	}

	@Override
	public Long getReactiveEnergyImported() {
		Long q1 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyImportedQ1Total);
		Long q2 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyImportedQ2Total);
		return (q1 != null ? q1.longValue() : 0) + (q2 != null ? q2.longValue() : 0);
	}

	@Override
	public Long getReactiveEnergyExported() {
		Long q3 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyExportedQ3Total);
		Long q4 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyExportedQ4Total);
		return (q3 != null ? q3.longValue() : 0) + (q4 != null ? q4.longValue() : 0);
	}

	@Override
	public Long getApparentEnergyImported() {
		return getApparentEnergyValue(IntegerMeterModelRegister.ApparentEnergyImportedTotal);
	}

	@Override
	public Long getApparentEnergyExported() {
		return getApparentEnergyValue(IntegerMeterModelRegister.ApparentEnergyExportedTotal);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(IntegerMeterModelRegister.EventsBitmask);
		return MeterModelEvent.forBitmask(n.longValue());
	}

	private class PhaseMeterModelAccessor implements MeterModelAccessor {

		private final ACPhase phase;

		private PhaseMeterModelAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public IntRange[] getAddressRanges(int maxRangeLength) {
			return IntegerMeterModelAccessor.this.getAddressRanges(maxRangeLength);
		}

		@Override
		public long getDataTimestamp() {
			return IntegerMeterModelAccessor.this.getDataTimestamp();
		}

		@Override
		public int getBaseAddress() {
			return IntegerMeterModelAccessor.this.getBaseAddress();
		}

		@Override
		public int getFixedBlockLength() {
			return IntegerMeterModelAccessor.this.getFixedBlockLength();
		}

		@Override
		public int getBlockAddress() {
			return IntegerMeterModelAccessor.this.getBlockAddress();
		}

		@Override
		public ModelId getModelId() {
			return IntegerMeterModelAccessor.this.getModelId();
		}

		@Override
		public int getModelLength() {
			return IntegerMeterModelAccessor.this.getModelLength();
		}

		@Override
		public int getRepeatingBlockInstanceLength() {
			return IntegerMeterModelAccessor.this.getRepeatingBlockInstanceLength();
		}

		@Override
		public MeterModelAccessor accessorForPhase(ACPhase phase) {
			return IntegerMeterModelAccessor.this.accessorForPhase(phase);
		}

		@Override
		public int getRepeatingBlockInstanceCount() {
			return IntegerMeterModelAccessor.this.getRepeatingBlockInstanceCount();
		}

		@Override
		public Float getFrequency() {
			return IntegerMeterModelAccessor.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			switch (phase) {
				case PhaseA:
					return getCurrentValue(IntegerMeterModelRegister.CurrentPhaseA);

				case PhaseB:
					return getCurrentValue(IntegerMeterModelRegister.CurrentPhaseB);

				case PhaseC:
					return getCurrentValue(IntegerMeterModelRegister.CurrentPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getCurrent();
			}
		}

		@Override
		public Float getVoltage() {
			switch (phase) {
				case PhaseA:
					return getVoltageValue(IntegerMeterModelRegister.VoltagePhaseANeutral);

				case PhaseB:
					return getVoltageValue(IntegerMeterModelRegister.VoltagePhaseBNeutral);

				case PhaseC:
					return getVoltageValue(IntegerMeterModelRegister.VoltagePhaseCNeutral);

				default:
					return IntegerMeterModelAccessor.this.getVoltage();
			}
		}

		@Override
		public Float getPowerFactor() {
			switch (phase) {
				case PhaseA:
					return getPowerFactorValue(IntegerMeterModelRegister.PowerFactorPhaseA);

				case PhaseB:
					return getPowerFactorValue(IntegerMeterModelRegister.PowerFactorPhaseB);

				case PhaseC:
					return getPowerFactorValue(IntegerMeterModelRegister.PowerFactorPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getPowerFactor();
			}
		}

		@Override
		public Integer getActivePower() {
			switch (phase) {
				case PhaseA:
					return getActivePowerValue(IntegerMeterModelRegister.ActivePowerPhaseA);

				case PhaseB:
					return getActivePowerValue(IntegerMeterModelRegister.ActivePowerPhaseB);

				case PhaseC:
					return getActivePowerValue(IntegerMeterModelRegister.ActivePowerPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getActivePower();
			}
		}

		@Override
		public Integer getApparentPower() {
			switch (phase) {
				case PhaseA:
					return getApparentPowerValue(IntegerMeterModelRegister.ApparentPowerPhaseA);

				case PhaseB:
					return getApparentPowerValue(IntegerMeterModelRegister.ApparentPowerPhaseB);

				case PhaseC:
					return getApparentPowerValue(IntegerMeterModelRegister.ApparentPowerPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getApparentPower();
			}
		}

		@Override
		public Integer getReactivePower() {
			switch (phase) {
				case PhaseA:
					return getReactivePowerValue(IntegerMeterModelRegister.ReactivePowerPhaseA);

				case PhaseB:
					return getReactivePowerValue(IntegerMeterModelRegister.ReactivePowerPhaseB);

				case PhaseC:
					return getReactivePowerValue(IntegerMeterModelRegister.ReactivePowerPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getReactivePower();
			}
		}

		@Override
		public Long getActiveEnergyImported() {
			switch (phase) {
				case PhaseA:
					return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyImportedPhaseA);

				case PhaseB:
					return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyImportedPhaseB);

				case PhaseC:
					return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyImportedPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getActiveEnergyImported();
			}
		}

		@Override
		public Long getActiveEnergyExported() {
			switch (phase) {
				case PhaseA:
					return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyExportedPhaseA);

				case PhaseB:
					return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyExportedPhaseB);

				case PhaseC:
					return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyExportedPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getActiveEnergyExported();
			}
		}

		@Override
		public Long getReactiveEnergyImported() {
			Long q1 = null;
			Long q2 = null;
			switch (phase) {
				case PhaseA:
					q1 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyImportedQ1PhaseA);
					q2 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyImportedQ2PhaseA);
					break;

				case PhaseB:
					q1 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyImportedQ1PhaseB);
					q2 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyImportedQ2PhaseB);
					break;

				case PhaseC:
					q1 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyImportedQ1PhaseC);
					q2 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyImportedQ2PhaseC);
					break;

				default:
					return IntegerMeterModelAccessor.this.getReactiveEnergyImported();
			}
			return (q1 == null && q2 == null ? null
					: (q1 != null ? q1.longValue() : 0) + (q2 != null ? q2.longValue() : 0));
		}

		@Override
		public Long getReactiveEnergyExported() {
			Long q1 = null;
			Long q2 = null;
			switch (phase) {
				case PhaseA:
					q1 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyExportedQ3PhaseA);
					q2 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyExportedQ4PhaseA);
					break;

				case PhaseB:
					q1 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyExportedQ3PhaseB);
					q2 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyExportedQ4PhaseB);
					break;

				case PhaseC:
					q1 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyExportedQ3PhaseC);
					q2 = getReactiveEnergyValue(
							IntegerMeterModelRegister.ReactiveEnergyExportedQ4PhaseC);
					break;

				default:
					return IntegerMeterModelAccessor.this.getReactiveEnergyExported();
			}
			return (q1 == null && q2 == null ? null
					: (q1 != null ? q1.longValue() : 0) + (q2 != null ? q2.longValue() : 0));
		}

		@Override
		public Long getApparentEnergyImported() {
			switch (phase) {
				case PhaseA:
					return getApparentEnergyValue(
							IntegerMeterModelRegister.ApparentEnergyImportedPhaseA);

				case PhaseB:
					return getApparentEnergyValue(
							IntegerMeterModelRegister.ApparentEnergyImportedPhaseB);

				case PhaseC:
					return getApparentEnergyValue(
							IntegerMeterModelRegister.ApparentEnergyImportedPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getApparentEnergyImported();
			}
		}

		@Override
		public Long getApparentEnergyExported() {
			switch (phase) {
				case PhaseA:
					return getApparentEnergyValue(
							IntegerMeterModelRegister.ApparentEnergyExportedPhaseA);

				case PhaseB:
					return getApparentEnergyValue(
							IntegerMeterModelRegister.ApparentEnergyExportedPhaseB);

				case PhaseC:
					return getApparentEnergyValue(
							IntegerMeterModelRegister.ApparentEnergyExportedPhaseC);

				default:
					return IntegerMeterModelAccessor.this.getApparentEnergyExported();
			}
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return IntegerMeterModelAccessor.this.getEvents();
		}

	}

}
