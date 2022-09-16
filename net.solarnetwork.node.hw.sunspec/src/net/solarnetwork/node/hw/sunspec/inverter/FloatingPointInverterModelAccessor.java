/* ==================================================================
 * FloatingPointInverterModelAccessor.java - 11/10/2019 5:11:44 pm
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

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;

/**
 * Data access object for an floating point inverter models.
 * 
 * @author matt
 * @version 3.1
 * @since 1.4
 */
public class FloatingPointInverterModelAccessor extends BaseModelAccessor
		implements InverterModelAccessor {

	/** The floating point inverter model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 60;

	/**
	 * A metadata key prefix used to hold a {@code Boolean} flag that, when
	 * {@literal true} signifies that the power factor values are encoded as
	 * integer percentage values (from -100...100) rather than the decimal form
	 * of the specification (-1...1).
	 */
	public static final String INTEGER_PF_PCT = "IntPfPct";

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
	public FloatingPointInverterModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
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
	 * @since 1.1
	 */
	public FloatingPointInverterModelAccessor(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	/**
	 * Get a power factor value.
	 * 
	 * @param ref
	 *        the register reference to read
	 * @return the register value, interpreted as a power factor value
	 */
	public Float getPowerFactorValue(ModbusReference ref) {
		Float n = getFloatValue(FloatingPointInverterModelRegister.PowerFactorAverage);
		if ( n == null ) {
			return null;
		}
		// check if we've seen this data as integer percentage before
		Object intPct = getData().getMetadataValue(INTEGER_PF_PCT);
		boolean integerForm = false;
		if ( intPct instanceof Boolean && ((Boolean) intPct).booleanValue() ) {
			integerForm = true;
		} else if ( n.intValue() < -1 || n.intValue() > 1 ) {
			// the data looks like it must be an integer percent, not decimal
			getData().putMetadataValue(INTEGER_PF_PCT, true);
			integerForm = true;
		}
		float f = n.floatValue();
		if ( integerForm ) {
			f /= 100.0f;
		}
		return f;
	}

	@Override
	public InverterModelAccessor accessorForPhase(AcPhase phase) {
		if ( phase == AcPhase.Total ) {
			return this;
		}
		return new PhaseInverterModelAccessor(phase);
	}

	@Override
	public Float getFrequency() {
		return getFloatValue(FloatingPointInverterModelRegister.Frequency);
	}

	@Override
	public Float getCurrent() {
		return getFloatValue(FloatingPointInverterModelRegister.CurrentTotal);
	}

	@Override
	public Float getVoltage() {
		final int modelId = (getModelId() != null ? getModelId().getId() : -1);
		int count = 0;
		float total = 0;
		Float f = getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseANeutral);
		if ( f != null ) {
			total = f.floatValue();
			count++;
		}
		if ( modelId > InverterModelId.SinglePhaseInverterFloatingPoint.getId() ) {
			Float f2 = getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseBNeutral);
			if ( f2 != null ) {
				total += f2.floatValue();
				count++;
			}
		}
		if ( modelId > InverterModelId.SplitPhaseInverterFloatingPoint.getId() ) {
			Float f3 = getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseCNeutral);
			if ( f3 != null ) {
				total += f3.floatValue();
				count++;
			}
		}

		return (count > 0 ? total / count : null);
	}

	@Override
	public Float getPowerFactor() {
		return getPowerFactorValue(FloatingPointInverterModelRegister.PowerFactorAverage);
	}

	@Override
	public Integer getActivePower() {
		return getIntegerValue(FloatingPointInverterModelRegister.ActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getIntegerValue(FloatingPointInverterModelRegister.ApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getIntegerValue(FloatingPointInverterModelRegister.ReactivePowerTotal);
	}

	@Override
	public Long getActiveEnergyExported() {
		return getLongValue(FloatingPointInverterModelRegister.ActiveEnergyExportedTotal);
	}

	@Override
	public Float getDcCurrent() {
		return getFloatValue(FloatingPointInverterModelRegister.DcCurrentTotal);
	}

	@Override
	public Float getDcVoltage() {
		return getFloatValue(FloatingPointInverterModelRegister.DcVoltageTotal);
	}

	@Override
	public Integer getDcPower() {
		return getIntegerValue(FloatingPointInverterModelRegister.DcPowerTotal);
	}

	@Override
	public Float getCabinetTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureCabinet);
	}

	@Override
	public Float getHeatSinkTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureHeatSink);
	}

	@Override
	public Float getTransformerTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureTransformer);
	}

	@Override
	public Float getOtherTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureOther);
	}

	@Override
	public OperatingState getOperatingState() {
		Number n = getData().getNumber(FloatingPointInverterModelRegister.OperatingState,
				getBlockAddress());
		if ( n == null ) {
			return null;
		}
		return InverterOperatingState.forCode(n.intValue());
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(FloatingPointInverterModelRegister.EventsBitmask);
		return InverterModelEvent.forBitmask(n.longValue());
	}

	@Override
	public Float getNeutralCurrent() {
		return null;
	}

	@Override
	public Float getLineVoltage() {
		final int modelId = (getModelId() != null ? getModelId().getId() : -1);
		int count = 0;
		float total = 0;
		Float f = getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseAPhaseB);
		if ( f != null ) {
			total = f.floatValue();
			count++;
		}
		if ( modelId > InverterModelId.SinglePhaseInverterFloatingPoint.getId() ) {
			Float f2 = getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseBPhaseC);
			if ( f2 != null ) {
				total += f2.floatValue();
				count++;
			}
		}
		if ( modelId > InverterModelId.SplitPhaseInverterFloatingPoint.getId() ) {
			Float f3 = getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseCPhaseA);
			if ( f3 != null ) {
				total += f3.floatValue();
				count++;
			}
		}

		return (count > 0 ? total / count : null);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getActiveEnergyExported();
	}

	@Override
	public Long getActiveEnergyReceived() {
		return null;
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public Long getApparentEnergyReceived() {
		return null;
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return null;
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return null;
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return getData().getDeviceInfo();
	}

	private class PhaseInverterModelAccessor implements InverterModelAccessor {

		private final AcPhase phase;

		private PhaseInverterModelAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public IntRange[] getAddressRanges(int maxRangeLength) {
			return FloatingPointInverterModelAccessor.this.getAddressRanges(maxRangeLength);
		}

		@Override
		public Instant getDataTimestamp() {
			return FloatingPointInverterModelAccessor.this.getDataTimestamp();
		}

		@Override
		public int getBaseAddress() {
			return FloatingPointInverterModelAccessor.this.getBaseAddress();
		}

		@Override
		public int getFixedBlockLength() {
			return FloatingPointInverterModelAccessor.this.getFixedBlockLength();
		}

		@Override
		public int getBlockAddress() {
			return FloatingPointInverterModelAccessor.this.getBlockAddress();
		}

		@Override
		public ModelId getModelId() {
			return FloatingPointInverterModelAccessor.this.getModelId();
		}

		@Override
		public int getModelLength() {
			return FloatingPointInverterModelAccessor.this.getModelLength();
		}

		@Override
		public int getRepeatingBlockInstanceLength() {
			return FloatingPointInverterModelAccessor.this.getRepeatingBlockInstanceLength();
		}

		@Override
		public InverterModelAccessor accessorForPhase(AcPhase phase) {
			return FloatingPointInverterModelAccessor.this.accessorForPhase(phase);
		}

		@Override
		public int getRepeatingBlockInstanceCount() {
			return FloatingPointInverterModelAccessor.this.getRepeatingBlockInstanceCount();
		}

		@Override
		public Float getFrequency() {
			return FloatingPointInverterModelAccessor.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			switch (phase) {
				case PhaseA:
					return getFloatValue(FloatingPointInverterModelRegister.CurrentPhaseA);

				case PhaseB:
					return getFloatValue(FloatingPointInverterModelRegister.CurrentPhaseB);

				case PhaseC:
					return getFloatValue(FloatingPointInverterModelRegister.CurrentPhaseC);

				default:
					return FloatingPointInverterModelAccessor.this.getCurrent();
			}
		}

		@Override
		public Float getVoltage() {
			switch (phase) {
				case PhaseA:
					return getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseANeutral);

				case PhaseB:
					return getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseBNeutral);

				case PhaseC:
					return getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseCNeutral);

				default:
					return FloatingPointInverterModelAccessor.this.getVoltage();
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
					return FloatingPointInverterModelAccessor.this.getPowerFactor();
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
					return FloatingPointInverterModelAccessor.this.getActivePower();
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
					return FloatingPointInverterModelAccessor.this.getApparentPower();
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
					return FloatingPointInverterModelAccessor.this.getReactivePower();
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
					return FloatingPointInverterModelAccessor.this.getActiveEnergyExported();
			}
		}

		@Override
		public Float getDcCurrent() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return FloatingPointInverterModelAccessor.this.getDcCurrent();
			}
		}

		@Override
		public Float getDcVoltage() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return FloatingPointInverterModelAccessor.this.getDcVoltage();
			}
		}

		@Override
		public Integer getDcPower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return FloatingPointInverterModelAccessor.this.getDcPower();
			}
		}

		@Override
		public Float getCabinetTemperature() {
			return FloatingPointInverterModelAccessor.this.getCabinetTemperature();
		}

		@Override
		public Float getHeatSinkTemperature() {
			return FloatingPointInverterModelAccessor.this.getHeatSinkTemperature();
		}

		@Override
		public Float getTransformerTemperature() {
			return FloatingPointInverterModelAccessor.this.getTransformerTemperature();
		}

		@Override
		public Float getOtherTemperature() {
			return FloatingPointInverterModelAccessor.this.getOtherTemperature();
		}

		@Override
		public OperatingState getOperatingState() {
			return FloatingPointInverterModelAccessor.this.getOperatingState();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return FloatingPointInverterModelAccessor.this.getEvents();
		}

		@Override
		public Float getNeutralCurrent() {
			return FloatingPointInverterModelAccessor.this.getNeutralCurrent();
		}

		@Override
		public Float getLineVoltage() {
			switch (phase) {
				case PhaseA:
					return getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseAPhaseB);

				case PhaseB:
					return getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseBPhaseC);

				case PhaseC:
					return getFloatValue(FloatingPointInverterModelRegister.VoltagePhaseCPhaseA);

				default:
					return FloatingPointInverterModelAccessor.this.getLineVoltage();
			}
		}

		@Override
		public Long getActiveEnergyDelivered() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return FloatingPointInverterModelAccessor.this.getActiveEnergyDelivered();
			}
		}

		@Override
		public Long getActiveEnergyReceived() {
			return null;
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return null;
		}

		@Override
		public Long getApparentEnergyReceived() {
			return null;
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return null;
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return null;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return FloatingPointInverterModelAccessor.this.getDeviceInfo();
		}

	}

}
