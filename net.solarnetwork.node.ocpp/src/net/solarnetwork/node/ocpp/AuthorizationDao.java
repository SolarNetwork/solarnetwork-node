/* ==================================================================
 * AuthorizationDao.java - 8/06/2015 7:30:49 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

import java.util.Date;
import java.util.Map;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.IdTagInfo;

/**
 * DAO for a local authorization list.
 * 
 * @author matt
 * @version 1.0
 */
public interface AuthorizationDao {

	/**
	 * Store (create or update) a {@link IdTagInfo} value associated with an ID
	 * tag.
	 * 
	 * @param auth
	 *        The {@link Authorization} to store.
	 */
	void storeAuthorization(Authorization auth);

	/**
	 * Get an {@link Authorization} for a given ID tag.
	 * 
	 * @param idTag
	 *        The ID of the info to get.
	 * @return The associated authorization, or <em>null</em> if not available.
	 */
	Authorization getAuthorization(String idTag);

	/**
	 * Delete all expired authorizations that expired on or before
	 * {@code olderThanDate}.
	 * 
	 * @param olderThanDate
	 *        The expiration date to delete, or <em>null</em> to use the current
	 *        time.
	 * @return The number of authorizations deleted.
	 */
	int deleteExpiredAuthorizations(Date olderThanDate);

	/**
	 * Get a Map of status values with corresponding counts representing the
	 * number of records in the database of that status.
	 * 
	 * @return Map of status counts, never <em>null</em>.
	 */
	Map<AuthorizationStatus, Integer> statusCounts();

}
