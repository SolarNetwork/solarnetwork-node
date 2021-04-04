/* ==================================================================
 * SmaSbN000UsDataAccessor.java - 17/09/2020 9:30:22 AM
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
 * {@link SmaDeviceDataAccessor} for Sunny Boy n000US series devices.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaSbN000UsDataAccessor extends SmaDeviceDataAccessor {

	@Override
	default boolean hasCommonDataAccessorSupport() {
		return false;
	}

	/**
	 * Get the device class.
	 * 
	 * @return the device class
	 */
	Long getDeviceClass();

	/**
	 * Get the error value.
	 * 
	 * @return the error, or {@literal null}
	 */
	SmaCodedValue getError();

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
	 * Get the grid mode.
	 * 
	 * @return the grid mode
	 */
	SmaCodedValue getBackupMode();

	/**
	 * Get the grid type.
	 * 
	 * @return the grid type
	 */
	SmaCodedValue getGridType();

	/**
	 * Get the operating mode of the power balancer.
	 * 
	 * @return the power balancer operating mode
	 */
	SmaCodedValue getPowerBalancerOperatingMode();

	/**
	 * Get the SMA operating state.
	 * 
	 * @return the operating state
	 */
	SmaCodedValue getOperatingState();

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
	 * Get the active (real) power, in W.
	 * 
	 * @return the active power
	 */
	Integer getActivePower();

	/**
	 * Get the AC grid voltage for line 1, in V.
	 * 
	 * @return the voltage
	 */
	Float getVoltageLine1Neutral();

	/**
	 * Get the AC grid voltage for line 1, in V.
	 * 
	 * @return the voltage
	 */
	Float getVoltageLine2Neutral();

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

}
