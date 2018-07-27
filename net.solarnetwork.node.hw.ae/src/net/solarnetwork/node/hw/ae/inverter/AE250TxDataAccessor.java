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

package net.solarnetwork.node.hw.ae.inverter;

/**
 * API for reading AE 250TX data.
 * 
 * @author matt
 * @version 1.0
 */
public interface AE250TxDataAccessor extends InverterDataAccessor {

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
	 * Get the inverter's rated power, in kW.
	 * 
	 * @return the rated power
	 */
	Integer getInverterRatedPower();

}
