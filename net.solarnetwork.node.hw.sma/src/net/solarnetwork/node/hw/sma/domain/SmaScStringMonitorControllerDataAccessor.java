/* ==================================================================
 * SmaScStringMonitorControllerDataAccessor.java - 15/09/2020 10:06:44 AM
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
import net.solarnetwork.node.domain.DataAccessor;

/**
 * {@link DataAccessor} for Sunny Central String Monitor Controller devices.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaScStringMonitorControllerDataAccessor extends SmaDeviceDataAccessor {

	@Override
	default boolean hasCommonDataAccessorSupport() {
		return false;
	}

	/**
	 * Get the latest event ID.
	 * 
	 * @return the event ID
	 */
	Long getEventId();

	/**
	 * Get the SMA operating state.
	 * 
	 * @return the operating state
	 */
	SmaCodedValue getOperatingState();

	/**
	 * Get the error value.
	 * 
	 * @return the error, or {@literal null}
	 */
	SmaCodedValue getError();

	/**
	 * Get the operating duration.
	 * 
	 * @return the operating time, in seconds
	 */
	BigInteger getOperatingTime();

	/**
	 * Get the PV string current group 1 average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentGroup1();

	/**
	 * Get the PV string current group 2 average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentGroup2();

	/**
	 * Get the PV string current group 3 average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentGroup3();

	/**
	 * Get the PV string current group 4 average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentGroup4();

	/**
	 * Get the PV string current group 5 average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentGroup5();

	/**
	 * Get the PV string current group 6 average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentGroup6();

	/**
	 * Get the SMU warning code for string error.
	 * 
	 * @return the warning code
	 */
	Long getSmuWarningCode();

}
