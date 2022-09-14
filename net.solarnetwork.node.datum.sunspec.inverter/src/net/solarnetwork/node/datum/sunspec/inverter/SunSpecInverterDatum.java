/* ==================================================================
 * SunSpecInverterDatum.java - 9/10/2018 10:15:56 AM
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

import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelEvent;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor.DcModule;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;

/**
 * Datum for a SunSpec compatible inverter.
 * 
 * @author matt
 * @version 2.0
 */
public class SunSpecInverterDatum extends SimpleAcDcEnergyDatum {

	private static final long serialVersionUID = -7099916188917660111L;

	/**
	 * The status sample key for {@link #getOperatingState()} values.
	 */
	public static final String OPERATING_STATE_KEY = "sunsOpState";

	/**
	 * The status sample key for {@link #getEvents()} values.
	 */
	public static final String EVENTS_KEY = "events";

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
	public SunSpecInverterDatum(InverterModelAccessor data, String sourceId, AcPhase phase) {
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
		setApparentPower(data.getApparentPower());
		setReactivePower(data.getReactivePower());

		setDcVoltage(data.getDcVoltage());
		setDcPower(data.getDcPower());

		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyExported());

		if ( data.getOperatingState() != null ) {
			setOperatingState(data.getOperatingState());
			setDeviceOperatingState(data.getOperatingState().asDeviceOperatingState());
		}

		getSamples().putInstantaneousSampleValue("temp", data.getCabinetTemperature());
		getSamples().putInstantaneousSampleValue("temp_heatSink", data.getHeatSinkTemperature());
		getSamples().putInstantaneousSampleValue("temp_transformer", data.getTransformerTemperature());
		getSamples().putInstantaneousSampleValue("temp_other", data.getOtherTemperature());

		setEvents(data.getEvents());
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
	 * @since 1.1
	 */
	public void populateDcModulesProperties(InverterMpptExtensionModelAccessor mppt) {
		List<DcModule> modules = (mppt != null ? mppt.getDcModules() : null);
		if ( modules == null || modules.isEmpty() ) {
			return;
		}
		for ( DcModule module : modules ) {
			Integer moduleId = module.getInputId();
			Float moduleVoltage = module.getDCVoltage();
			Integer modulePower = module.getDCPower();
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
	 * @return the operating state, or {@literal null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public OperatingState getOperatingState() {
		Integer code = getSamples().getStatusSampleInteger(OPERATING_STATE_KEY);
		OperatingState result = null;
		if ( code != null ) {
			try {
				result = InverterOperatingState.forCode(code);
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
	 *        the state to set, or {@literal null}
	 */
	public void setOperatingState(OperatingState state) {
		Integer code = (state != null ? state.getCode() : null);
		getSamples().putStatusSampleValue(OPERATING_STATE_KEY, code);
	}

	/**
	 * Get the device operating state.
	 * 
	 * @return the device operating state, or {@literal null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public DeviceOperatingState getDeviceOperatingState() {
		DeviceOperatingState result = null;
		Integer code = getSamples().getStatusSampleInteger(Datum.OP_STATE);
		if ( code != null ) {
			try {
				result = DeviceOperatingState.forCode(code);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		} else {
			OperatingState opState = getOperatingState();
			if ( opState != null ) {
				result = opState.asDeviceOperatingState();
			}
		}
		return result;
	}

	/**
	 * Set the operating state.
	 * 
	 * @param state
	 *        the state to set, or {@literal null}
	 */
	public void setDeviceOperatingState(DeviceOperatingState state) {
		Integer code = (state != null ? state.getCode() : null);
		getSamples().putStatusSampleValue(Datum.OP_STATE, code);
	}

	/**
	 * Get the operating state.
	 * 
	 * @return the operating state, or {@literal null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public Set<ModelEvent> getEvents() {
		Long bitmask = getSamples().getStatusSampleLong(EVENTS_KEY);
		Set<ModelEvent> result = null;
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
	 *        the events to set, or {@literal null}
	 */
	public void setEvents(Set<ModelEvent> events) {
		long bitmask = ModelEvent.bitField32Value(events);
		getSamples().putStatusSampleValue(EVENTS_KEY, bitmask);
	}

}
