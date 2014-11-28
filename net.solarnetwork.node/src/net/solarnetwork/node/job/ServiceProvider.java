/* ==================================================================
 * ServiceProvider.java - Nov 28, 2014 2:44:23 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.job;

import java.util.Collection;
import java.util.Map;

/**
 * API for a provider of runtime service configuration. This is designed for the
 * scenario where a managed job wants to expose a service that is used by the
 * job instance as a registered service itself.
 * 
 * @author matt
 * @version 1.0
 */
public interface ServiceProvider {

	/**
	 * A single service configuration.
	 */
	interface ServiceConfiguration {

		/**
		 * The service instance.
		 * 
		 * @return The service instance.
		 */
		Object getService();

		/**
		 * The name of all interfaces this service supports.
		 * 
		 * @return An array of interface names the service supports.
		 */
		String[] getInterfaces();

		/**
		 * Get an optional map of service properties.
		 * 
		 * @return An optional map of service properties.
		 */
		Map<String, ?> getProperties();

	}

	/**
	 * Get a collection of service configurations.
	 * 
	 * @return A collection of configuration objects.
	 */
	Collection<ServiceConfiguration> getServiceConfigurations();

}
