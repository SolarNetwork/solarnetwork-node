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

import java.util.Date;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;

/**
 * Extension of {@link GeneralNodeACEnergyDatum} with additional properties
 * supported by the PM3200 series meters.
 * 
 * @author matt
 * @version 2.0
 */
public class PM3200ConsumptionDatum extends GeneralNodeACEnergyDatum {

	private final PM3200Data sample;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 */
	public PM3200ConsumptionDatum(PM3200Data sample, ACPhase phase) {
		super();
		this.sample = sample;
		setPhase(phase);
		if ( sample.getDataTimestamp() > 0 ) {
			setCreated(new Date(sample.getDataTimestamp()));
		}
		extractMeasurements(phase);
	}

	/**
	 * Test if the data appears valid in this datum.
	 * 
	 * @return <em>true</em> if the data appears to be valid
	 */
	public boolean isValid() {
		return (getWatts() != null || getWattHourReading() != null);
	}

	private void extractMeasurements(ACPhase phase) {
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
		setEffectivePowerFactor(sample.getEffectiveTotalPowerFactor());
		setFrequency(sample.getFrequency(PM3200Data.ADDR_DATA_FREQUENCY));
		setWattHourReading(sample.getEnergy(PM3200Data.ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT));

		setApparentPower(sample.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_TOTAL));
		setCurrent(sample.getCurrent(PM3200Data.ADDR_DATA_I_AVERAGE));
		setPhaseVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L_L_AVERAGE));
		setReactivePower(sample.getPower(PM3200Data.ADDR_DATA_REACTIVE_POWER_TOTAL));
		setRealPower(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		setPowerFactor(sample.getPowerFactor(PM3200Data.ADDR_DATA_POWER_FACTOR_TOTAL));
		setVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_NEUTRAL_AVERAGE));
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
	}

	private void extractPhaseAMeasurements() {
		setApparentPower(sample.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_P1));
		setCurrent(sample.getCurrent(PM3200Data.ADDR_DATA_I1));
		setPhaseVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L1_L2));
		setReactivePower(sample.getPower(PM3200Data.ADDR_DATA_REACTIVE_POWER_P1));
		setRealPower(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P1));
		setPowerFactor(sample.getPowerFactor(PM3200Data.ADDR_DATA_POWER_FACTOR_P1));
		setVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L1_NEUTRAL));
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P1));
	}

	private void extractPhaseBMeasurements() {
		setApparentPower(sample.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_P2));
		setCurrent(sample.getCurrent(PM3200Data.ADDR_DATA_I2));
		setPhaseVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L2_L3));
		setReactivePower(sample.getPower(PM3200Data.ADDR_DATA_REACTIVE_POWER_P2));
		setRealPower(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P2));
		setPowerFactor(sample.getPowerFactor(PM3200Data.ADDR_DATA_POWER_FACTOR_P2));
		setVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L2_NEUTRAL));
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P2));
	}

	private void extractPhaseCMeasurements() {
		setApparentPower(sample.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_P3));
		setCurrent(sample.getCurrent(PM3200Data.ADDR_DATA_I3));
		setPhaseVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L3_L1));
		setReactivePower(sample.getPower(PM3200Data.ADDR_DATA_REACTIVE_POWER_P3));
		setRealPower(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P3));
		setPowerFactor(sample.getPowerFactor(PM3200Data.ADDR_DATA_POWER_FACTOR_P3));
		setVoltage(sample.getVoltage(PM3200Data.ADDR_DATA_V_L3_NEUTRAL));
		setWatts(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_P3));
	}

}
