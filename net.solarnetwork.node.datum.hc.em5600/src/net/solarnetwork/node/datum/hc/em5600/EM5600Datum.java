/* ==================================================================
 * EM5600Datum.java - Mar 26, 2014 10:17:43 AM
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

package net.solarnetwork.node.datum.hc.em5600;

import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.hw.hc.EM5600DataAccessor;

/**
 * Extension of {@link SimpleAcEnergyDatum} with additional properties supported
 * by the EM5600 series meters.
 *
 * @author matt
 * @version 2.0
 * @since 2.0
 */
public class EM5600Datum extends SimpleAcEnergyDatum {

	private static final long serialVersionUID = 3929665682204910961L;

	/** The data. */
	private final EM5600DataAccessor data;

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
	 *        {@literal true} to reverse the direction of current
	 */
	public EM5600Datum(EM5600DataAccessor data, String sourceId, AcPhase phase, boolean backwards) {
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
	 * Get the source data.
	 *
	 * @return the data
	 */
	public EM5600DataAccessor getData() {
		return data;
	}

}
