/* ==================================================================
 * EM5600ConsumptionDatum.java - Mar 26, 2014 10:17:43 AM
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

package net.solarnetwork.node.consumption.hc.em5600;

import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.hw.hc.EM5600Data;
import net.solarnetwork.node.hw.hc.MeasurementKind;

/**
 * Extension of {@link ConsumptionDatum} with additional properties supported by
 * the EM5600 series meters.
 * 
 * @author matt
 * @version 1.0
 */
public class EM5600ConsumptionDatum extends ConsumptionDatum {

	private final EM5600Data sample;
	private final MeasurementKind kind;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public EM5600ConsumptionDatum(EM5600Data sample, MeasurementKind kind) {
		super();
		this.sample = sample;
		this.kind = kind;
		extractMeasurements();
	}

	/**
	 * Test if the data appears valid in this datum.
	 * 
	 * @return <em>true</em> if the data appears to be valid
	 */
	public boolean isValid() {
		return (getWatts() != null || getWattHourReading() != null);
	}

	private void extractMeasurements() {
		switch (kind) {
			case Total:
				extractTotalMeasurements();
				break;

			case PhaseA:
				extractPhaseAMeasurements();
				break;

			case PhaseB:
				extractPhaseBMeasurements();
				break;

			case PhaseC:
				extractPhaseCMeasurements();
				break;
		}
	}

	private void extractTotalMeasurements() {
		setWatts(sample.getPower(EM5600Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		setWattHourReading(sample.getEnergy(EM5600Data.ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT));
	}

	private void extractPhaseAMeasurements() {
		setWatts(sample.getPower(EM5600Data.ADDR_DATA_ACTIVE_POWER_P1));
	}

	private void extractPhaseBMeasurements() {
		setWatts(sample.getPower(EM5600Data.ADDR_DATA_ACTIVE_POWER_P2));
	}

	private void extractPhaseCMeasurements() {
		setWatts(sample.getPower(EM5600Data.ADDR_DATA_ACTIVE_POWER_P3));
	}

}
