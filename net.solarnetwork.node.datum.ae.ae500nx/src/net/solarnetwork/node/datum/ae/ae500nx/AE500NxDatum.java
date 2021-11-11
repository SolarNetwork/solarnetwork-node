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

import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.math.BigInteger;
import java.util.BitSet;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.GroupedBitmaskable;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxDataAccessor;
import net.solarnetwork.util.NumberUtils;

/**
 * Datum for the AE 500NX inverter.
 * 
 * @author matt
 * @version 2.0
 */
public class AE500NxDatum extends SimpleAcDcEnergyDatum {

	private static final long serialVersionUID = -3525924176266298024L;

	private final AE500NxDataAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public AE500NxDatum(AE500NxDataAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	private void populateMeasurements(AE500NxDataAccessor data) {
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setNeutralCurrent(data.getNeutralCurrent());
		setDcVoltage(data.getDcVoltage());
		setDcPower(data.getDcPower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		setReactivePower(data.getReactivePower());

		DeviceOperatingState opState = data.getDeviceOperatingState();
		if ( opState != null ) {
			asMutableSampleOperations().putSampleValue(Status, Datum.OP_STATE, opState.getCode());
		}

		int status = Bitmaskable.bitmaskValue(data.getSystemStatus());
		if ( status > 0 ) {
			asMutableSampleOperations().putSampleValue(Status, Datum.OP_STATES, status);
		}

		BitSet faults = GroupedBitmaskable.overallBitmaskValue(data.getFaults());
		if ( faults != null && faults.cardinality() > 0 ) {
			BigInteger v = NumberUtils.bigIntegerForBitSet(faults);
			if ( v != null ) {
				asMutableSampleOperations().putSampleValue(Status, "faults", "0x" + v.toString(16));
			}
		}

		BitSet warnings = GroupedBitmaskable.overallBitmaskValue(data.getWarnings());
		if ( warnings != null && warnings.cardinality() > 0 ) {
			BigInteger v = NumberUtils.bigIntegerForBitSet(warnings);
			if ( v != null ) {
				asMutableSampleOperations().putSampleValue(Status, "warnings", "0x" + v.toString(16));
			}
		}

		int limits = Bitmaskable.bitmaskValue(data.getSystemLimits());
		if ( limits > 0 ) {
			asMutableSampleOperations().putSampleValue(Status, "limits", limits);
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
