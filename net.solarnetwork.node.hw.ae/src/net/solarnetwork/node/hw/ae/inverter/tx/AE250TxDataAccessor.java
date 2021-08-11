/* ==================================================================
 * AE250TxDataAccessor.java - 27/07/2018 2:12:14 PM
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

package net.solarnetwork.node.hw.ae.inverter.tx;

import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.PVEnergyDataAccessor;

/**
 * API for reading AE 250TX data.
 * 
 * @author matt
 * @version 1.1
 */
public interface AE250TxDataAccessor extends PVEnergyDataAccessor, ACEnergyDataAccessor {

	/**
	 * Get the inverter type.
	 * 
	 * @return the name
	 */
	AEInverterType getInverterType();

	/**
	 * Get the device ID number.
	 * 
	 * @return the ID number
	 */
	String getIdNumber();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision
	 */
	String getFirmwareRevision();

	/**
	 * Get the device register mapping version.
	 * 
	 * @return the map version
	 */
	Integer getMapVersion();

	/**
	 * Get the inverter configuration.
	 * 
	 * @return the configuration
	 */
	AEInverterConfiguration getInverterConfiguration();

	/**
	 * Get the inverter's rated power, in W.
	 * 
	 * @return the rated power
	 */
	Integer getInverterRatedPower();

	/**
	 * Get the system status.
	 * 
	 * @return the status
	 * @since 1.1
	 */
	Set<AE250TxSystemStatus> getSystemStatus();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 */
	SortedSet<AE250TxFault> getFaults();

	/**
	 * Get the complete set of active warnings, sorted by warning number.
	 * 
	 * @return the active warnings
	 */
	SortedSet<AE250TxWarning> getWarnings();

	/**
	 * Get the device operating state.
	 * 
	 * @return the device operating state
	 */
	DeviceOperatingState getDeviceOperatingState();

}
