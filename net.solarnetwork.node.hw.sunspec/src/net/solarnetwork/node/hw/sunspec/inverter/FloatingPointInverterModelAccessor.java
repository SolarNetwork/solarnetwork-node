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

import java.math.BigInteger;
import java.time.Instant;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
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
 * Data access object for an floating point inverter models.
 *
 * @author matt
 * @version 3.2
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
	public @Nullable Float getPowerFactorValue(ModbusReference ref) {
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
	public @Nullable Float getFrequency() {
		return getFloatValue(FloatingPointInverterModelRegister.Frequency);
	}

	@Override
	public @Nullable Float getCurrent() {
		return getFloatValue(FloatingPointInverterModelRegister.CurrentTotal);
	}

	@Override
	public @Nullable Float getVoltage() {
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
	public @Nullable Float getPowerFactor() {
		return getPowerFactorValue(FloatingPointInverterModelRegister.PowerFactorAverage);
	}

	@Override
	public @Nullable Integer getActivePower() {
		return getIntegerValue(FloatingPointInverterModelRegister.ActivePowerTotal);
	}

	@Override
	public @Nullable Integer getApparentPower() {
		return getIntegerValue(FloatingPointInverterModelRegister.ApparentPowerTotal);
	}

	@Override
	public @Nullable Integer getReactivePower() {
		return getIntegerValue(FloatingPointInverterModelRegister.ReactivePowerTotal);
	}

	@Override
	public @Nullable Long getActiveEnergyExported() {
		return getLongValue(FloatingPointInverterModelRegister.ActiveEnergyExportedTotal);
	}

	@Override
	public @Nullable Float getDcCurrent() {
		return getFloatValue(FloatingPointInverterModelRegister.DcCurrentTotal);
	}

	@Override
	public @Nullable Float getDcVoltage() {
		return getFloatValue(FloatingPointInverterModelRegister.DcVoltageTotal);
	}

	@Override
	public @Nullable Integer getDcPower() {
		return getIntegerValue(FloatingPointInverterModelRegister.DcPowerTotal);
	}

	@Override
	public @Nullable Float getCabinetTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureCabinet);
	}

	@Override
	public @Nullable Float getHeatSinkTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureHeatSink);
	}

	@Override
	public @Nullable Float getTransformerTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureTransformer);
	}

	@Override
	public @Nullable Float getOtherTemperature() {
		return getFloatValue(FloatingPointInverterModelRegister.TemperatureOther);
	}

	@Override
	public @Nullable OperatingState getOperatingState() {
		Number n = getData().getNumber(FloatingPointInverterModelRegister.OperatingState,
				getBlockAddress());
		if ( n == null ) {
			return null;
		}
		return InverterOperatingState.forCode(n.intValue());
	}

	@Override
	public @Nullable Integer getVendorOperatingState() {
		Number n = getData().getNumber(FloatingPointInverterModelRegister.OperatingStateVendor,
				getBlockAddress());
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(FloatingPointInverterModelRegister.EventsBitmask);
		return InverterModelEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public @Nullable BitSet getVendorEvents() {
		BitSet s = new BitSet(128);
		BigInteger n = NumberUtils.bigIntegerForNumber(
				getBitfield(FloatingPointInverterModelRegister.EventsVendorBitmask));
		if ( n != null ) {
			BitSet s1 = NumberUtils.bitSetForBigInteger(n);
			s.or(s1);
		}
		n = NumberUtils.bigIntegerForNumber(
				getBitfield(FloatingPointInverterModelRegister.Events2VendorBitmask));
		if ( n != null ) {
			BitSet s1 = NumberUtils.bitSetForBigInteger(n.shiftLeft(32));
			s.or(s1);
		}
		n = NumberUtils.bigIntegerForNumber(
				getBitfield(FloatingPointInverterModelRegister.Events3VendorBitmask));
		if ( n != null ) {
			BitSet s1 = NumberUtils.bitSetForBigInteger(n.shiftLeft(64));
			s.or(s1);
		}
		n = NumberUtils.bigIntegerForNumber(
				getBitfield(FloatingPointInverterModelRegister.Events4VendorBitmask));
		if ( n != null ) {
			BitSet s1 = NumberUtils.bitSetForBigInteger(n.shiftLeft(96));
			s.or(s1);
		}
		return (s.length() > 0 ? s : null);
	}

	@Override
	public @Nullable Float getNeutralCurrent() {
		return null;
	}

	@Override
	public @Nullable Float getLineVoltage() {
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
	public @Nullable Long getActiveEnergyDelivered() {
		return getActiveEnergyExported();
	}

	@Override
	public @Nullable Long getActiveEnergyReceived() {
		return null;
	}

	@Override
	public @Nullable Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public @Nullable Long getApparentEnergyReceived() {
		return null;
	}

	@Override
	public @Nullable Long getReactiveEnergyDelivered() {
		return null;
	}

	@Override
	public @Nullable Long getReactiveEnergyReceived() {
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
		public @Nullable Instant getDataTimestamp() {
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
		public @Nullable Float getFrequency() {
			return FloatingPointInverterModelAccessor.this.getFrequency();
		}

		@Override
		public @Nullable Float getCurrent() {
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
		public @Nullable Float getVoltage() {
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
		public @Nullable Float getPowerFactor() {
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
		public @Nullable Integer getActivePower() {
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
		public @Nullable Integer getApparentPower() {
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
		public @Nullable Integer getReactivePower() {
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
		public @Nullable Long getActiveEnergyExported() {
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
		public @Nullable Float getDcCurrent() {
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
		public @Nullable Float getDcVoltage() {
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
		public @Nullable Integer getDcPower() {
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
		public @Nullable Float getCabinetTemperature() {
			return FloatingPointInverterModelAccessor.this.getCabinetTemperature();
		}

		@Override
		public @Nullable Float getHeatSinkTemperature() {
			return FloatingPointInverterModelAccessor.this.getHeatSinkTemperature();
		}

		@Override
		public @Nullable Float getTransformerTemperature() {
			return FloatingPointInverterModelAccessor.this.getTransformerTemperature();
		}

		@Override
		public @Nullable Float getOtherTemperature() {
			return FloatingPointInverterModelAccessor.this.getOtherTemperature();
		}

		@Override
		public @Nullable OperatingState getOperatingState() {
			return FloatingPointInverterModelAccessor.this.getOperatingState();
		}

		@Override
		public @Nullable Integer getVendorOperatingState() {
			return FloatingPointInverterModelAccessor.this.getVendorOperatingState();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return FloatingPointInverterModelAccessor.this.getEvents();
		}

		@Override
		public @Nullable BitSet getVendorEvents() {
			return FloatingPointInverterModelAccessor.this.getVendorEvents();
		}

		@Override
		public @Nullable Float getNeutralCurrent() {
			return FloatingPointInverterModelAccessor.this.getNeutralCurrent();
		}

		@Override
		public @Nullable Float getLineVoltage() {
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
		public @Nullable Long getActiveEnergyDelivered() {
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
		public @Nullable Long getActiveEnergyReceived() {
			return null;
		}

		@Override
		public @Nullable Long getApparentEnergyDelivered() {
			return null;
		}

		@Override
		public @Nullable Long getApparentEnergyReceived() {
			return null;
		}

		@Override
		public @Nullable Long getReactiveEnergyDelivered() {
			return null;
		}

		@Override
		public @Nullable Long getReactiveEnergyReceived() {
			return null;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return FloatingPointInverterModelAccessor.this.getDeviceInfo();
		}

	}

}
