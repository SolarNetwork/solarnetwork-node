/* ==================================================================
 * SDMDatum.java - 26/01/2016 3:07:11 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.deson.sdm;

import java.util.Date;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.deson.meter.SDMData;

/**
 * Extension of {@link GeneralNodeACEnergyDatum} with additional properties
 * supported by the SDM-XXX series meters.
 * 
 * @author matt
 * @version 1.0
 */
public class SDMDatum extends GeneralNodeACEnergyDatum {

	private final SDMData sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public SDMDatum(SDMData sample, ACPhase phase) {
		super();
		this.sample = sample;
		setPhase(phase);
		if ( sample.getMeterDataTimestamp() > 0 ) {
			setCreated(new Date(sample.getMeterDataTimestamp()));
		}
		sample.populateMeasurements(phase, this);
	}

	/**
	 * Test if the data appears valid in this datum.
	 * 
	 * @return <em>true</em> if the data appears to be valid
	 */
	public boolean isValid() {
		return (getWatts() != null || getWattHourReading() != null);
	}

	/**
	 * Get the raw sample data used by this datum.
	 * 
	 * @return the sample data
	 */
	public SDMData getSample() {
		return sample;
	}

}
