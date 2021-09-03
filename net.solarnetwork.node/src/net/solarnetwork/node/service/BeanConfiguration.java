/* ==================================================================
 * BeanConfiguration.java - Dec 8, 2009 8:51:52 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.service;

import java.util.Map;
import net.solarnetwork.node.service.support.BeanConfigurationServiceRegistrationListener;

/**
 * API for a configuration object.
 * 
 * <p>This API can be used to publish configuration information from one OSGi
 * bundle to another, without knowing the actual implementation of any 
 * target service. For example, imagine two serial IO implementation bundles
 * exist, but the SolarNode application does not know which one the user
 * will choose to use. In this case, the SolarNode application bundle need
 * not import either of the IO implementation bundles directly. Instead it
 * can publish a {@code BeanConfiguration} service that the IO implementations
 * can register to use to configure itself.</p>
 * 
 * <p>The {@link BeanConfigurationServiceRegistrationListener} provides an
 * easy way for implementation bundles to bind to {@link BeanConfiguration}
 * services at runtime and dynamically publish services based on this
 * configuration.</p>
 * 
 * @author matt
 * @version $Id$
 * @see BeanConfigurationServiceRegistrationListener
 */
public interface BeanConfiguration {

	/**
	 * Get the configuration properties as a Map.
	 * 
	 * @return Map of configuration properties
	 */
	Map<String, Object> getConfiguration();
	
	/**
	 * Get configuration attributes as a Map.
	 * 
	 * <p>These attributes are not configuration properties that get applied
	 * directly to services, but can be used to distinguish one configuration
	 * from another.</p>
	 * 
	 * @return Map of configuration attributes
	 */
	Map<String, Object> getAttributes();
	
	/**
	 * Get a configuration ordering.
	 * 
	 * <p>The ordering can be used to treat similar configurations in an
	 * ordered fashion.</p>
	 * 
	 * @return an ordering
	 */
	Integer getOrdering();
	
}
