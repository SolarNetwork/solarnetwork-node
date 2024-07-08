/* ==================================================================
 * PM3200Datum.java - 1/03/2014 10:12:53 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.schneider.pm3200;

import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200DataAccessor;

/**
 * Extension of {@link SimpleAcEnergyDatum} with additional properties supported
 * by the PM3200 series meters.
 *
 * @author matt
 * @version 4.0
 */
public class PM3200Datum extends SimpleAcEnergyDatum {

	private static final long serialVersionUID = -298143693772962051L;

	/** The data. */
	private final PM3200DataAccessor data;

	/**
	 * Construct with a sample.
	 *
	 * @param data
	 *        the accessor
	 * @param sourceId
	 *        the source ID
	 * @param phase
	 *        the phase
	 * @param backwards
	 *        {@literal true} to reverse current direction
	 */
	public PM3200Datum(PM3200DataAccessor data, String sourceId, AcPhase phase, boolean backwards) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		AcEnergyDataAccessor accessor = data.accessorForPhase(phase);
		if ( backwards ) {
			accessor = accessor.reversed();
		}
		populateMeasurements(accessor, phase);
	}

	private void populateMeasurements(AcEnergyDataAccessor data, AcPhase phase) {
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
	 * Test if the data appears valid in this datum.
	 *
	 * @return {@literal true} if the data appears to be valid
	 */
	public boolean isValid() {
		return (getWatts() != null || getWattHourReading() != null);
	}

	/**
	 * Get the data.
	 *
	 * @return the data
	 */
	public PM3200DataAccessor getData() {
		return data;
	}

}
