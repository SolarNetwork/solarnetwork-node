/* ==================================================================
 * SDMDataAccessor.java - 25/01/2016 5:42:42 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.deson.meter;

import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.domain.AcPhase;

/**
 * Common API for SDM meter data.
 * 
 * @author matt
 * @since 2.0
 */
public interface SDMDataAccessor extends AcEnergyDataAccessor {

	/**
	 * Test if a particular phase is supported by the device. The SDM-120, for
	 * example, only supports a single phase. Also the SDM-360 might be
	 * configured in a single phase (two wire) configuration.
	 * 
	 * @param phase
	 *        The phase to test.
	 * @return <em>true</em> if the given {@code phase} is supported
	 * @since 1.1
	 */
	boolean supportsPhase(AcPhase phase);

	/**
	 * Get the device serial number.
	 * 
	 * @return the servial number
	 */
	String getSerialNumber();

	/**
	 * Get the device type.
	 * 
	 * @return the device type
	 */
	SDMDeviceType getDeviceType();

	/**
	 * Get the wiring mode.
	 * 
	 * @return the wiring mode, or {@literal null} if not known
	 */
	SDMWiringMode getWiringMode();

	/**
	 * Get the system time control data was read from the actual device. If
	 * never read, then return {@code 0}.
	 * 
	 * @return the control data timestamp
	 */
	long getControlDataTimestamp();

}
