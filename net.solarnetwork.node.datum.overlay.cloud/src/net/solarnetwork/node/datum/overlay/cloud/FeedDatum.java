/* ==================================================================
 * FeedDatum.java - 7/07/2022 10:30:04 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.overlay.cloud;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.datum.EnergyStorageDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;

/**
 * Datum for feed data.
 * 
 * @author matt
 * @version 1.0
 */
public class FeedDatum extends SimpleAcEnergyDatum implements EnergyStorageDatum {

	private static final long serialVersionUID = -2627650132197287299L;

	/**
	 * A status sample key for total capacity energy values.
	 */
	public static final String CAPACITY_WATT_HOURS_KEY = "capacityWattHours";

	/**
	 * A status sample key for a state-of-health percentage.
	 */
	public static final String STATE_OF_HEALTH_PERCENTAGE_KEY = "soh";

	private final FeedDataAccessor data;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the data
	 * @param sourceId
	 *        the source ID
	 * @param phase
	 *        the phase
	 */
	public FeedDatum(FeedDataAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	private void populateMeasurements(FeedDataAccessor data) {
		setFrequency(data.getFrequency());
		setCurrent(data.getCurrent());
		setVoltage(data.getVoltage());
		setNeutralCurrent(data.getNeutralCurrent());
		setPowerFactor(data.getPowerFactor());
		setWatts(data.getActivePower());
		setAvailableEnergy(data.getAvailableEnergy());
		setAvailableEnergyPercentage(data.getAvailableEnergyPercentage());
		setEnergyCapacity(data.getEnergyCapacity());
		setStateOfHealthPercentage(data.getStateOfHealthPercentage());
		populatePhaseMeasurementProperties(data);
	}

	@Override
	public void populatePhaseMeasurementProperties(AcEnergyDataAccessor data) {
		Set<AcPhase> phases = EnumSet.of(AcPhase.PhaseA, AcPhase.PhaseB, AcPhase.PhaseC);
		for ( AcPhase phase : phases ) {
			AcEnergyDataAccessor acc = data.accessorForPhase(phase);
			setCurrent(phase, acc.getCurrent());
			setVoltage(phase, acc.getVoltage());
			putSampleValue(Instantaneous, phase.withKey(WATTS_KEY), acc.getActivePower());
			putSampleValue(Instantaneous, phase.withKey(POWER_FACTOR_KEY), acc.getPowerFactor());
		}
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public FeedDataAccessor getData() {
		return data;
	}

	/**
	 * Get the energy capacity, in Wh.
	 * 
	 * @return the capacity
	 */
	public Long getEnergyCapacity() {
		return getSampleLong(Instantaneous, CAPACITY_WATT_HOURS_KEY);
	}

	/**
	 * Set the energy capacity.
	 * 
	 * @param capacity
	 *        the capacity, in WH
	 */
	public void setEnergyCapacity(Long capacity) {
		putSampleValue(Status, CAPACITY_WATT_HOURS_KEY, capacity);
	}

	/**
	 * Get the state of health percentage.
	 * 
	 * @return the percentage
	 */
	public Float getStateOfHealthPercentage() {
		return getSampleFloat(Instantaneous, STATE_OF_HEALTH_PERCENTAGE_KEY);
	}

	/**
	 * Set the state-of-health percentage.
	 * 
	 * @param value
	 *        the percentage
	 */
	public void setStateOfHealthPercentage(Float value) {
		putSampleValue(Instantaneous, STATE_OF_HEALTH_PERCENTAGE_KEY, value);
	}

}
