/* ==================================================================
 * PVITLDatum.java - 21/09/2018 2:21:03 PM
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

package net.solarnetwork.node.datum.yaskawa.pvitl;

import java.util.Date;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLDataAccessor;

/**
 * Extension of {@link GeneralNodePVEnergyDatum} for use with PVI-TL inverter
 * samples.
 * 
 * @author matt
 * @version 1.0
 */
public class PVITLDatum extends GeneralNodePVEnergyDatum {

	private final PVITLDataAccessor sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public PVITLDatum(PVITLDataAccessor sample) {
		super();
		this.sample = sample;
		if ( sample.getDataTimestamp() > 0 ) {
			setCreated(new Date(sample.getDataTimestamp()));
		}
		populateMeasurements(sample);
	}

	private void populateMeasurements(PVITLDataAccessor data) {
		setDCPower(data.getDCPower());
		setDCVoltage(data.getDCVoltage());
		setVoltage(data.getVoltage());
		setWattHourReading(data.getActiveEnergyDelivered());
		setWatts(data.getActivePower());

		putInstantaneousSampleValue(ACEnergyDatum.FREQUENCY_KEY, data.getFrequency());
		putInstantaneousSampleValue(ACEnergyDatum.CURRENT_KEY, data.getCurrent());

		putInstantaneousSampleValue(PVEnergyDatum.DC_VOLTAGE_KEY + "1", data.getPv1Voltage());
		putInstantaneousSampleValue(PVEnergyDatum.DC_POWER_KEY + "1", data.getPv1Power());
		putInstantaneousSampleValue(PVEnergyDatum.DC_VOLTAGE_KEY + "2", data.getPv2Voltage());
		putInstantaneousSampleValue(PVEnergyDatum.DC_POWER_KEY + "2", data.getPv2Power());
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
	public PVITLDataAccessor getSample() {
		return sample;
	}

}
