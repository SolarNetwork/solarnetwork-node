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

import static net.solarnetwork.domain.Bitmaskable.bitmaskValue;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.util.Set;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLCTDataAccessor;
import net.solarnetwork.node.hw.csi.inverter.KTLCTInverterWorkMode;

/**
 * Extension of {@link SimpleAcDcEnergyDatum} with additional properties
 * supported by the KTL series inverters.
 * 
 * @author matt
 * @author maxieduncan
 * @version 2.0
 */
public class KTLDatum extends SimpleAcDcEnergyDatum {

	private static final long serialVersionUID = 2770342711914064679L;

	private final KTLCTDataAccessor sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param data
	 *        the sample
	 * @param sourceId
	 *        the source ID
	 */
	public KTLDatum(KTLCTDataAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.sample = data;
		populateMeasurements(data);
	}

	private void populateMeasurements(KTLCTDataAccessor data) {
		KTLCTInverterWorkMode workMode = data.getWorkMode();
		DeviceOperatingState state = workMode != null ? workMode.asDeviceOperatingState()
				: DeviceOperatingState.Unknown;
		MutableDatumSamplesOperations ops = asMutableSampleOperations();
		Set<? extends Bitmaskable> bitmask = data.getWarnings();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "warn", bitmaskValue(bitmask));
		}
		bitmask = data.getFaults0();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "fault0", bitmaskValue(bitmask));
		}

		bitmask = data.getFaults1();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "fault1", bitmaskValue(bitmask));
		}

		bitmask = data.getFaults2();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "fault2", bitmaskValue(bitmask));
		}

		bitmask = data.getPermanentFaults();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "permFault", bitmaskValue(bitmask));
		}

		// verify in Running/Derate work mode, else invalid data might be collected
		if ( !(state == DeviceOperatingState.Normal || state == DeviceOperatingState.Override) ) {
			ops.putSampleValue(Status, Datum.OP_STATE, state.getCode());
			return;
		}

		setFrequency(data.getFrequency());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		setApparentPower(data.getActivePower());

		ops.putSampleValue(Instantaneous, DcEnergyDatum.DC_VOLTAGE_KEY + "1", data.getPv1Voltage());
		ops.putSampleValue(Instantaneous, DcEnergyDatum.DC_POWER_KEY + "1",
				data.getPv1Voltage() * data.getPv1Current());
		ops.putSampleValue(Instantaneous, DcEnergyDatum.DC_VOLTAGE_KEY + "2", data.getPv2Voltage());
		ops.putSampleValue(Instantaneous, DcEnergyDatum.DC_POWER_KEY + "2",
				data.getPv2Voltage() * data.getPv2Current());
		ops.putSampleValue(Instantaneous, DcEnergyDatum.DC_VOLTAGE_KEY + "3", data.getPv3Voltage());
		ops.putSampleValue(Instantaneous, DcEnergyDatum.DC_POWER_KEY + "3",
				data.getPv3Voltage() * data.getPv3Current());

		ops.putSampleValue(Instantaneous, "temp", data.getModuleTemperature());
		ops.putSampleValue(Instantaneous, "ambientTemp", data.getInternalTemperature());
	}

	/**
	 * Test if the data appears valid in this datum.
	 * 
	 * @return {@literal true} if the data appears to be valid
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
