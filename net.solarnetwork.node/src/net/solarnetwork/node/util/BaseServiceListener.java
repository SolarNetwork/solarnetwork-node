/* ==================================================================
 * BaseServiceListener.java - Mar 20, 2012 7:36:58 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.util;

import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract helper class for a service listener that registers new OSGi
 * services.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>bundleContext</dt>
 * <dd>The {@link BundleContext} to manage services with.</dd>
 * </dl>
 * 
 * @param <T>
 *        the service type
 * @param <R>
 *        the registered service type
 * @author matt
 * @version 1.0
 */
public abstract class BaseServiceListener<T, R extends RegisteredService<T>> {

	private BundleContext bundleContext = null;

	private final List<R> registeredServices = new LinkedList<R>();

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Register a new OSGi service.
	 * 
	 * @param tracked
	 *        the RegisteredService instance
	 * @param service
	 *        the service to register with OSGi
	 * @param serviceInterfaces
	 *        the interfaces to publish the OSGi service as
	 * @param serviceProps
	 *        properties for the OSGi service
	 */
	protected void addRegisteredService(R tracked, Object service, String[] serviceInterfaces,
			Dictionary<String, ?> serviceProps) {
		log.debug("Registering service [{}] with props {}", service, serviceProps);
		synchronized ( registeredServices ) {
			ServiceRegistration<?> reg = bundleContext.registerService(serviceInterfaces, service,
					serviceProps);
			tracked.setReg(reg);
			registeredServices.add(tracked);
		}
	}

	/**
	 * Track a collection of registered OSGi services.
	 * 
	 * @param tracked
	 *        the RegisteredService instance
	 * @param services
	 *        the service already registered with OSGi
	 */
	protected void addRegisteredService(R tracked, Collection<ServiceRegistration<?>> services) {
		synchronized ( registeredServices ) {
			for ( ServiceRegistration<?> reg : services ) {
				tracked.addReg(reg);
			}
			registeredServices.add(tracked);
		}
	}

	/**
	 * Remove the registered service associated with a specific service.
	 * 
	 * @param tracked
	 *        the tracked service to remove
	 * @param properties
	 *        the service properties
	 */
	protected void removeRegisteredService(T tracked, Map<String, ?> properties) {
		synchronized ( registeredServices ) {
			for ( R regService : registeredServices ) {
				if ( regService.isSameAs(tracked, properties) ) {
					log.debug("Unregistering service [{}] with props {}", tracked);
					regService.unregister();
				}
			}
		}
	}

	protected List<R> getRegisteredServices() {
		return registeredServices;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
