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
 * @since 2.4
 */
public interface ION6200DataAccessor extends MeterDataAccessor {

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

}
