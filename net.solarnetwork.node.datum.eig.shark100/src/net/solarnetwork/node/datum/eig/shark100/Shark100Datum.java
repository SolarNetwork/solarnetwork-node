/* ==================================================================
 * Shark100Datum.java - 27/07/2018 8:49:42 AM
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

package net.solarnetwork.node.datum.eig.shark100;

import java.util.Date;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.eig.meter.Shark100DataAccessor;

/**
 * Datum for the Shark 100 meter.
 * 
 * @author matt
 * @version 1.0
 */
public class Shark100Datum extends GeneralNodeACEnergyDatum {

	private final Shark100DataAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 */
	public Shark100Datum(Shark100DataAccessor data, ACPhase phase) {
		super();
		this.data = data;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		populateMeasurements(data, phase);
	}

	private void populateMeasurements(Shark100DataAccessor data, ACPhase phase) {
		setPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
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
	public Shark100DataAccessor getData() {
		return data;
	}
}
