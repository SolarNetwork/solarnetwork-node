/* ==================================================================
 * ACEnergyDataAccessor.java - 30/07/2018 9:17:39 AM
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

package net.solarnetwork.node.domain;

/**
 * API for accessing AC energy data from an object, such as a power meter.
 * 
 * @author matt
 * @version 1.0
 * @since 1.60
 */
public interface ACEnergyDataAccessor extends DataAccessor {

	/**
	 * Get an accessor for phase-specific measurements.
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	ACEnergyDataAccessor accessorForPhase(ACPhase phase);

	/**
	 * Get an accessor that reverses the current direction of the data, turning
	 * received into delivered and vice versa.
	 * 
	 * @return the accessor
	 */
	ACEnergyDataAccessor reversed();

	/**
	 * Get the AC frequency value, in Hz.
	 * 
	 * @return the frequency
	 */
	Float getFrequency();

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
	 * Get the power factor, as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	Float getPowerFactor();

	/**
	 * Get the active (real) power, in W.
	 * 
	 * @return the active power
	 */
	Integer getActivePower();

	/**
	 * /** Get the active energy delivered (imported), in Wh.
	 * 
	 * @return the delivered active energy
	 */
	Long getActiveEnergyDelivered();

	/**
	 * Get the active energy received (exported), in Wh.
	 * 
	 * @return the received active energy
	 */
	Long getActiveEnergyReceived();

	/**
	 * Get the apparent power, in VA.
	 * 
	 * @return the apparent power
	 */
	Integer getApparentPower();

	/**
	 * Get the apparent energy delivered (imported), in VAh.
	 * 
	 * @return the delivered apparent energy
	 */
	Long getApparentEnergyDelivered();

	/**
	 * Get the apparent energy received (exported), in VAh.
	 * 
	 * @return the received apparent energy
	 */
	Long getApparentEnergyReceived();

	/**
	 * Get the reactive power, in VAR.
	 * 
	 * @return the reactive power
	 */
	Integer getReactivePower();

	/**
	 * Get the reactive energy delivered (imported), in VARh.
	 * 
	 * @return the delivered reactive energy
	 */
	Long getReactiveEnergyDelivered();

	/**
	 * Get the reactive energy received (exported), in VARh.
	 * 
	 * @return the received reactive energy
	 */
	Long getReactiveEnergyReceived();
}
