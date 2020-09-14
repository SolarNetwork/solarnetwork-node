/* ==================================================================
 * SmaScNnnUDataAccessor.java - 14/09/2020 2:31:25 PM
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
 * {@link DataAccessor} for Sunny Central nnnU inverters, for example the SC
 * 250-US.
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaScNnnUDataAccessor extends SmaDeviceCommonDataAccessor {

	@Override
	default boolean hasCommonDataAccessorSupport() {
		return true;
	}

	/**
	 * Get the time until grid connection attempt, in seconds.
	 * 
	 * @return the seconds until grid connection attempt
	 */
	Long getGridReconnectTime();

	/**
	 * Get the recommended action.
	 * 
	 * @return the recommended action, or {@literal null}
	 */
	SmaCommonStatusCode getRecommendedAction();

	/**
	 * Get the grid contactor status.
	 * 
	 * @return the grid contactor status
	 */
	SmaCommonStatusCode getGridContactorStatus();

	/**
	 * Get the SMA operating state.
	 * 
	 * @return the operating state
	 */
	SmaCommonStatusCode getOperatingState();

	/**
	 * Get the error value.
	 * 
	 * @return the error, or {@literal null}
	 */
	SmaCommonStatusCode getError();

}
