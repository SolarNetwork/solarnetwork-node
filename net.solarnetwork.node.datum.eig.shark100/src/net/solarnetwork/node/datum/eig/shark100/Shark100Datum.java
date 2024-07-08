/* ==================================================================
 * Shark100Datum.java - 27/07/2018 8:49:42 AM
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

package net.solarnetwork.node.datum.eig.shark100;

import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.hw.eig.meter.Shark100DataAccessor;

/**
 * Datum for the Shark 100 meter.
 *
 * @author matt
 * @version 2.0
 */
public class Shark100Datum extends SimpleAcEnergyDatum {

	private static final long serialVersionUID = -546770850559144044L;

	/** The data. */
	private final Shark100DataAccessor data;

	/**
	 * Construct from a sample.
	 *
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 * @param phase
	 *        the phase
	 * @param backwards
	 *        {@literal true} to reverse the current direction
	 */
	public Shark100Datum(Shark100DataAccessor data, String sourceId, AcPhase phase, boolean backwards) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		AcEnergyDataAccessor accessor = data.accessorForPhase(phase);
		if ( backwards ) {
			accessor = accessor.reversed();
		}
		populateMeasurements(accessor, phase);
	}

	private void populateMeasurements(AcEnergyDataAccessor data, AcPhase phase) {
		assert phase == AcPhase.Total;
		setAcPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setLineVoltage(data.getLineVoltage());
		setCurrent(data.getCurrent());
		setNeutralCurrent(data.getNeutralCurrent());
		setPowerFactor(data.getPowerFactor());
		setApparentPower(data.getApparentPower());
		setReactivePower(data.getReactivePower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		setReverseWattHourReading(data.getActiveEnergyReceived());
	}

	/**
	 * Get the raw data used to populate this datum.
	 *
	 * @return the data
	 */
	public Shark100DataAccessor getData() {
		return data;
	}

}
