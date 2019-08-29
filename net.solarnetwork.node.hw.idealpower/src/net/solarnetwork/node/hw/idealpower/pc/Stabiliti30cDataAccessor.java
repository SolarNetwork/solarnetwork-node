/* ==================================================================
 * Stabiliti30cDataAccessor.java - 28/08/2019 12:06:26 pm
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

package net.solarnetwork.node.hw.idealpower.pc;

import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.node.domain.DataAccessor;

/**
 * API for reading Stabiliti 30C series power control system data.
 * 
 * @author matt
 * @version 1.0
 */
public interface Stabiliti30cDataAccessor extends DataAccessor {

	/* === Port 1 === */

	/**
	 * Get the configured P1 port type.
	 * 
	 * @return the port type
	 */
	Stabiliti30cAcPortType getP1PortType();

	/**
	 * Get the P1 active (real) power, in watts.
	 * 
	 * <p>
	 * This is positive when exporting to grid.
	 * </p>
	 * 
	 * @return the active power
	 */
	Integer getP1ActivePower();

	/* === Port 2 === */

	/**
	 * Get the P2 voltage, positive to negative, in volts.
	 * 
	 * @return the voltage
	 */
	Float getP2Voltage();

	/**
	 * Get the P2 power, positive when exporting to battery (charging), in
	 * watts.
	 * 
	 * @return the power
	 */
	Integer getP2Power();

	/**
	 * Get the P2 current, in amps.
	 */
	Float getP2Current();

	/* === Port 3 === */

	/**
	 * Get the P3 voltage, positive to negative, in volts.
	 * 
	 * @return the voltage
	 */
	Float getP3Voltage();

	/**
	 * Get the P3 power, negative when importing from PV, watts.
	 * 
	 * @return the power
	 */
	Integer getP3Power();

	/**
	 * Get the P3 current, in amps.
	 * 
	 * @return the current
	 */
	Float getP3Current();

	/* === Device information === */

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
	 * Get the communications module version.
	 * 
	 * @return the comms version
	 */
	String getCommunicationsVersion();

	/* === Operational status === */

	/**
	 * Get the system information.
	 * 
	 * @return the information
	 */
	Set<Stabiliti30cSystemInfo> getSystemInfo();

	/**
	 * Get the operating mode.
	 * 
	 * @return the mode
	 */
	Stabiliti30cOperatingMode getOperatingMode();

	/**
	 * Get the system status.
	 * 
	 * @return the status
	 */
	Set<Stabiliti30cSystemStatus> getSystemStatus();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 */
	SortedSet<Stabiliti30cFault> getFaults();

}
