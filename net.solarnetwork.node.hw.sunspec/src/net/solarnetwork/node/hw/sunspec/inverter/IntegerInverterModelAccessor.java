/* ==================================================================
 * IntegerInverterModelAccessor.java - 5/10/2018 5:13:14 PM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import java.util.Set;
import bak.pcj.set.IntRange;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Data access object for an integer inverter model.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerInverterModelAccessor extends BaseModelAccessor implements InverterModelAccessor {

	/** The integer inverter model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 50;

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
	public IntegerInverterModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	public Float getFrequencyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorFrequency);
		return (n != null ? n.floatValue() : null);
	}

	public Float getCurrentValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorCurrent);
		return (n != null ? n.floatValue() : null);
	}

	public Float getVoltageValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorVoltage);
		return (n != null ? n.floatValue() : null);
	}

	public Float getPowerFactorValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorPowerFactor);
		return (n != null ? n.floatValue() : null);
	}

	public Integer getActivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorActivePower);
		return (n != null ? n.intValue() : null);
	}

	public Integer getApparentPowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorApparentPower);
		return (n != null ? n.intValue() : null);
	}

	public Integer getReactivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorReactivePower);
		return (n != null ? n.intValue() : null);
	}

	public Long getActiveEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorActiveEnergy);
		return (n != null ? n.longValue() : null);
	}

	public Float getTemperatureValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorTemperature);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public InverterModelAccessor accessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		return new PhaseInverterModelAccessor(phase);
	}

	@Override
	public Float getFrequency() {
		return getFrequencyValue(IntegerInverterModelRegister.Frequency);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(IntegerInverterModelRegister.CurrentTotal);
	}

	@Override
	public Float getVoltage() {
		final int modelId = (getModelId() != null ? getModelId().getId() : -1);
		int count = 0;
		float total = 0;
		Float f = getVoltageValue(IntegerInverterModelRegister.VoltagePhaseANeutral);
		if ( f != null ) {
			total = f.floatValue();
			count++;
		}
		if ( modelId > InverterModelId.SinglePhaseInverterInteger.getId() ) {
			Float f2 = getVoltageValue(IntegerInverterModelRegister.VoltagePhaseBNeutral);
			if ( f2 != null ) {
				total += f2.floatValue();
				count++;
			}
		}
		if ( modelId > InverterModelId.SplitPhaseInverterInteger.getId() ) {
			Float f3 = getVoltageValue(IntegerInverterModelRegister.VoltagePhaseCNeutral);
			if ( f3 != null ) {
				total += f3.floatValue();
				count++;
			}
		}

		return (count > 0 ? total / count : null);
	}

	@Override
	public Float getPowerFactor() {
		return getPowerFactorValue(IntegerInverterModelRegister.PowerFactorAverage);
	}

	@Override
	public Integer getActivePower() {
		return getActivePowerValue(IntegerInverterModelRegister.ActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getApparentPowerValue(IntegerInverterModelRegister.ApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getReactivePowerValue(IntegerInverterModelRegister.ReactivePowerTotal);
	}

	@Override
	public Long getActiveEnergyExported() {
		return getActiveEnergyValue(IntegerInverterModelRegister.ActiveEnergyExportedTotal);
	}

	@Override
	public Float getDcCurrent() {
		Number n = getScaledValue(IntegerInverterModelRegister.DcCurrentTotal,
				IntegerInverterModelRegister.ScaleFactorDcCurrent);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getDcVoltage() {
		Number n = getScaledValue(IntegerInverterModelRegister.DcVoltageTotal,
				IntegerInverterModelRegister.ScaleFactorDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getDcPower() {
		Number n = getScaledValue(IntegerInverterModelRegister.DcPowerTotal,
				IntegerInverterModelRegister.ScaleFactorDcPower);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Float getCabinetTemperature() {
		return getTemperatureValue(IntegerInverterModelRegister.TemperatureCabinet);
	}

	@Override
	public Float getHeatSinkTemperature() {
		return getTemperatureValue(IntegerInverterModelRegister.TemperatureHeatSink);
	}

	@Override
	public Float getTransformerTemperature() {
		return getTemperatureValue(IntegerInverterModelRegister.TemperatureTransformer);
	}

	@Override
	public Float getOtherTemperature() {
		return getTemperatureValue(IntegerInverterModelRegister.TemperatureOther);
	}

	@Override
	public OperatingState getOperatingState() {
		Number n = getData().getNumber(IntegerInverterModelRegister.OperatingState, getBlockAddress());
		if ( n == null ) {
			return null;
		}
		return InverterOperatingState.forCode(n.intValue());
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(IntegerInverterModelRegister.EventsBitmask);
		return InverterModelEvent.forBitmask(n.longValue());
	}

	private class PhaseInverterModelAccessor implements InverterModelAccessor {

		private final ACPhase phase;

		private PhaseInverterModelAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public IntRange[] getAddressRanges(int maxRangeLength) {
			return IntegerInverterModelAccessor.this.getAddressRanges(maxRangeLength);
		}

		@Override
		public long getDataTimestamp() {
			return IntegerInverterModelAccessor.this.getDataTimestamp();
		}

		@Override
		public int getBaseAddress() {
			return IntegerInverterModelAccessor.this.getBaseAddress();
		}

		@Override
		public int getFixedBlockLength() {
			return IntegerInverterModelAccessor.this.getFixedBlockLength();
		}

		@Override
		public int getBlockAddress() {
			return IntegerInverterModelAccessor.this.getBlockAddress();
		}

		@Override
		public ModelId getModelId() {
			return IntegerInverterModelAccessor.this.getModelId();
		}

		@Override
		public int getModelLength() {
			return IntegerInverterModelAccessor.this.getModelLength();
		}

		@Override
		public int getRepeatingBlockInstanceLength() {
			return IntegerInverterModelAccessor.this.getRepeatingBlockInstanceLength();
		}

		@Override
		public InverterModelAccessor accessorForPhase(ACPhase phase) {
			return IntegerInverterModelAccessor.this.accessorForPhase(phase);
		}

		@Override
		public int getRepeatingBlockInstanceCount() {
			return IntegerInverterModelAccessor.this.getRepeatingBlockInstanceCount();
		}

		@Override
		public Float getFrequency() {
			return IntegerInverterModelAccessor.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			switch (phase) {
				case PhaseA:
					return getCurrentValue(IntegerInverterModelRegister.CurrentPhaseA);

				case PhaseB:
					return getCurrentValue(IntegerInverterModelRegister.CurrentPhaseB);

				case PhaseC:
					return getCurrentValue(IntegerInverterModelRegister.CurrentPhaseC);

				default:
					return IntegerInverterModelAccessor.this.getCurrent();
			}
		}

		@Override
		public Float getVoltage() {
			switch (phase) {
				case PhaseA:
					return getVoltageValue(IntegerInverterModelRegister.VoltagePhaseANeutral);

				case PhaseB:
					return getVoltageValue(IntegerInverterModelRegister.VoltagePhaseBNeutral);

				case PhaseC:
					return getVoltageValue(IntegerInverterModelRegister.VoltagePhaseCNeutral);

				default:
					return IntegerInverterModelAccessor.this.getVoltage();
			}
		}

		@Override
		public Float getPowerFactor() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return IntegerInverterModelAccessor.this.getPowerFactor();
			}
		}

		@Override
		public Integer getActivePower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return IntegerInverterModelAccessor.this.getActivePower();
			}
		}

		@Override
		public Integer getApparentPower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return IntegerInverterModelAccessor.this.getApparentPower();
			}
		}

		@Override
		public Integer getReactivePower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return IntegerInverterModelAccessor.this.getReactivePower();
			}
		}

		@Override
		public Long getActiveEnergyExported() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return IntegerInverterModelAccessor.this.getActiveEnergyExported();
			}
		}

		@Override
		public Float getDcCurrent() {
			return IntegerInverterModelAccessor.this.getDcCurrent();
		}

		@Override
		public Float getDcVoltage() {
			return IntegerInverterModelAccessor.this.getDcVoltage();
		}

		@Override
		public Integer getDcPower() {
			return IntegerInverterModelAccessor.this.getDcPower();
		}

		@Override
		public Float getCabinetTemperature() {
			return IntegerInverterModelAccessor.this.getCabinetTemperature();
		}

		@Override
		public Float getHeatSinkTemperature() {
			return IntegerInverterModelAccessor.this.getHeatSinkTemperature();
		}

		@Override
		public Float getTransformerTemperature() {
			return IntegerInverterModelAccessor.this.getTransformerTemperature();
		}

		@Override
		public Float getOtherTemperature() {
			return IntegerInverterModelAccessor.this.getOtherTemperature();
		}

		@Override
		public OperatingState getOperatingState() {
			return IntegerInverterModelAccessor.this.getOperatingState();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return IntegerInverterModelAccessor.this.getEvents();
		}

	}
}
