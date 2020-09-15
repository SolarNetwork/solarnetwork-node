/* ==================================================================
 * SmaDeviceCommonDataAccessor.java - 11/09/2020 4:27:29 PM
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

package net.solarnetwork.node.hw.sma.domain;

import java.math.BigInteger;

/**
 * API for the SMA "common" device model.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaDeviceCommonDataAccessor extends SmaDeviceDataAccessor {

	/**
	 * Get the latest event ID.
	 * 
	 * @return the event ID
	 */
	Long getEventId();

	/**
	 * Get the maximum possible continuous active power.
	 * 
	 * @return the power, in W
	 */
	Integer getActivePowerMaximum();

	/**
	 * Get the permanent active power limitation.
	 * 
	 * @return the active power limit, in W
	 */
	Integer getActivePowerPermanentLimit();

	/**
	 * Get the active energy exported, in Wh.
	 * 
	 * @return the exported active energy
	 */
	Long getActiveEnergyExported();

	/**
	 * Get the operating duration.
	 * 
	 * @return the operating time, in seconds
	 */
	BigInteger getOperatingTime();

	/**
	 * Get the feed-in duration.
	 * 
	 * @return the feed-in time, in seconds
	 */
	BigInteger getFeedInTime();

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
	 * Get the active (real) power, in W.
	 * 
	 * @return the active power
	 */
	Integer getActivePower();

	/**
	 * Get the AC line AB voltage.
	 * 
	 * @return the voltage
	 */
	Float getLineVoltageLine1Line2();

	/**
	 * Get the AC line BC voltage.
	 * 
	 * @return the voltage
	 */
	Float getLineVoltageLine2Line3();

	/**
	 * Get the AC line CA voltage.
	 * 
	 * @return the voltage
	 */
	Float getLineVoltageLine3Line1();

	/**
	 * Get the voltage across all lines, in V.
	 * 
	 * @return the voltage
	 */
	Float getVoltage();

	/**
	 * Get the current, in A.
	 * 
	 * @return the current
	 */
	Float getCurrent();

	/**
	 * Get the AC frequency value, in Hz.
	 * 
	 * @return the frequency
	 */
	Float getFrequency();

	/**
	 * Get the reactive power, in VAR.
	 * 
	 * @return the reactive power
	 */
	Integer getReactivePower();

	/**
	 * Get the apparent power, in VA.
	 * 
	 * @return the apparent power
	 */
	Integer getApparentPower();

	/**
	 * Get the active power target, in W.
	 * 
	 * @return the power target
	 */
	Integer getActivePowerTarget();

	/**
	 * Get the heat sink temperature, in degrees Celsius.
	 * 
	 * @return the heat sink temperature
	 */
	Float getHeatSinkTemperature();

	/**
	 * Get the cabinet temperature, in degrees Celsius.
	 * 
	 * @return the cabinet temperature
	 */
	Float getCabinetTemperature();

	/**
	 * Get the external (air supply) temperature, in degrees Celsius.
	 * 
	 * @return the temperature
	 */
	Float getExternalTemperature();

}
