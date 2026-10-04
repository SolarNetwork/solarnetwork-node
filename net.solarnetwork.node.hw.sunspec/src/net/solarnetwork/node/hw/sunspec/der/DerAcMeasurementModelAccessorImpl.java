/* ==================================================================
 * DerAcMeasurementModelAccessorImpl.java - 5/10/2026 8:27:22 am
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

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;

/**
 * Implementation of {@link DerAcMeasurementModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerAcMeasurementModelAccessorImpl extends BaseModelAccessor
		implements DerAcMeasurementModelAccessor {

	/** The DER AC measurement model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 153;

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
	public DerAcMeasurementModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerAcMeasurementModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	private @Nullable Float scaledFloat(DerAcMeasurementModelRegister ref,
			DerAcMeasurementModelRegister scaleRef) {
		Number n = getScaledValue(ref, scaleRef);
		return (n != null ? n.floatValue() : null);
	}

	private @Nullable Integer scaledInteger(DerAcMeasurementModelRegister ref,
			DerAcMeasurementModelRegister scaleRef) {
		Number n = getScaledValue(ref, scaleRef);
		return (n != null ? n.intValue() : null);
	}

	private @Nullable Long scaledLong(DerAcMeasurementModelRegister ref,
			DerAcMeasurementModelRegister scaleRef) {
		Number n = getScaledValue(ref, scaleRef);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public InverterModelAccessor accessorForPhase(AcPhase phase) {
		switch (phase) {
			case PhaseA:
			case PhaseB:
			case PhaseC:
				return new PhaseAccessor(phase);

			default:
				return this;
		}
	}

	@Override
	public @Nullable DerAcWiringType getAcWiringType() {
		return getCodedValue(DerAcMeasurementModelRegister.AcWiringType, DerAcWiringType.class);
	}

	@Override
	public @Nullable DerOperatingState getDerOperatingState() {
		return getCodedValue(DerAcMeasurementModelRegister.OperatingState, DerOperatingState.class);
	}

	@Override
	public @Nullable DerInverterState getInverterState() {
		return getCodedValue(DerAcMeasurementModelRegister.InverterState, DerInverterState.class);
	}

	@Override
	public @Nullable OperatingState getOperatingState() {
		DerInverterState state = getInverterState();
		return (state != null ? state.asInverterOperatingState() : null);
	}

	@Override
	public @Nullable DerGridConnectionState getGridConnectionState() {
		return getCodedValue(DerAcMeasurementModelRegister.GridConnectionState,
				DerGridConnectionState.class);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(DerAcMeasurementModelRegister.AlarmsBitmask);
		return DerAlarm.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public Set<DerOperationalCharacteristic> getOperationalCharacteristics() {
		return getBitmaskableValues(DerAcMeasurementModelRegister.OperationalCharacteristicsBitmask,
				DerOperationalCharacteristic.class);
	}

	@Override
	public @Nullable Integer getActivePower() {
		return scaledInteger(DerAcMeasurementModelRegister.ActivePowerTotal,
				DerAcMeasurementModelRegister.ScaleFactorActivePower);
	}

	@Override
	public @Nullable Integer getApparentPower() {
		return scaledInteger(DerAcMeasurementModelRegister.ApparentPowerTotal,
				DerAcMeasurementModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public @Nullable Integer getReactivePower() {
		return scaledInteger(DerAcMeasurementModelRegister.ReactivePowerTotal,
				DerAcMeasurementModelRegister.ScaleFactorReactivePower);
	}

	@Override
	public @Nullable Float getPowerFactor() {
		return scaledFloat(DerAcMeasurementModelRegister.PowerFactorTotal,
				DerAcMeasurementModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable Float getCurrent() {
		return scaledFloat(DerAcMeasurementModelRegister.CurrentTotal,
				DerAcMeasurementModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Float getNeutralCurrent() {
		return null;
	}

	@Override
	public @Nullable Float getVoltage() {
		return scaledFloat(DerAcMeasurementModelRegister.VoltageLineNeutralAverage,
				DerAcMeasurementModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getLineVoltage() {
		return scaledFloat(DerAcMeasurementModelRegister.VoltageLineLineAverage,
				DerAcMeasurementModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getFrequency() {
		return scaledFloat(DerAcMeasurementModelRegister.Frequency,
				DerAcMeasurementModelRegister.ScaleFactorFrequency);
	}

	@Override
	public @Nullable Long getActiveEnergyExported() {
		return getActiveEnergyDelivered();
	}

	@Override
	public @Nullable Long getActiveEnergyDelivered() {
		return scaledLong(DerAcMeasurementModelRegister.ActiveEnergyInjectedTotal,
				DerAcMeasurementModelRegister.ScaleFactorActiveEnergy);
	}

	@Override
	public @Nullable Long getActiveEnergyReceived() {
		return scaledLong(DerAcMeasurementModelRegister.ActiveEnergyAbsorbedTotal,
				DerAcMeasurementModelRegister.ScaleFactorActiveEnergy);
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
		return scaledLong(DerAcMeasurementModelRegister.ReactiveEnergyInjectedTotal,
				DerAcMeasurementModelRegister.ScaleFactorReactiveEnergy);
	}

	@Override
	public @Nullable Long getReactiveEnergyReceived() {
		return scaledLong(DerAcMeasurementModelRegister.ReactiveEnergyAbsorbedTotal,
				DerAcMeasurementModelRegister.ScaleFactorReactiveEnergy);
	}

	@Override
	public @Nullable Float getDcCurrent() {
		return null;
	}

	@Override
	public @Nullable Float getDcVoltage() {
		return null;
	}

	@Override
	public @Nullable Integer getDcPower() {
		return null;
	}

	@Override
	public @Nullable Float getAmbientTemperature() {
		return scaledFloat(DerAcMeasurementModelRegister.TemperatureAmbient,
				DerAcMeasurementModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Float getCabinetTemperature() {
		return scaledFloat(DerAcMeasurementModelRegister.TemperatureCabinet,
				DerAcMeasurementModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Float getHeatSinkTemperature() {
		return scaledFloat(DerAcMeasurementModelRegister.TemperatureHeatSink,
				DerAcMeasurementModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Float getTransformerTemperature() {
		return scaledFloat(DerAcMeasurementModelRegister.TemperatureTransformer,
				DerAcMeasurementModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Float getSwitchTemperature() {
		return scaledFloat(DerAcMeasurementModelRegister.TemperatureSwitch,
				DerAcMeasurementModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Float getOtherTemperature() {
		return scaledFloat(DerAcMeasurementModelRegister.TemperatureOther,
				DerAcMeasurementModelRegister.ScaleFactorTemperature);
	}

	@Override
	public @Nullable Integer getThrottlePercent() {
		return getIntegerValue(DerAcMeasurementModelRegister.ThrottlePercent);
	}

	@Override
	public Set<DerThrottleSource> getThrottleSources() {
		return getBitmaskableValues(DerAcMeasurementModelRegister.ThrottleSourcesBitmask,
				DerThrottleSource.class);
	}

	@Override
	public @Nullable String getManufacturerAlarmInfo() {
		return getStringValue(DerAcMeasurementModelRegister.ManufacturerAlarmInfo);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return getData().getDeviceInfo();
	}

	/**
	 * Phase-specific accessor.
	 */
	private class PhaseAccessor implements InverterModelAccessor {

		private final DerAcMeasurementModelRegister activePower;
		private final DerAcMeasurementModelRegister apparentPower;
		private final DerAcMeasurementModelRegister reactivePower;
		private final DerAcMeasurementModelRegister powerFactor;
		private final DerAcMeasurementModelRegister current;
		private final DerAcMeasurementModelRegister lineVoltage;
		private final DerAcMeasurementModelRegister voltage;
		private final DerAcMeasurementModelRegister activeEnergyInjected;
		private final DerAcMeasurementModelRegister activeEnergyAbsorbed;
		private final DerAcMeasurementModelRegister reactiveEnergyInjected;
		private final DerAcMeasurementModelRegister reactiveEnergyAbsorbed;

		private PhaseAccessor(AcPhase phase) {
			super();
			switch (phase) {
				case PhaseA:
					activePower = DerAcMeasurementModelRegister.ActivePowerPhaseA;
					apparentPower = DerAcMeasurementModelRegister.ApparentPowerPhaseA;
					reactivePower = DerAcMeasurementModelRegister.ReactivePowerPhaseA;
					powerFactor = DerAcMeasurementModelRegister.PowerFactorPhaseA;
					current = DerAcMeasurementModelRegister.CurrentPhaseA;
					lineVoltage = DerAcMeasurementModelRegister.VoltagePhaseAPhaseB;
					voltage = DerAcMeasurementModelRegister.VoltagePhaseANeutral;
					activeEnergyInjected = DerAcMeasurementModelRegister.ActiveEnergyInjectedPhaseA;
					activeEnergyAbsorbed = DerAcMeasurementModelRegister.ActiveEnergyAbsorbedPhaseA;
					reactiveEnergyInjected = DerAcMeasurementModelRegister.ReactiveEnergyInjectedPhaseA;
					reactiveEnergyAbsorbed = DerAcMeasurementModelRegister.ReactiveEnergyAbsorbedPhaseA;
					break;

				case PhaseB:
					activePower = DerAcMeasurementModelRegister.ActivePowerPhaseB;
					apparentPower = DerAcMeasurementModelRegister.ApparentPowerPhaseB;
					reactivePower = DerAcMeasurementModelRegister.ReactivePowerPhaseB;
					powerFactor = DerAcMeasurementModelRegister.PowerFactorPhaseB;
					current = DerAcMeasurementModelRegister.CurrentPhaseB;
					lineVoltage = DerAcMeasurementModelRegister.VoltagePhaseBPhaseC;
					voltage = DerAcMeasurementModelRegister.VoltagePhaseBNeutral;
					activeEnergyInjected = DerAcMeasurementModelRegister.ActiveEnergyInjectedPhaseB;
					activeEnergyAbsorbed = DerAcMeasurementModelRegister.ActiveEnergyAbsorbedPhaseB;
					reactiveEnergyInjected = DerAcMeasurementModelRegister.ReactiveEnergyInjectedPhaseB;
					reactiveEnergyAbsorbed = DerAcMeasurementModelRegister.ReactiveEnergyAbsorbedPhaseB;
					break;

				default:
					activePower = DerAcMeasurementModelRegister.ActivePowerPhaseC;
					apparentPower = DerAcMeasurementModelRegister.ApparentPowerPhaseC;
					reactivePower = DerAcMeasurementModelRegister.ReactivePowerPhaseC;
					powerFactor = DerAcMeasurementModelRegister.PowerFactorPhaseC;
					current = DerAcMeasurementModelRegister.CurrentPhaseC;
					lineVoltage = DerAcMeasurementModelRegister.VoltagePhaseCPhaseA;
					voltage = DerAcMeasurementModelRegister.VoltagePhaseCNeutral;
					activeEnergyInjected = DerAcMeasurementModelRegister.ActiveEnergyInjectedPhaseC;
					activeEnergyAbsorbed = DerAcMeasurementModelRegister.ActiveEnergyAbsorbedPhaseC;
					reactiveEnergyInjected = DerAcMeasurementModelRegister.ReactiveEnergyInjectedPhaseC;
					reactiveEnergyAbsorbed = DerAcMeasurementModelRegister.ReactiveEnergyAbsorbedPhaseC;
					break;
			}
		}

		@Override
		public @Nullable Instant getDataTimestamp() {
			return DerAcMeasurementModelAccessorImpl.this.getDataTimestamp();
		}

		@Override
		public int getBaseAddress() {
			return DerAcMeasurementModelAccessorImpl.this.getBaseAddress();
		}

		@Override
		public int getBlockAddress() {
			return DerAcMeasurementModelAccessorImpl.this.getBlockAddress();
		}

		@Override
		public ModelId getModelId() {
			return DerAcMeasurementModelAccessorImpl.this.getModelId();
		}

		@Override
		public int getFixedBlockLength() {
			return DerAcMeasurementModelAccessorImpl.this.getFixedBlockLength();
		}

		@Override
		public int getModelLength() {
			return DerAcMeasurementModelAccessorImpl.this.getModelLength();
		}

		@Override
		public InverterModelAccessor accessorForPhase(AcPhase phase) {
			return DerAcMeasurementModelAccessorImpl.this.accessorForPhase(phase);
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return DerAcMeasurementModelAccessorImpl.this.getDeviceInfo();
		}

		@Override
		public @Nullable Integer getActivePower() {
			return scaledInteger(activePower, DerAcMeasurementModelRegister.ScaleFactorActivePower);
		}

		@Override
		public @Nullable Integer getApparentPower() {
			return scaledInteger(apparentPower, DerAcMeasurementModelRegister.ScaleFactorApparentPower);
		}

		@Override
		public @Nullable Integer getReactivePower() {
			return scaledInteger(reactivePower, DerAcMeasurementModelRegister.ScaleFactorReactivePower);
		}

		@Override
		public @Nullable Float getPowerFactor() {
			return scaledFloat(powerFactor, DerAcMeasurementModelRegister.ScaleFactorPowerFactor);
		}

		@Override
		public @Nullable Float getCurrent() {
			return scaledFloat(current, DerAcMeasurementModelRegister.ScaleFactorCurrent);
		}

		@Override
		public @Nullable Float getNeutralCurrent() {
			return null;
		}

		@Override
		public @Nullable Float getVoltage() {
			return scaledFloat(voltage, DerAcMeasurementModelRegister.ScaleFactorVoltage);
		}

		@Override
		public @Nullable Float getLineVoltage() {
			return scaledFloat(lineVoltage, DerAcMeasurementModelRegister.ScaleFactorVoltage);
		}

		@Override
		public @Nullable Float getFrequency() {
			return DerAcMeasurementModelAccessorImpl.this.getFrequency();
		}

		@Override
		public @Nullable Long getActiveEnergyExported() {
			return getActiveEnergyDelivered();
		}

		@Override
		public @Nullable Long getActiveEnergyDelivered() {
			return scaledLong(activeEnergyInjected,
					DerAcMeasurementModelRegister.ScaleFactorActiveEnergy);
		}

		@Override
		public @Nullable Long getActiveEnergyReceived() {
			return scaledLong(activeEnergyAbsorbed,
					DerAcMeasurementModelRegister.ScaleFactorActiveEnergy);
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
			return scaledLong(reactiveEnergyInjected,
					DerAcMeasurementModelRegister.ScaleFactorReactiveEnergy);
		}

		@Override
		public @Nullable Long getReactiveEnergyReceived() {
			return scaledLong(reactiveEnergyAbsorbed,
					DerAcMeasurementModelRegister.ScaleFactorReactiveEnergy);
		}

		@Override
		public @Nullable Float getDcCurrent() {
			return null;
		}

		@Override
		public @Nullable Float getDcVoltage() {
			return null;
		}

		@Override
		public @Nullable Integer getDcPower() {
			return null;
		}

		@Override
		public @Nullable Float getCabinetTemperature() {
			return DerAcMeasurementModelAccessorImpl.this.getCabinetTemperature();
		}

		@Override
		public @Nullable Float getHeatSinkTemperature() {
			return DerAcMeasurementModelAccessorImpl.this.getHeatSinkTemperature();
		}

		@Override
		public @Nullable Float getTransformerTemperature() {
			return DerAcMeasurementModelAccessorImpl.this.getTransformerTemperature();
		}

		@Override
		public @Nullable Float getOtherTemperature() {
			return DerAcMeasurementModelAccessorImpl.this.getOtherTemperature();
		}

		@Override
		public @Nullable OperatingState getOperatingState() {
			return DerAcMeasurementModelAccessorImpl.this.getOperatingState();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return DerAcMeasurementModelAccessorImpl.this.getEvents();
		}

	}

}
