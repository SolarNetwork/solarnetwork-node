/* ==================================================================
 * PM5100Datum.java - 15/05/2018 7:39:50 AM
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

package net.solarnetwork.node.datum.schneider.pm5100;

import java.util.Date;
import java.util.Map;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM5100DataAccessor;

/**
 * Datum for the PM5100 meter.
 * 
 * @author matt
 * @version 1.2
 */
public class PM5100Datum extends GeneralNodeACEnergyDatum implements ACEnergyDataAccessor {

	private final PM5100DataAccessor data;
	private final boolean backwards;

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
	public PM5100Datum(PM5100DataAccessor data, ACPhase phase, boolean backwards) {
		super();
		this.data = data;
		this.backwards = backwards;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		ACEnergyDataAccessor phaseData = accessorForPhase(phase);
		populateMeasurements(phaseData, phase);
	}

	private void populateMeasurements(ACEnergyDataAccessor data, ACPhase phase) {
		setPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setNeutralCurrent(data.getNeutralCurrent());
		setPowerFactor(data.getPowerFactor());
		setApparentPower(data.getApparentPower());
		setReactivePower(data.getReactivePower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		setReverseWattHourReading(data.getActiveEnergyReceived());
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public PM5100DataAccessor getData() {
		return data;
	}

	@Override
	public long getDataTimestamp() {
		return data.getDataTimestamp();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return data.getDeviceInfo();
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		ACEnergyDataAccessor phaseData = data.accessorForPhase(phase);
		if ( backwards ) {
			phaseData = phaseData.reversed();
		}
		return phaseData;
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		return data.reversed();
	}

	@Override
	public Integer getActivePower() {
		return data.getActivePower();
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return data.getActiveEnergyDelivered();
	}

	@Override
	public Long getActiveEnergyReceived() {
		return data.getActiveEnergyReceived();
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return data.getApparentEnergyDelivered();
	}

	@Override
	public Long getApparentEnergyReceived() {
		return data.getApparentEnergyReceived();
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return data.getReactiveEnergyDelivered();
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return data.getReactiveEnergyReceived();
	}

}
