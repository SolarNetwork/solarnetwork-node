/* ==================================================================
 * CommonModelAccessor.java - 22/05/2018 9:13:28 AM
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

package net.solarnetwork.node.hw.sunspec;

/**
 * API for accessing common model data.
 * 
 * @author matt
 * @version 1.0
 */
public interface CommonModelAccessor extends ModelAccessor {

	/** The common model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 66;

	@Override
	default int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	default int getModelLength() {
		// some implementations return 65; we always want 66 reported here (to include pad)
		return FIXED_BLOCK_LENGTH;
	}

	/**
	 * Get the device manufacturer.
	 * 
	 * @return the manufacturer
	 */
	String getManufacturer();

	/**
	 * Get the device model name.
	 * 
	 * @return the device model
	 */
	String getModelName();

	/**
	 * Get the device options.
	 * 
	 * @return the options
	 */
	String getOptions();

	/**
	 * Get the device version.
	 * 
	 * @return the version
	 */
	String getVersion();

	/**
	 * Get the serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the device ID (the Modbus unit ID).
	 * 
	 * @return the device address
	 */
	Integer getDeviceAddress();

}
