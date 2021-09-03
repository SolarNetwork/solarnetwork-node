/* ==================================================================
 * DatumService.java - 18/08/2021 7:27:06 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import java.util.Collection;
import java.util.Set;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for a service that supports node-wide datum information.
 * 
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public interface DatumService {

	/**
	 * Get the latest available datum of a given type, optionally filtered by
	 * source IDs.
	 * 
	 * @param <T>
	 *        the type of datum to get
	 * @param sourceIdFilter
	 *        an optional set of Ant-style source ID patterns to filter by
	 * @param type
	 *        the type of datum
	 * @return the matching datum, never {@literal null}
	 */
	<T extends NodeDatum> Collection<T> latest(Set<String> sourceIdFilter, Class<T> type);

}
