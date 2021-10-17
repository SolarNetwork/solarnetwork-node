/* ==================================================================
 * TablesMaintenanceService.java - Jul 29, 2017 2:03:47 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby;

/**
 * API for a service that performs periodic database table maintenance.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
public interface TablesMaintenanceService {

	/**
	 * Perform maintenance on tables, optionally starting after a specific table
	 * key.
	 * 
	 * This method is designed to exit early if the maintenance is taking longer
	 * than desired. If not all tables can be maintained within that time, it
	 * should return a unique key value that can later be passed as the
	 * {@code startAfterKey} argument to this method, to essentially pick up
	 * where the previous invocation left off.
	 * 
	 * @param startAfterKey
	 *        A {@code key} returned from a previous execution of this method,
	 *        to start table maintenance from, or {@literal null} to start at the
	 *        first available table.
	 * @return A {@code key} for the last table processed, or {@literal null} if
	 *         all tables were processed.
	 */
	String processTables(String startAfterKey);

}
