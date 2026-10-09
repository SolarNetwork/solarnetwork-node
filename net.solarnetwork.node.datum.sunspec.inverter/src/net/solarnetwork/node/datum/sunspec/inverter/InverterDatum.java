/* ==================================================================
 * InverterDatum.java - 9/10/2018 10:15:56 AM
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

package net.solarnetwork.node.datum.sunspec.inverter;

import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.sunspec.api.ModelEvent;
import net.solarnetwork.sunspec.api.OperatingState;
import net.solarnetwork.sunspec.api.inverter.InverterModelAccessor;
import net.solarnetwork.sunspec.api.inverter.InverterModelEvent;
import net.solarnetwork.sunspec.api.inverter.InverterMpptExtensionModelAccessor;
import net.solarnetwork.sunspec.api.inverter.InverterMpptExtensionModelAccessor.DcModule;
import net.solarnetwork.sunspec.api.inverter.InverterOperatingState;
import net.solarnetwork.util.NumberUtils;

/**
 * Datum for a SunSpec compatible inverter.
 *
 * @author matt
 * @version 1.0
 * @since 6.0
 */
public class InverterDatum extends SimpleAcDcEnergyDatum {

	@Serial
	private static final long serialVersionUID = 2307520311615307449L;

	/**
	 * The status sample key for {@link #getOperatingState()} values.
	 */
	public static final String OPERATING_STATE_KEY = "sunsOpState";

	/**
	 * The status sample key for {@link #getEvents()} values.
	 */
	public static final String EVENTS_KEY = "events";

	/**
	 * The status sample key for {@link #getVendorEvents()} values.
	 */
	public static final String VENDOR_EVENTS_KEY = "vendorEvents";

	/** The model data. */
	private final InverterModelAccessor data;

	/**
	 * Construct from a sample.
	 *
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 * @param phase
	 *        the phase to associate with the data
	 */
	public InverterDatum(InverterModelAccessor data, @Nullable String sourceId, AcPhase phase) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data, phase);
	}

	private void populateMeasurements(InverterModelAccessor data, AcPhase phase) {
		setAcPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setPowerFactor(data.getPowerFactor());
		setApparentPower(data.getApparentPower() != null ? data.getActivePower().intValue() : null);
		setReactivePower(data.getReactivePower() != null ? data.getReactivePower().intValue() : null);

		setDcCurrent(data.getDcCurrent());
		setDcVoltage(data.getDcVoltage());
		setDcPower(data.getDcPower() != null ? data.getDcPower().intValue() : null);

		setWatts(data.getActivePower() != null ? data.getActivePower().intValue() : null);
		setWattHourReading(
				data.getActiveEnergyExported() != null ? data.getActiveEnergyExported().longValue()
						: null);

		if ( data.getOperatingState() != null ) {
			setOperatingState(data.getOperatingState());
			setDeviceOperatingState(deviceOperatingState(data.getOperatingState()));
		}

		getSamples().putInstantaneousSampleValue("temp", data.getCabinetTemperature());
		getSamples().putInstantaneousSampleValue("temp_heatSink", data.getHeatSinkTemperature());
		getSamples().putInstantaneousSampleValue("temp_transformer", data.getTransformerTemperature());
		getSamples().putInstantaneousSampleValue("temp_other", data.getOtherTemperature());

		setEvents(data.getEvents());
		setVendorEvents(data.getVendorEvents());
	}

	private String modulePropertyName(String baseName, Integer moduleId) {
		return String.format("%s_%d", baseName, moduleId);
	}

	/**
	 * Populate DC module level properties extracted from a MPPT extension model
	 * accessor.
	 *
	 * @param mppt
	 *        the MPPT accessor
	 */
	public void populateDcModulesProperties(@Nullable InverterMpptExtensionModelAccessor mppt) {
		List<DcModule> modules = (mppt != null ? mppt.getDcModules() : null);
		if ( modules == null || modules.isEmpty() ) {
			return;
		}
		for ( DcModule module : modules ) {
			Integer moduleId = module.getInputId();
			Float moduleVoltage = module.getDCVoltage();
			BigDecimal modulePower = module.getDCPower();
			if ( moduleId == null || moduleVoltage == null || modulePower == null ) {
				continue;
			}
			getSamples().putInstantaneousSampleValue(modulePropertyName(DC_VOLTAGE_KEY, moduleId),
					moduleVoltage);
			getSamples().putInstantaneousSampleValue(modulePropertyName(DC_POWER_KEY, moduleId),
					module.getDCPower());
			getSamples().putAccumulatingSampleValue(modulePropertyName(WATT_HOUR_READING_KEY, moduleId),
					module.getDCEnergyDelivered());
			getSamples().putInstantaneousSampleValue(modulePropertyName("temp", moduleId),
					module.getTemperature());

			OperatingState moduleState = module.getOperatingState();
			getSamples().putStatusSampleValue(modulePropertyName(OPERATING_STATE_KEY, moduleId),
					moduleState != null ? moduleState.getCode() : null);

			long moduleEvents = ModelEvent.bitField32Value(module.getEvents());
			getSamples().putStatusSampleValue(modulePropertyName(EVENTS_KEY, moduleId), moduleEvents);
		}
	}

	/**
	 * Get the raw data used to populate this datum.
	 *
	 * @return the data
	 */
	public InverterModelAccessor getData() {
		return data;
	}

	/**
	 * Get the operating state.
	 *
	 * @return the operating state, or {@code null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public @Nullable OperatingState getOperatingState() {
		Integer code = getSamples().getStatusSampleInteger(OPERATING_STATE_KEY);
		OperatingState result = null;
		if ( code != null ) {
			try {
				result = net.solarnetwork.sunspec.api.inverter.InverterOperatingState.forCode(code);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return result;
	}

	/**
	 * Set the operating state.
	 *
	 * @param state
	 *        the state to set, or {@code null}
	 */
	public void setOperatingState(@Nullable OperatingState state) {
		Integer code = (state != null ? state.getCode() : null);
		getSamples().putStatusSampleValue(OPERATING_STATE_KEY, code);
	}

	/**
	 * Get the device operating state.
	 *
	 * @return the device operating state, or {@code null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public @Nullable DeviceOperatingState getDeviceOperatingState() {
		DeviceOperatingState result = null;
		Integer code = getSamples().getStatusSampleInteger(Datum.OP_STATE);
		if ( code != null ) {
			try {
				result = DeviceOperatingState.forCode(code);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		} else {
			result = deviceOperatingState(getOperatingState());
		}
		return result;
	}

	/**
	 * Set the operating state.
	 *
	 * @param state
	 *        the state to set, or {@code null}
	 */
	public void setDeviceOperatingState(@Nullable DeviceOperatingState state) {
		Integer code = (state != null ? state.getCode() : null);
		getSamples().putStatusSampleValue(Datum.OP_STATE, code);
	}

	/**
	 * Get the events.
	 *
	 * @return the events, or {@code null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public @Nullable Set<? extends ModelEvent> getEvents() {
		Long bitmask = getSamples().getStatusSampleLong(EVENTS_KEY);
		Set<? extends ModelEvent> result = null;
		if ( bitmask != null ) {
			try {
				result = InverterModelEvent.forBitmask(bitmask);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return result;
	}

	/**
	 * Set the events.
	 *
	 * @param events
	 *        the events to set, or {@code null}
	 */
	public void setEvents(@Nullable Set<? extends ModelEvent> events) {
		long bitmask = ModelEvent.bitField32Value(events);
		getSamples().putStatusSampleValue(EVENTS_KEY, bitmask);
	}

	/**
	 * Get the vendor events.
	 *
	 * @return the events, or {@code null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public @Nullable BitSet getVendorEvents() {
		String ve = getSamples().getStatusSampleString(VENDOR_EVENTS_KEY);
		if ( ve != null ) {
			BigInteger bi = new BigInteger(ve, 16);
			return NumberUtils.bitSetForBigInteger(bi);
		}
		return null;
	}

	/**
	 * Set the vendor events.
	 *
	 * @param events
	 *        the vendor events
	 */
	public void setVendorEvents(@Nullable BitSet events) {
		if ( events != null && events.length() > 0 ) {
			BigInteger v = NumberUtils.bigIntegerForBitSet(events);
			if ( v != null ) {
				asMutableSampleOperations().putSampleValue(Status, VENDOR_EVENTS_KEY,
						"0x" + v.toString(16));
			}
		}
	}

	/**
	 * Get a {@link DeviceOperatingState} for a SunSpec {@code  OperatingState}.
	 *
	 * @param opState
	 *        the operating state to convert
	 * @return the device operating state, never {@code null}
	 * @since 2.0
	 */
	public static DeviceOperatingState deviceOperatingState(@Nullable OperatingState opState) {
		if ( opState == null ) {
			return DeviceOperatingState.Unknown;
		}
		final InverterOperatingState invOpState = (opState instanceof InverterOperatingState s ? s
				: InverterOperatingState.forCode(opState.getCode()));
		switch (invOpState) {
			case Normal:
			case Mppt:
				return DeviceOperatingState.Normal;

			case Off:
			case ShuttingDown:
				return DeviceOperatingState.Shutdown;

			case Sleeping:
			case Standby:
				return DeviceOperatingState.Standby;

			case Starting:
			case Test:
				return DeviceOperatingState.Starting;

			case Throttled:
				return DeviceOperatingState.Override;

			case Fault:
				return DeviceOperatingState.Fault;

			default:
				return DeviceOperatingState.Unknown;

		}
	}

}
