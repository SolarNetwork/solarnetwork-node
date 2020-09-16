/* ==================================================================
 * SmaScStringMonitorUsDataAccessor.java - 14/09/2020 8:55:54 AM
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

import net.solarnetwork.node.domain.DataAccessor;

/**
 * {@link DataAccessor} for Sunny Central String Monitor US devices.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaScStringMonitorUsDataAccessor extends SmaDeviceDataAccessor {

	@Override
	default boolean hasCommonDataAccessorSupport() {
		return false;
	}

	/**
	 * Get the SMA operating state.
	 * 
	 * @return the operating state
	 */
	SmaCommonStatusCode getOperatingState();

	/**
	 * Get the SMU ID.
	 * 
	 * @return the ID
	 */
	Long getStringMonitoringUnitId();

	/**
	 * Get the PV string 1 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString1();

	/**
	 * Get the PV string 2 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString2();

	/**
	 * Get the PV string 3 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString3();

	/**
	 * Get the PV string 4 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString4();

	/**
	 * Get the PV string 5 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString5();

	/**
	 * Get the PV string 6 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString6();

	/**
	 * Get the PV string 7 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString7();

	/**
	 * Get the PV string 8 current group average (MeanCurGr1), in A.
	 * 
	 * @return the current
	 */
	Float getCurrentString8();

}
