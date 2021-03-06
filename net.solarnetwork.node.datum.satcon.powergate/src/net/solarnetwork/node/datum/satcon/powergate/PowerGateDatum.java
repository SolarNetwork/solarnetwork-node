/* ==================================================================
 * PowerGateDatum.java - 11/11/2019 10:28:43 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.satcon.powergate;

import static net.solarnetwork.domain.Bitmaskable.bitmaskValue;
import java.util.Date;
import java.util.Set;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.hw.satcon.Fault;
import net.solarnetwork.node.hw.satcon.PowerGateInverterDataAccessor;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class PowerGateDatum extends GeneralNodePVEnergyDatum {

	private final PowerGateInverterDataAccessor sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public PowerGateDatum(PowerGateInverterDataAccessor sample) {
		super();
		this.sample = sample;
		if ( sample.getDataTimestamp() > 0 ) {
			setCreated(new Date(sample.getDataTimestamp()));
		}
		populateMeasurements(sample);
	}

	private void populateMeasurements(PowerGateInverterDataAccessor data) {
		for ( int i = 0; i <= 6; i++ ) {
			Set<? extends Fault> bitmask = data.getFaults(i);
			if ( bitmask != null && !bitmask.isEmpty() ) {
				putStatusSampleValue("fault" + i, bitmaskValue(bitmask));
			}
		}

		// verify in Running/Derate work mode, else invalid data might be collected
		DeviceOperatingState state = data.getDeviceOperatingState();
		if ( !(state == DeviceOperatingState.Normal || state == DeviceOperatingState.Override) ) {
			putStatusSampleValue(Datum.OP_STATE, state.getCode());
			return;
		}

		putInstantaneousSampleValue(ACEnergyDatum.FREQUENCY_KEY, data.getFrequency());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		putInstantaneousSampleValue(ACEnergyDatum.APPARENT_POWER_KEY, data.getApparentPower());
		setDCVoltage(data.getDCVoltage());
		setDCPower(data.getDCPower());

		putInstantaneousSampleValue("temp", data.getInverterTemperature());
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
	public PowerGateInverterDataAccessor getSample() {
		return sample;
	}

}
