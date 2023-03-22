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

import static net.solarnetwork.domain.Bitmaskable.bitmaskValue;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Set;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLDataAccessor;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLInverterState;
import net.solarnetwork.util.NumberUtils;

/**
 * Extension of {@link SimpleAcDcEnergyDatum} for use with PVI-TL inverter
 * samples.
 * 
 * @author matt
 * @version 2.2
 */
public class PVITLDatum extends SimpleAcDcEnergyDatum {

	private static final long serialVersionUID = -6059283981724116296L;

	/** The sample. */
	private final PVITLDataAccessor sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 * @param sourceId
	 *        the source ID
	 */
	public PVITLDatum(PVITLDataAccessor sample, String sourceId) {
		super(sourceId, sample.getDataTimestamp(), new DatumSamples());
		this.sample = sample;
		populateMeasurements(sample);
	}

	private void populateMeasurements(PVITLDataAccessor data) {
		setDcCurrent(data.getDcCurrent());
		setDcVoltage(data.getDcVoltage());
		setDcPower(data.getDcPower());
		setVoltage(data.getVoltage());
		setWattHourReading(data.getActiveEnergyDelivered());
		setWatts(data.getActivePower());
		setApparentPower(data.getApparentPower());
		setPowerFactor(data.getPowerFactor());

		getSamples().putInstantaneousSampleValue("temp", data.getInternalTemperature());
		getSamples().putInstantaneousSampleValue("temp_heatSink", data.getModuleTemperature());

		getSamples().putInstantaneousSampleValue(AcEnergyDatum.FREQUENCY_KEY, data.getFrequency());
		getSamples().putInstantaneousSampleValue(AcEnergyDatum.CURRENT_KEY, data.getCurrent());

		getSamples().putInstantaneousSampleValue(DcEnergyDatum.DC_CURRENT_KEY + "1",
				data.getPv1Current());
		getSamples().putInstantaneousSampleValue(DcEnergyDatum.DC_VOLTAGE_KEY + "1",
				data.getPv1Voltage());
		getSamples().putInstantaneousSampleValue(DcEnergyDatum.DC_POWER_KEY + "1", data.getPv1Power());
		getSamples().putInstantaneousSampleValue(DcEnergyDatum.DC_CURRENT_KEY + "2",
				data.getPv2Current());
		getSamples().putInstantaneousSampleValue(DcEnergyDatum.DC_VOLTAGE_KEY + "2",
				data.getPv2Voltage());
		getSamples().putInstantaneousSampleValue(DcEnergyDatum.DC_POWER_KEY + "2", data.getPv2Power());

		final MutableDatumSamplesOperations ops = asMutableSampleOperations();

		DeviceOperatingState state = DeviceOperatingState.Unknown;
		PVITLInverterState invState = data.getOperatingState();
		if ( invState != null ) {
			state = invState.asDeviceOperatingState();
		}
		if ( state != DeviceOperatingState.Normal ) {
			ops.putSampleValue(Status, Datum.OP_STATE, state.getCode());
			ops.putSampleValue(Status, Datum.OP_STATES, invState.getCode());
		}

		Set<? extends Bitmaskable> bitmask = null;

		bitmask = data.getPermanentFaults();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "permFault", bitmaskValue(bitmask));
		}

		bitmask = data.getWarnings();
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

		bitmask = data.getFaults4();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "fault3", bitmaskValue(bitmask));
		}

		bitmask = data.getFaults3();
		if ( bitmask != null && !bitmask.isEmpty() ) {
			ops.putSampleValue(Status, "fault4", bitmaskValue(bitmask));
		}

		// SunSpec compatibility

		Set<ModelEvent> events = data.getEvents();
		if ( events != null && !events.isEmpty() ) {
			long b = ModelEvent.bitField32Value(data.getEvents());
			asMutableSampleOperations().putSampleValue(Status, "events", b);
		}

		BitSet vendorEvents = data.getVendorEvents();
		if ( events != null && !events.isEmpty() ) {
			BigInteger v = NumberUtils.bigIntegerForBitSet(vendorEvents);
			if ( v != null ) {
				asMutableSampleOperations().putSampleValue(Status, "vendorEvents",
						"0x" + v.toString(16));
			}
		}
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
	public PVITLDataAccessor getSample() {
		return sample;
	}

}
