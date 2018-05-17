/* ==================================================================
 * PM5100DataAccessor.java - 17/05/2018 3:13:41 PM
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
 * API for reading PM5100 series meter data.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public interface PM5100DataAccessor extends MeterDataAccessor {

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
	PM5100Model getModel();

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
	PM5100PowerSystem getPowerSystem();

}
