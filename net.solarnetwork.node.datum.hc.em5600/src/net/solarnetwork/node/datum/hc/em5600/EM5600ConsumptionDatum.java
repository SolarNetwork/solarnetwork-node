/* ==================================================================
 * EM5600ConsumptionDatum.java - Mar 26, 2014 10:17:43 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.hc.em5600;

import java.util.Date;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.hc.EM5600Data;

/**
 * Extension of {@link GeneralNodeACEnergyDatum} with additional properties
 * supported by the EM5600 series meters.
 * 
 * @author matt
 * @version 1.2
 */
public class EM5600ConsumptionDatum extends GeneralNodeACEnergyDatum {

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public EM5600ConsumptionDatum(EM5600Data sample, ACPhase phase) {
		super();
		setPhase(phase);
		if ( sample.getDataTimestamp() > 0 ) {
			setCreated(new Date(sample.getDataTimestamp()));
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

}
