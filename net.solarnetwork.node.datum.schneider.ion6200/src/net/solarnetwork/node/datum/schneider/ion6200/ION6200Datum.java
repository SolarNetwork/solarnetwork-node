/* ==================================================================
 * ION6200Datum.java - 15/05/2018 7:39:50 AM
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

package net.solarnetwork.node.datum.schneider.ion6200;

import java.util.Date;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.ION6200DataAccessor;

/**
 * Datum for the ION6200 meter.
 * 
 * @author matt
 * @version 1.0
 */
public class ION6200Datum extends GeneralNodeACEnergyDatum {

	private final ION6200DataAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 */
	public ION6200Datum(ION6200DataAccessor data, ACPhase phase) {
		super();
		this.data = data;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		populateMeasurements(data, phase);
	}

	private void populateMeasurements(ION6200DataAccessor data, ACPhase phase) {
		setPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setPowerFactor(data.getPowerFactor());
		setWatts(data.getActivePower());
		setApparentPower(data.getApparentPower());
		setReactivePower(data.getReactivePower());
		setWattHourReading(data.getActiveEnergyReceived());
		setReverseWattHourReading(data.getActiveEnergyDelivered());
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public ION6200DataAccessor getData() {
		return data;
	}

}
