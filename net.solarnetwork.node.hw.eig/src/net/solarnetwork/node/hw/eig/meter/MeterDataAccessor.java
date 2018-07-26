/* ==================================================================
 * MeterDataAccessor.java - 17/05/2018 7:16:42 PM
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

package net.solarnetwork.node.hw.eig.meter;

/**
 * Common API for accessing meter data.
 * 
 * @author matt
 * @version 1.0
 */
public interface MeterDataAccessor {

	/**
	 * Gets the time stamp of the data.
	 * 
	 * @return the data time stamp
	 */
	long getDataTimestamp();

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
	 * Get the active energy imported (delivered), in Wh.
	 * 
	 * @return the imported active energy
	 */
	Long getActiveEnergyDelivered();

	/**
	 * Get the active energy exported (received), in Wh.
	 * 
	 * @return the exported active energy
	 */
	Long getActiveEnergyReceived();

	/**
	 * Get the reactive energy imported (delivered), in VARh.
	 * 
	 * @return the imported reactive energy
	 */
	Long getReactiveEnergyDelivered();

	/**
	 * Get the reactive energy exported (received), in VARh.
	 * 
	 * @return the exported reactive energy
	 */
	Long getReactiveEnergyReceived();

}
