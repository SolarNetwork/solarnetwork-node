/* ==================================================================
 * PM3200ConsumptionDatum.java - 1/03/2014 10:12:53 AM
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

package net.solarnetwork.node.consumption.schneider.pm3200;

import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;

/**
 * Extension of {@link ConsumptionDatum} with additional properties supported by
 * the PM3200 series meters.
 * 
 * @author matt
 * @version 1.1
 */
public class PM3200ConsumptionDatum extends ConsumptionDatum implements ACEnergyDatum {

	private final PM3200Data sample;
	private final ACPhase phase;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public PM3200ConsumptionDatum(PM3200Data sample, ACPhase phase) {
		super();
		this.sample = sample;
		this.phase = phase;
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
		switch (phase) {
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
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		setWattHourReading(sample.getEnergy(PM3200Data.ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT));
	}

	private void extractPhaseAMeasurements() {
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P1));
	}

	private void extractPhaseBMeasurements() {
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P2));
	}

	private void extractPhaseCMeasurements() {
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P3));
	}

	@Override
	public ACPhase getPhase() {
		return phase;
	}

	@Override
	public Integer getRealPower() {
		return getWatts();
	}

	@Override
	public Integer getApparentPower() {
		final int addr;
		switch (phase) {
			case PhaseA:
				addr = PM3200Data.ADDR_DATA_APPARENT_POWER_P1;
				break;

			case PhaseB:
				addr = PM3200Data.ADDR_DATA_APPARENT_POWER_P2;
				break;

			case PhaseC:
				addr = PM3200Data.ADDR_DATA_APPARENT_POWER_P3;
				break;

			default:
				addr = PM3200Data.ADDR_DATA_APPARENT_POWER_TOTAL;
				break;
		}
		return sample.getPower(addr);
	}

	@Override
	public Integer getReactivePower() {
		final int addr;
		switch (phase) {
			case PhaseA:
				addr = PM3200Data.ADDR_DATA_REACTIVE_POWER_P1;
				break;

			case PhaseB:
				addr = PM3200Data.ADDR_DATA_REACTIVE_POWER_P2;
				break;

			case PhaseC:
				addr = PM3200Data.ADDR_DATA_REACTIVE_POWER_P3;
				break;

			default:
				addr = PM3200Data.ADDR_DATA_REACTIVE_POWER_TOTAL;
				break;
		}
		return sample.getPower(addr);
	}

	@Override
	public Float getEffectivePowerFactor() {
		return sample.getEffectiveTotalPowerFactor();
	}

	@Override
	public Float getFrequency() {
		return sample.getFrequency(PM3200Data.ADDR_DATA_FREQUENCY);
	}

}
