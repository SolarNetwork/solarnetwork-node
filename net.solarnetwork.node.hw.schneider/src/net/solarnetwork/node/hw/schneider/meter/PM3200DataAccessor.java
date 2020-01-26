/* ==================================================================
 * PM3200DataAccessor.java - 20/01/2020 5:40:58 pm
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

package net.solarnetwork.node.hw.schneider.meter;

import org.joda.time.LocalDateTime;

/**
 * API for reading PM3200 series meter data.
 * 
 * @author matt
 * @version 1.0
 */
public interface PM3200DataAccessor extends MeterDataAccessor {

	/**
	 * Get the device name.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	Long getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision, as {@literal X.Y.Z}.
	 */
	String getFirmwareRevision();

	/**
	 * Get the model.
	 * 
	 * @return the model
	 */
	String getModel();

	/**
	 * Get the manufacture date.
	 * 
	 * @return the data
	 */
	LocalDateTime getManufactureDate();

	/**
	 * Get the manufacturer name.
	 * 
	 * @return the manufacturer name
	 */
	String getManufacturer();

	/**
	 * Get the number of phases configured.
	 * 
	 * @return the phase count
	 */
	Integer getPhaseCount();

	/**
	 * Get the number of wires configured.
	 * 
	 * @return the wire count
	 */
	Integer getWireCount();

	/**
	 * Get the power system configuration.
	 * 
	 * @return the power system
	 */
	PowerSystem getPowerSystem();

}
