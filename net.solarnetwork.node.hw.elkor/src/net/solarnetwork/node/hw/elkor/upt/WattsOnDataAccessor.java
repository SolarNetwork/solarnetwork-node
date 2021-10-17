/* ==================================================================
 * WattsOnDataAccessor.java - 14/08/2020 9:37:40 AM
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

package net.solarnetwork.node.hw.elkor.upt;

import net.solarnetwork.node.domain.AcEnergyDataAccessor;

/**
 * API for accessing WattsOn data elements.
 * 
 * @author matt
 * @version 2.0
 */
public interface WattsOnDataAccessor extends AcEnergyDataAccessor {

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	Number getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision
	 */
	Number getFirmwareRevision();

	/**
	 * Get the power transformer ratio.
	 * 
	 * @return the ratio
	 */
	Ratio getPowerTransformerRatio();

	/**
	 * Get the current transformer ratio.
	 * 
	 * @return the ratio
	 */
	Ratio getCurrentTransformerRatio();
}
