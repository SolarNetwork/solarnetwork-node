/* ==================================================================
 * KTLDatum.java - 23/11/2017 3:07:11 pm
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

package net.solarnetwork.node.datum.csi.ktl;

import java.util.Date;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLCTDataAccessor;

/**
 * Extension of {@link GeneralNodePVEnergyDatum} with additional properties
 * supported by the KTL series inverters.
 * 
 * @author matt
 * @author maxieduncan
 * @version 1.0
 */
public class KTLDatum extends GeneralNodePVEnergyDatum {

	private final KTLCTDataAccessor sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public KTLDatum(KTLCTDataAccessor sample) {
		super();
		this.sample = sample;
		if ( sample.getDataTimestamp() > 0 ) {
			setCreated(new Date(sample.getDataTimestamp()));
		}
		populateMeasurements(sample);
	}

	private void populateMeasurements(KTLCTDataAccessor data) {
		putInstantaneousSampleValue(ACEnergyDatum.FREQUENCY_KEY, data.getFrequency());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		putInstantaneousSampleValue(ACEnergyDatum.APPARENT_POWER_KEY, data.getApparentPower());

		putInstantaneousSampleValue(PVEnergyDatum.DC_VOLTAGE_KEY + "1", data.getPv1Voltage());
		putInstantaneousSampleValue(PVEnergyDatum.DC_POWER_KEY + "1",
				data.getPv1Voltage() * data.getPv1Current());
		putInstantaneousSampleValue(PVEnergyDatum.DC_VOLTAGE_KEY + "2", data.getPv2Voltage());
		putInstantaneousSampleValue(PVEnergyDatum.DC_POWER_KEY + "2",
				data.getPv2Voltage() * data.getPv2Current());
		putInstantaneousSampleValue(PVEnergyDatum.DC_VOLTAGE_KEY + "3", data.getPv3Voltage());
		putInstantaneousSampleValue(PVEnergyDatum.DC_POWER_KEY + "3",
				data.getPv3Voltage() * data.getPv3Current());

		putInstantaneousSampleValue("temp", data.getModuleTemperature());
		putInstantaneousSampleValue("ambientTemp", data.getInternalTemperature());
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
	public KTLCTDataAccessor getSample() {
		return sample;
	}

}
