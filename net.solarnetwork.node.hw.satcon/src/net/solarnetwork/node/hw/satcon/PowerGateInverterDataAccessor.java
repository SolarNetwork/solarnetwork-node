/* ==================================================================
 * PowerGateInverterDataAccessor.java - 8/11/2019 11:14:36 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.satcon;

import java.util.Set;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.PVEnergyDataAccessor;

/**
 * API for reading Power Gate inverter series data.
 * 
 * @author matt
 * @version 1.0
 */
public interface PowerGateInverterDataAccessor extends PVEnergyDataAccessor, ACEnergyDataAccessor {

	/**
	 * Get the operating state.
	 * 
	 * @return the state
	 */
	PowerGateOperatingState getOperatingState();

	/**
	 * Get the device operating state.
	 * 
	 * @return the state
	 */
	DeviceOperatingState getDeviceOperatingState();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the device firmware version.
	 * 
	 * @return the firmware version
	 */
	String getFirmwareVersion();

	/**
	 * Get all active faults across all groups.
	 * 
	 * @return the faults
	 */
	Set<? extends Fault> getFaults();

	/**
	 * Get all active faults for a given group.
	 * 
	 * @param group
	 *        the fault group
	 * @return the faults
	 */
	Set<? extends Fault> getFaults(int group);

	/**
	 * Get the internal (ambient) temperature, in degrees Celsius.
	 * 
	 * @return the internal temperature
	 */
	Float getInternalTemperature();

	/**
	 * Get the transformer temperature, in degrees Celsius.
	 * 
	 * @return the transformer temperature
	 */
	Float getInverterTemperature();

	/**
	 * Get the number of heat sink temperature modules avaialable.
	 * 
	 * @return the number of heatsink tempeature modules
	 */
	int getHeatsinkTemperatureCount();

	/**
	 * Get the heat sink temperature, in degrees Celsius.
	 * 
	 * 
	 * @param index
	 *        the heatsink module to get the temperature for, starting at
	 *        {@literal 1} and up to {@link #getHeatsinkTemperatureCount()}
	 * @return the heatshink temperature, in degrees Celsius
	 * @throws IllegalArgumentException
	 *         if {@code index} is out of range
	 */
	Float getHeatsinkTemperature(int index);

	/**
	 * Get the active energy delivered today, in Wh.
	 * 
	 * @return the delivered active energy today only
	 */
	Long getActiveEnergyDeliveredToday();

}
