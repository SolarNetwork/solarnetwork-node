/* ==================================================================
 * AE500NxDataAccessor.java - 22/04/2020 11:37:34 am
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DcEnergyDataAccessor;

/**
 * API for reading AE 500NX data.
 * 
 * @author matt
 * @version 1.1
 * @since 2.1
 */
public interface AE500NxDataAccessor extends DcEnergyDataAccessor, AcEnergyDataAccessor {

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the firmware version.
	 * 
	 * @return the version
	 */
	String getFirmwareVersion();

	/**
	 * Get the ambient temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getAmbientTemperature();

	/**
	 * Get the cabinet temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getCabinetTemperature();

	/**
	 * Get the coolant temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getCoolantTemperature();

	/**
	 * Get the line reactor temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getReactorTemperature();

	/**
	 * Get the DC current.
	 * 
	 * @return the DC current, in amperes
	 */
	Float getDcCurrent();

	/**
	 * Get the system status.
	 * 
	 * @return the status
	 */
	Set<AE500NxSystemStatus> getSystemStatus();

	/**
	 * Get the system limits.
	 * 
	 * @return the limits
	 */
	Set<AE500NxSystemLimit> getSystemLimits();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 */
	SortedSet<AE500NxFault> getFaults();

	/**
	 * Get the complete set of active warnings, sorted by warning number.
	 * 
	 * @return the active warnings
	 */
	SortedSet<AE500NxWarning> getWarnings();

	/**
	 * Get the device operating state.
	 * 
	 * @return the device operating state
	 */
	DeviceOperatingState getDeviceOperatingState();

}
