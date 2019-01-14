/* ==================================================================
 * InverterModelAccessor.java - 5/10/2018 3:59:35 PM
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

import java.util.Set;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;

/**
 * API for accessing inverter model data.
 * 
 * @author matt
 * @version 1.0
 */
public interface InverterModelAccessor extends ModelAccessor {

	/**
	 * Get the current, in A.
	 * 
	 * @return the current
	 */
	Float getCurrent();

	/**
	 * Get the voltage, in V.
	 * 
	 * @return the voltage
	 */
	Float getVoltage();

	/**
	 * Get the active (real) power, in W.
	 * 
	 * @return the active power
	 */
	Integer getActivePower();

	/**
	 * Get the AC frequency value, in Hz.
	 * 
	 * @return the frequency
	 */
	Float getFrequency();

	/**
	 * Get the apparent power, in VA.
	 * 
	 * @return the apparent power
	 */
	Integer getApparentPower();

	/**
	 * Get the reactive power, in VAR.
	 * 
	 * @return the reactive power
	 */
	Integer getReactivePower();

	/**
	 * Get the power factor, as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	Float getPowerFactor();

	/**
	 * Get the active energy exported, in Wh.
	 * 
	 * @return the exported active energy
	 */
	Long getActiveEnergyExported();

	/**
	 * Get the DC current, in A.
	 * 
	 * @return the DC current
	 */
	Float getDcCurrent();

	/**
	 * Get the DC voltage, in V.
	 * 
	 * @return the DC voltage
	 */
	Float getDcVoltage();

	/**
	 * Get the DC power, in W.
	 * 
	 * @return the DC power
	 */
	Integer getDcPower();

	/**
	 * Get the cabinet temperature, in degrees Celsius.
	 * 
	 * @return the cabinet temperature
	 */
	Float getCabinetTemperature();

	/**
	 * Get the heat sink temperature, in degrees Celsius.
	 * 
	 * @return the heat sink temperature
	 */
	Float getHeatSinkTemperature();

	/**
	 * Get the transformer temperature, in degrees Celsius.
	 * 
	 * @return the transformer temperature
	 */
	Float getTransformerTemperature();

	/**
	 * Get the vendor-specific "other" temperature, in degrees Celsius.
	 * 
	 * @return the "other" temperature
	 */
	Float getOtherTemperature();

	/**
	 * Get the operating state.
	 * 
	 * @return the state
	 */
	OperatingState getOperatingState();

	/**
	 * Get the active events.
	 * 
	 * @return the events, never {@literal null}
	 */
	Set<ModelEvent> getEvents();

	/**
	 * Get an accessor for phase-specific measurements.
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	InverterModelAccessor accessorForPhase(ACPhase phase);
}
