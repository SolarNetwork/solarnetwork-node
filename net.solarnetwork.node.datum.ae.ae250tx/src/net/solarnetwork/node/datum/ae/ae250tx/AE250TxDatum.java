/* ==================================================================
 * AE250TxDatum.java - 30/07/2018 7:28:35 AM
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

package net.solarnetwork.node.datum.ae.ae250tx;

import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.Date;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxDataAccessor;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxFault;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxSystemStatus;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxWarning;

/**
 * Datum for the AE 250TX inverter.
 * 
 * @author matt
 * @version 1.1
 */
public class AE250TxDatum extends GeneralNodePVEnergyDatum {

	private final AE250TxDataAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 */
	public AE250TxDatum(AE250TxDataAccessor data) {
		super();
		this.data = data;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		populateMeasurements(data);
	}

	private void populateMeasurements(AE250TxDataAccessor data) {
		putInstantaneousSampleValue(ACEnergyDatum.FREQUENCY_KEY, data.getFrequency());
		setVoltage(data.getVoltage());
		putInstantaneousSampleValue(ACEnergyDatum.CURRENT_KEY, data.getCurrent());
		setDCVoltage(data.getDCVoltage());
		setDCPower(data.getDCPower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());

		DeviceOperatingState opState = data.getDeviceOperatingState();
		if ( opState != null ) {
			putStatusSampleValue(Datum.OP_STATE, opState.getCode());
		}

		AE250TxSystemStatus status = data.getSystemStatus();
		if ( status != null ) {
			putStatusSampleValue(Datum.OP_STATES, status.getCode());
		}

		SortedSet<AE250TxFault> faults = data.getFaults();
		if ( faults != null && !faults.isEmpty() ) {
			putStatusSampleValue("faults", commaDelimitedStringFromCollection(faults));
		}

		SortedSet<AE250TxWarning> warnings = data.getWarnings();
		if ( warnings != null && !warnings.isEmpty() ) {
			putStatusSampleValue("warnings", commaDelimitedStringFromCollection(warnings));
		}
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public AE250TxDataAccessor getData() {
		return data;
	}

}
