/* ==================================================================
 * Shark100DataAccessor.java - 26/07/2018 3:01:45 PM
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

import net.solarnetwork.node.domain.ACEnergyDataAccessor;

/**
 * API for reading Shark 100 meter data.
 * 
 * @author matt
 * @version 1.0
 */
public interface Shark100DataAccessor extends ACEnergyDataAccessor {

	/**
	 * Get the assigned meter name.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision.
	 */
	String getFirmwareRevision();

	/**
	 * Get the power system configuration.
	 * 
	 * @return the power system
	 */
	SharkPowerSystem getPowerSystem();

	/**
	 * Get the power energy format configuration.
	 * 
	 * @return the configuration
	 */
	SharkPowerEnergyFormat getPowerEnergyFormat();

}
