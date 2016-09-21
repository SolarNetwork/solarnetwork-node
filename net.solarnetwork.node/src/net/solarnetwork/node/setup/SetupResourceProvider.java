/* ==================================================================
 * SetupResourceProvider.java - 21/09/2016 5:54:13 AM
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

import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * API for a provider of resource(s) to support {@link SettingSpecifier}
 * clients.
 * 
 * @author matt
 * @version 1.0
 */
public interface SetupResourceProvider {

	/** A consumer type for webapp-based resources. */
	String WEB_CONSUMER_TYPE = "web";

	/**
	 * Get a specific resource for a resource UID.
	 * 
	 * @param resourceUID
	 *        The ID of the resource to get.
	 * @return The resource, or {@code null} if not available.
	 */
	SetupResource getSetupResource(String resourceUID);

	/**
	 * Get a set of resources for specific context and content type.
	 * 
	 * A {@code consumerType} represents the type of application the consumer of
	 * the setup resources represents. The {@link #WEB_CONSUMER_TYPE} represents
	 * a webapp, for example, and would be interested in resources such as
	 * JavaScript, CSS, images, etc.
	 * 
	 * @param consumerType
	 *        The consumer type to get all appropriate resources for.
	 * @return All matching resources, never <em>null</em>.
	 */
	List<SetupResource> getSetupResourcesForConsumer(String consumerType);

}
