/* ==================================================================
 * SystemService.java - 10/02/2017 8:31:04 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node;

/**
 * API for node system services, such as restarting, rebooting, or making system
 * configuration changes.
 * 
 * @author matt
 * @version 1.0
 * @since 1.47
 */
public interface SystemService {

	/**
	 * Exit the node application, stopping the active process.
	 * 
	 * @param syncState
	 *        A flag to indicate (when {@code true}) that any transient data
	 *        should be persisted to permanent storage.
	 */
	void exit(boolean syncState);

	/**
	 * Reboot the device the application is running on.
	 */
	void reboot();

}
