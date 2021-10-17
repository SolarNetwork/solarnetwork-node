/* ==================================================================
 * InverterBasicSettingsModelAccessor.java - 15/10/2018 1:44:08 PM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.ApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ReactivePowerAction;

/**
 * API for accessing inverter basic settings model data.
 * 
 * @author matt
 * @version 2.0
 * @since 1.2
 */
public interface InverterBasicSettingsModelAccessor extends ModelAccessor {

	/**
	 * Get the maximum active power output, in W.
	 * 
	 * @return the active power maximum
	 */
	Integer getActivePowerMaximum();

	/**
	 * Get the voltage at the point of common coupling (PCC), in V.
	 * 
	 * @return the PCC voltage
	 */
	Float getPccVoltage();

	/**
	 * Get the voltage offset from the PCC to the inverter, in V.
	 * 
	 * @return the voltage offset
	 */
	Float getPccVoltageOffset();

	/**
	 * Get the maximum voltage, in V.
	 * 
	 * @return the maximum voltage
	 */
	Float getVoltageMaximum();

	/**
	 * Get the minimum voltage, in V.
	 * 
	 * @return the minimum voltage
	 */
	Float getVoltageMinimum();

	/**
	 * Get the maximum apparent power output, in VA.
	 * 
	 * @return the apparent power maximum
	 */
	Integer getApparentPowerMaximum();

	/**
	 * Get the maximum reactive power for EEI quadrant 1 (lagging, inductive),
	 * in VAR.
	 * 
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ1Maximum();

	/**
	 * Get the maximum reactive power for EEI quadrant 2 (leading, capacitive),
	 * in VAR.
	 * 
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ2Maximum();

	/**
	 * Get the maximum reactive power for EEI quadrant 3 (lagging, inductive),
	 * in VAR.
	 * 
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ3Maximum();

	/**
	 * Get the maximum reactive power for EEI quadrant 4 (leading, capacitive),
	 * in VAR.
	 * 
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ4Maximum();

	/**
	 * Get the ramp rate of change of active power due to commands or internal
	 * actions, in maximum active power percentage/sec.
	 * 
	 * @return active power ramp rate percentage
	 */
	Float getActivePowerRampRate();

	/**
	 * Get the minimum power factor rating for EEI quadrant 1 (lagging,
	 * inductive), as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	Float getPowerFactorQ1Minimum();

	/**
	 * Get the minimum power factor rating for EEI quadrant 2 (leading,
	 * capacitive), as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	Float getPowerFactorQ2Minimum();

	/**
	 * Get the minimum power factor rating for EEI quadrant 3 (lagging,
	 * inductive), as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	Float getPowerFactorQ3Minimum();

	/**
	 * Get the minimum power factor rating for EEI quadrant 4 (leading,
	 * capacitive), as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	Float getPowerFactorQ4Minimum();

	/**
	 * Get the action to take when changing between charging and discharging.
	 * 
	 * @return the action
	 */
	ReactivePowerAction getImportExportChangeReactivePowerAction();

	/**
	 * Get the apparent power calculation method used.
	 * 
	 * @return the apparent power calculation method
	 */
	ApparentPowerCalculationMethod getApparentPowerCalculationMethod();

	/**
	 * Get the ramp rate of change of active power due to intermittent PV
	 * generation, as a percentage of {@link #getActivePowerRampRate()}.
	 * 
	 * @return active power ramp rate percentage
	 */
	Float getActivePowerRampRateMaximum();

	/**
	 * Get the nominal frequency at the electrical connection point (ECP), in
	 * Hz.
	 * 
	 * @return the ECP frequency
	 */
	Float getEcpFrequency();

	/**
	 * Get the connected phase, for single phase inverters.
	 * 
	 * @return the connected phase
	 */
	AcPhase getConnectedPhase();

}
