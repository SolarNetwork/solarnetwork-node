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

import java.util.Date;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelEvent;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor.DcModule;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Datum for a SunSpec compatible inverter.
 * 
 * @author matt
 * @version 1.1
 */
public class SunSpecInverterDatum extends GeneralNodeACEnergyDatum implements PVEnergyDatum {

	/**
	 * The status sample key for {@link #getOperatingState()} values.
	 */
	public static final String OPERATING_STATE_KEY = "opState";

	/**
	 * The status sample key for {@link #getEvents()} values.
	 */
	public static final String EVENTS_KEY = "events";

	private final InverterModelAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 * @param phase
	 *        the phase to associate with the data
	 * @param backwards
	 *        if {@literal true} then treat the meter as being installed
	 *        backwards with respect to the current direction; in this case
	 *        certain instantaneous measurements will be negated and certain
	 *        accumulating properties will be switched (like <i>received</i>
	 *        energy will be captured as {@code wattHours} and <i>delivered</i>
	 *        energy as {@code wattHoursReverse})
	 */
	public SunSpecInverterDatum(InverterModelAccessor data, ACPhase phase) {
		super();
		this.data = data;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		populateMeasurements(data, phase);
	}

	private void populateMeasurements(InverterModelAccessor data, ACPhase phase) {
		setPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setPowerFactor(data.getPowerFactor());
		setApparentPower(data.getApparentPower());
		setReactivePower(data.getReactivePower());

		setDCVoltage(data.getDcVoltage());
		setDCPower(data.getDcPower());

		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyExported());

		setOperatingState(data.getOperatingState());
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
			putInstantaneousSampleValue(modulePropertyName(DC_VOLTAGE_KEY, moduleId), moduleVoltage);
			putInstantaneousSampleValue(modulePropertyName(DC_POWER_KEY, moduleId), module.getDCPower());
			putAccumulatingSampleValue(modulePropertyName(WATT_HOUR_READING_KEY, moduleId),
					module.getDCEnergyDelivered());
			putInstantaneousSampleValue(modulePropertyName("temp", moduleId), module.getTemperature());

			OperatingState moduleState = module.getOperatingState();
			putStatusSampleValue(modulePropertyName(OPERATING_STATE_KEY, moduleId),
					moduleState != null ? moduleState.getCode() : null);

			long moduleEvents = ModelEvent.bitField32Value(module.getEvents());
			putStatusSampleValue(modulePropertyName(EVENTS_KEY, moduleId), moduleEvents);
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

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getDCPower() {
		return getInstantaneousSampleInteger(DC_POWER_KEY);
	}

	public void setDCPower(Integer value) {
		putInstantaneousSampleValue(DC_POWER_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getDCVoltage() {
		return getInstantaneousSampleFloat(DC_VOLTAGE_KEY);
	}

	public void setDCVoltage(Float value) {
		putInstantaneousSampleValue(DC_VOLTAGE_KEY, value);
	}

	/**
	 * Get the operating state.
	 * 
	 * @return the operating state, or {@literal null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public OperatingState getOperatingState() {
		Integer code = getStatusSampleInteger(OPERATING_STATE_KEY);
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
		putStatusSampleValue(OPERATING_STATE_KEY, code);
	}

	/**
	 * Get the operating state.
	 * 
	 * @return the operating state, or {@literal null}
	 */
	@JsonIgnore
	@SerializeIgnore
	public Set<ModelEvent> getEvents() {
		Long bitmask = getStatusSampleLong(EVENTS_KEY);
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
	 * @param state
	 *        the state to set, or {@literal null}
	 */
	public void setEvents(Set<ModelEvent> events) {
		long bitmask = ModelEvent.bitField32Value(events);
		putStatusSampleValue(EVENTS_KEY, bitmask);
	}

}
