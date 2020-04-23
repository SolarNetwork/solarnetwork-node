/* ==================================================================
 * AE500NxDatum.java - 23/04/2020 2:36:35 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.ae.ae500nx;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Date;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.GroupedBitmaskable;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxDataAccessor;
import net.solarnetwork.util.NumberUtils;

/**
 * Datum for the AE 500NX inverter.
 * 
 * @author matt
 * @version 1.0
 */
public class AE500NxDatum extends GeneralNodePVEnergyDatum {

	private final AE500NxDataAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 */
	public AE500NxDatum(AE500NxDataAccessor data) {
		super();
		this.data = data;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		populateMeasurements(data);
	}

	private void populateMeasurements(AE500NxDataAccessor data) {
		putInstantaneousSampleValue(ACEnergyDatum.FREQUENCY_KEY, data.getFrequency());
		setVoltage(data.getVoltage());
		putInstantaneousSampleValue(ACEnergyDatum.CURRENT_KEY, data.getCurrent());
		putInstantaneousSampleValue(ACEnergyDatum.NEUTRAL_CURRENT_KEY, data.getNeutralCurrent());
		setDCVoltage(data.getDCVoltage());
		setDCPower(data.getDCPower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		putInstantaneousSampleValue(ACEnergyDatum.REACTIVE_POWER_KEY, data.getReactivePower());

		DeviceOperatingState opState = data.getDeviceOperatingState();
		if ( opState != null ) {
			putStatusSampleValue(Datum.OP_STATE, opState.getCode());
		}

		int status = Bitmaskable.bitmaskValue(data.getSystemStatus());
		if ( status > 0 ) {
			putStatusSampleValue(Datum.OP_STATES, status);
		}

		BitSet faults = GroupedBitmaskable.overallBitmaskValue(data.getFaults());
		if ( faults != null && faults.cardinality() > 0 ) {
			BigInteger v = NumberUtils.bigIntegerForBitSet(faults);
			if ( v != null ) {
				putStatusSampleValue("faults", "0x" + v.toString(16));
			}
		}

		BitSet warnings = GroupedBitmaskable.overallBitmaskValue(data.getWarnings());
		if ( warnings != null && warnings.cardinality() > 0 ) {
			BigInteger v = NumberUtils.bigIntegerForBitSet(warnings);
			if ( v != null ) {
				putStatusSampleValue("warnings", "0x" + v.toString(16));
			}
		}

		int limits = Bitmaskable.bitmaskValue(data.getSystemLimits());
		if ( limits > 0 ) {
			putStatusSampleValue("limits", limits);
		}
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public AE500NxDataAccessor getData() {
		return data;
	}

}
