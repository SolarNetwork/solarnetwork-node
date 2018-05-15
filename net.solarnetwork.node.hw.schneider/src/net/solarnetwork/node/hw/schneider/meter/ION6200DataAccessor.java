/* ==================================================================
 * ION6200DataAccessor.java - 15/05/2018 7:28:15 AM
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

package net.solarnetwork.node.hw.schneider.meter;

/**
 * API for accessing ION6200 data elements.
 * 
 * @author matt
 * @version 1.0
 */
public interface ION6200DataAccessor {

	/**
	 * Gets the time stamp of the data.
	 * 
	 * @return the data time stamp
	 */
	long getDataTimestamp();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	Long getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision
	 */
	Integer getFirmwareRevision();

	/**
	 * Get the device type, e.g. model number like {@literal 6200}.
	 * 
	 * @return the device type
	 */
	Integer getDeviceType();

	/**
	 * Get the volts mode.
	 * 
	 * @return the mode
	 */
	ION6200VoltsMode getVoltsMode();

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
	 * Get the active power, in W.
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
	 * Get the active energy delivered, in Wh.
	 * 
	 * @return the delivered active energy
	 */
	Long getActiveEnergyDelivered();

	/**
	 * Get the active energy received, in Wh.
	 * 
	 * @return the received active energy
	 */
	Long getActiveEnergyReceived();

	/**
	 * Get the reactive energy delivered, in VARh.
	 * 
	 * @return the delivered reactive energy
	 */
	Long getReactiveEnergyDelivered();

	/**
	 * Get the reactive energy received, in VARh.
	 * 
	 * @return the received reactive energy
	 */
	Long getReactiveEnergyReceived();

}
