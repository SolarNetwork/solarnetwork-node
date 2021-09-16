/* ==================================================================
 * SetupResourceService.java - 21/09/2016 6:30:10 AM
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

package net.solarnetwork.node.setup;

import java.util.Collection;
import java.util.Locale;

/**
 * API for a centrally managed manager of {@link SetupResource} instances.
 * 
 * @author matt
 * @version 1.0
 */
public interface SetupResourceService {

	/**
	 * Get a specific resource for a resource UID.
	 * 
	 * @param resourceUID
	 *        The ID of the resource to get.
	 * @param locale
	 *        The desired locale.
	 * @return The resource, or {@literal null} if not available.
	 */
	SetupResource getSetupResource(String resourceUID, Locale locale);

	/**
	 * Get a set of resources for a specific consumer type.
	 * 
	 * @param consumerType
	 *        The consumer type to get resources for.
	 * @param locale
	 *        The desired locale.
	 * @return All matching resources, never {@literal null}.
	 */
	Collection<SetupResource> getSetupResourcesForConsumer(String consumerType, Locale locale);

}
