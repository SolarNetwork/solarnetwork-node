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

import java.time.Instant;
import java.util.BitSet;
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
import net.solarnetwork.util.NumberUtils;

/**
 * Data access object for an integer inverter model.
 * 
 * @author matt
 * @version 3.1
 */
public class IntegerInverterModelAccessor extends BaseModelAccessor implements InverterModelAccessor {

	/** The integer inverter model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 50;

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
	public IntegerInverterModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
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
	public IntegerInverterModelAccessor(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	/**
	 * Get a frequency register value.
	 * 
	 * @param ref
	 *        the register reference to read
	 * @return the register value, interpreted as a frequency value
	 */
	public Float getFrequencyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorFrequency);
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get a current register value.
	 * 
	 * @param ref
	 *        the register reference to read
	 * @return the register value, interpreted as a current value
	 */
	public Float getCurrentValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorCurrent);
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get a voltage register value.
	 * 
	 * @param ref
	 *        the register reference to read
	 * @return the register value, interpreted as a voltage value
	 */
	public Float getVoltageValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorVoltage);
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get a power factor register value.
	 * 
	 * @param ref
	 *        the register reference to read
	 * @return the register value, interpreted as a power factor value
	 */
	public Float getPowerFactorValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorPowerFactor);
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

	/**
	 * Get an active power register value.
	 * 
	 * @param ref
	 *        the register reference
	 * @return the register value, interpreted as an active power value
	 */
	public Integer getActivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorActivePower);
		return (n != null ? n.intValue() : null);
	}

	/**
	 * Get an apparent power register value.
	 * 
	 * @param ref
	 *        the register reference
	 * @return the register value, interpreted as an apparent power value
	 */
	public Integer getApparentPowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorApparentPower);
		return (n != null ? n.intValue() : null);
	}

	/**
	 * Get an reactive power register value.
	 * 
	 * @param ref
	 *        the register reference
	 * @return the register value, interpreted as an reactive power value
	 */
	public Integer getReactivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorReactivePower);
		return (n != null ? n.intValue() : null);
	}

	/**
	 * Get an active energy register value.
	 * 
	 * @param ref
	 *        the register reference
	 * @return the register value, interpreted as an active energy value
	 */
	public Long getActiveEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorActiveEnergy);
		return (n != null ? n.longValue() : null);
	}

	/**
	 * Get a temperature register value.
	 * 
	 * @param ref
	 *        the register reference
	 * @return the register value, interpreted as a temperature value
	 */
	public Float getTemperatureValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerInverterModelRegister.ScaleFactorTemperature);
		return (n != null ? n.floatValue() : null);
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
	public Integer getVendorOperatingState() {
		Number n = getData().getNumber(IntegerInverterModelRegister.OperatingStateVendor,
				getBlockAddress());
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(IntegerInverterModelRegister.EventsBitmask);
		return InverterModelEvent.forBitmask(n.longValue());
	}

	@Override
	public BitSet getVendorEvents() {
		BitSet s = new BitSet(128);
		Number n = getBitfield(IntegerInverterModelRegister.EventsVendorBitmask);
		if ( n != null ) {
			BitSet s1 = NumberUtils.bitSetForBigInteger(NumberUtils.bigIntegerForNumber(n));
			s.or(s1);
		}
		n = getBitfield(IntegerInverterModelRegister.Events2VendorBitmask);
		if ( n != null ) {
			BitSet s1 = NumberUtils
					.bitSetForBigInteger(NumberUtils.bigIntegerForNumber(n).shiftLeft(32));
			s.or(s1);
		}
		n = getBitfield(IntegerInverterModelRegister.Events3VendorBitmask);
		if ( n != null ) {
			BitSet s1 = NumberUtils
					.bitSetForBigInteger(NumberUtils.bigIntegerForNumber(n).shiftLeft(64));
			s.or(s1);
		}
		n = getBitfield(IntegerInverterModelRegister.Events4VendorBitmask);
		if ( n != null ) {
			BitSet s1 = NumberUtils
					.bitSetForBigInteger(NumberUtils.bigIntegerForNumber(n).shiftLeft(96));
			s.or(s1);
		}
		return (s.length() > 0 ? s : null);
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
		Float f = getVoltageValue(IntegerInverterModelRegister.VoltagePhaseAPhaseB);
		if ( f != null ) {
			total = f.floatValue();
			count++;
		}
		if ( modelId > InverterModelId.SinglePhaseInverterInteger.getId() ) {
			Float f2 = getVoltageValue(IntegerInverterModelRegister.VoltagePhaseBPhaseC);
			if ( f2 != null ) {
				total += f2.floatValue();
				count++;
			}
		}
		if ( modelId > InverterModelId.SplitPhaseInverterInteger.getId() ) {
			Float f3 = getVoltageValue(IntegerInverterModelRegister.VoltagePhaseCPhaseA);
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
			return IntegerInverterModelAccessor.this.getAddressRanges(maxRangeLength);
		}

		@Override
		public Instant getDataTimestamp() {
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
		public InverterModelAccessor accessorForPhase(AcPhase phase) {
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
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return IntegerInverterModelAccessor.this.getDcCurrent();
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
					return IntegerInverterModelAccessor.this.getDcVoltage();
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
					return IntegerInverterModelAccessor.this.getDcPower();
			}
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
		public Integer getVendorOperatingState() {
			return IntegerInverterModelAccessor.this.getVendorOperatingState();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return IntegerInverterModelAccessor.this.getEvents();
		}

		@Override
		public BitSet getVendorEvents() {
			return IntegerInverterModelAccessor.this.getVendorEvents();
		}

		@Override
		public Float getNeutralCurrent() {
			return IntegerInverterModelAccessor.this.getNeutralCurrent();
		}

		@Override
		public Float getLineVoltage() {
			switch (phase) {
				case PhaseA:
					return getVoltageValue(IntegerInverterModelRegister.VoltagePhaseAPhaseB);

				case PhaseB:
					return getVoltageValue(IntegerInverterModelRegister.VoltagePhaseBPhaseC);

				case PhaseC:
					return getVoltageValue(IntegerInverterModelRegister.VoltagePhaseCPhaseA);

				default:
					return IntegerInverterModelAccessor.this.getLineVoltage();
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
					return IntegerInverterModelAccessor.this.getActiveEnergyDelivered();
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
			return IntegerInverterModelAccessor.this.getDeviceInfo();
		}

	}

}
