/* ==================================================================
 * WebBoxDataAccessor.java - 14/09/2020 9:56:46 AM
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

package net.solarnetwork.node.hw.sma.modbus.webbox;

import java.util.Collection;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceDataAccessor;

/**
 * {@link DataAccessor} for WebBox devices themselves (the actual WebBox, not
 * the devices connected to the WebBox).
 * 
 * @author matt
 * @version 1.0
 */
public interface WebBoxDataAccessor extends SmaDeviceDataAccessor {

	/**
	 * Get the version number of the SMA Modbus profile.
	 * 
	 * @return the profile version number
	 */
	Long getModbusProfileVersion();

	/**
	 * Get the counter value that is incremented when data in the Modbus profile
	 * changes.
	 * 
	 * @return the data change counter
	 */
	Long getModbusDataChangeCounter();

	/**
	 * Get a collection of all available references to devices managed by this
	 * WebBox.
	 * 
	 * @return the collection of device references, never {@literal null}
	 */
	Collection<WebBoxDeviceReference> availableDeviceReferences();
}
