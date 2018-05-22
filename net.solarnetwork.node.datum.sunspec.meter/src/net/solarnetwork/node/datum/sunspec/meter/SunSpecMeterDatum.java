/* ==================================================================
 * SunSpecMeterDatum.java - 23/05/2018 6:46:30 AM
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

package net.solarnetwork.node.datum.sunspec.meter;

import java.util.Date;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;

/**
 * Datum for a SunSpec compatible meter.
 * 
 * @author matt
 * @version 1.0
 */
public class SunSpecMeterDatum extends GeneralNodeACEnergyDatum {

	private final MeterModelAccessor data;
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
	public SunSpecMeterDatum(MeterModelAccessor data, ACPhase phase, boolean backwards) {
		super();
		this.data = data;
		this.backwards = backwards;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		populateMeasurements(data, phase);
	}

	private void populateMeasurements(MeterModelAccessor data, ACPhase phase) {
		setPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setPowerFactor(data.getPowerFactor());
		setApparentPower((backwards ? -1 : 1) * data.getApparentPower());
		setReactivePower((backwards ? -1 : 1) * data.getReactivePower());
		setWatts((backwards ? -1 : 1) * data.getActivePower());
		if ( backwards ) {
			setWattHourReading(data.getActiveEnergyExported());
			setReverseWattHourReading(data.getActiveEnergyImported());
		} else {
			setWattHourReading(data.getActiveEnergyImported());
			setReverseWattHourReading(data.getActiveEnergyExported());
		}
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public MeterModelAccessor getData() {
		return data;
	}

}
