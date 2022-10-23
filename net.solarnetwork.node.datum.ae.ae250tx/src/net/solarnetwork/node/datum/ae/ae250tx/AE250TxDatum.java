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

import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxDataAccessor;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxFault;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxSystemStatus;
import net.solarnetwork.node.hw.ae.inverter.tx.AE250TxWarning;

/**
 * Datum for the AE 250TX inverter.
 * 
 * @author matt
 * @version 2.1
 */
public class AE250TxDatum extends SimpleAcDcEnergyDatum {

	private static final long serialVersionUID = -6794780558933937702L;

	/** Sample data. */
	private final AE250TxDataAccessor data;

	/**
	 * Construct from a sample.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public AE250TxDatum(AE250TxDataAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	private void populateMeasurements(AE250TxDataAccessor data) {
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setCurrent(data.getCurrent());
		setPvVoltage(data.getPvVoltage());
		setDcCurrent(data.getDcCurrent());
		setDcVoltage(data.getDcVoltage());
		setDcPower(data.getDcPower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());

		DeviceOperatingState opState = data.getDeviceOperatingState();
		if ( opState != null ) {
			asMutableSampleOperations().putSampleValue(Status, Datum.OP_STATE, opState.getCode());
		}

		AE250TxSystemStatus status = data.getSystemStatus();
		if ( status != null ) {
			asMutableSampleOperations().putSampleValue(Status, Datum.OP_STATES, status.getCode());
		}

		SortedSet<AE250TxFault> faults = data.getFaults();
		if ( faults != null && !faults.isEmpty() ) {
			asMutableSampleOperations().putSampleValue(Status, "faults",
					commaDelimitedStringFromCollection(faults));
		}

		SortedSet<AE250TxWarning> warnings = data.getWarnings();
		if ( warnings != null && !warnings.isEmpty() ) {
			asMutableSampleOperations().putSampleValue(Status, "warnings",
					commaDelimitedStringFromCollection(warnings));
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

	/**
	 * Get the PV voltage.
	 * 
	 * @return the voltage
	 * @since 2.1
	 */
	public Float getPvVoltage() {
		return asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "pvVoltage");
	}

	/**
	 * Set the PV voltage.
	 * 
	 * @param pvVoltage
	 *        the voltage
	 * @since 2.1
	 */
	public void setPvVoltage(Float pvVoltage) {
		asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous, "pvVoltage",
				pvVoltage);
	}

}
