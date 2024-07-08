/* ==================================================================
 * WattsOnDatum.java - 14/08/2020 1:36:55 PM
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

package net.solarnetwork.node.datum.elkor.wattson;

import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.hw.elkor.upt.WattsOnDataAccessor;

/**
 * Datum implementation for Elkor WattsOn meter devices.
 *
 * @author matt
 * @version 2.0
 */
public class WattsOnDatum extends SimpleAcEnergyDatum {

	private static final long serialVersionUID = -9089223830028755982L;

	/** The data. */
	private final WattsOnDataAccessor data;

	/**
	 * Construct from a sample.
	 *
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 * @param phase
	 *        the phase to associate with the data
	 * @param backwards
	 *        if {@literal true} then treat the meter as being installed
	 *        backwards with respect to the current direction; in this case
	 *        certain instantaneous measurements will be negated and certain
	 *        accumulating properties will be switched (like <i>received</i>
	 *        energy will be captured as {@code wattHours} and <i>delivered</i>
	 *        energy as {@code wattHoursReverse})
	 */
	public WattsOnDatum(WattsOnDataAccessor data, String sourceId, AcPhase phase, boolean backwards) {
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
	 * Get the raw data used to populate this datum.
	 *
	 * @return the data
	 */
	public WattsOnDataAccessor getData() {
		return data;
	}

}
