/* ==================================================================
 * SolcastClient.java - 14/10/2022 9:50:54 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.solcast;

import net.solarnetwork.node.domain.datum.AtmosphericDatum;

/**
 * API for accessing Solcast data.
 * 
 * @author matt
 * @version 1.0
 */
public interface SolcastClient {

	/**
	 * Test if the client is configured and can be used.
	 * 
	 * @return {@literal true} if the client is fully configured
	 */
	boolean isConfigured();

	/**
	 * Get the most recent conditions.
	 * 
	 * @param sourceId
	 *        the source ID to assign to the returned datum
	 * @param criteria
	 *        the criteria of the data to query from Solcast
	 * @return the datum, or {@literal null} if nothing available
	 */
	AtmosphericDatum getMostRecentConditions(String sourceId, SolcastCriteria criteria);

}
