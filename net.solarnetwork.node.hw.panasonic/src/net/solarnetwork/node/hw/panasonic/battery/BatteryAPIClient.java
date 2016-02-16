/* ==================================================================
 * BatteryAPIClient.java - 16/02/2016 7:11:36 am
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

package net.solarnetwork.node.hw.panasonic.battery;

/**
 * Client API to the Panasonic Battery API service.
 * 
 * The Panasonic Battery API exposes information about battery systems that have
 * been registered with Panasonic.
 * 
 * @author matt
 * @version 1.0
 */
public interface BatteryAPIClient {

	/**
	 * Get the most recently available battery data for a specific registered
	 * email address.
	 * 
	 * @param email
	 *        The battery owner's email address.
	 * @return The most recently available battery data
	 * @throws BatteryAPIException
	 *         If the response indicates an error.
	 */
	BatteryData getCurrentBatteryDataForEmail(String email) throws BatteryAPIException;

}
