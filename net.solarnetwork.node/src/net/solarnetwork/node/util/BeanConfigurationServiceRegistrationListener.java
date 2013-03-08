/* ==================================================================
 * BeanConfigurationServiceRegistrationListener.java - Dec 8, 2009 10:25:17 AM
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

package net.solarnetwork.node.util;

import java.util.Hashtable;
import java.util.Map;
import net.solarnetwork.node.util.BeanConfigurationServiceRegistrationListener.BeanConfigurationRegisteredService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * An OSGi service registration listener for {@link BeanConfiguration} objects,
 * so they can be used to dynamically configure and publish other OSGi services.
 * 
 * <p>
 * This object acts like a dynamic OSGi service factory. You configure the type
 * of service this factory should create, and when {@link BeanConfiguration} are
 * found new instances of the service will be created and configured from the
 * discovered {@link BeanConfiguration}. This allows the bundle the
 * configuration came from to be completely isolated and unaware of the
 * implementation bundle using that configuration.
 * </p>
 * 
 * <p>
 * The {@link BeanConfiguration#getConfiguration()} Map will be used to
 * configure the properties on instantiated service objects. The keys of this
 * Map should be standard Spring JavaBean property names.
 * </p>
 * 
 * <p>
 * The {@link BeanConfiguration#getAttributes()} Map will be used to create OSGi
 * service properties when the service is registered.
 * </p>
 * 
 * <p>
 * The {@Link BeanConfiguration#getOrdering()} Integer will be used to
 * assign an OSGi ranking to the service when it is registered.
 * </p>
 * 
 * <p>
 * When the {@link BeanConfiguration} is unregistered and the
 * {@link #onUnbind(BeanConfiguration, Map)} method is called, the associated
 * service created by this factory will be unregistered as well.
 * </p>
 * 
 * <p>
 * For example, this might be configured via Spring DM like this:
 * </p>
 * 
 * <pre>
 * &lt;osgi:list id="myConfigurationList"
 * 		interface="net.solarnetwork.node.util.BeanConfiguration" cardinality="0..N">
 * 		&lt;osgi:listener bind-method="onBind" unbind-method="onUnbind" ref="myServiceBean"/>
 * &lt;/osgi:list>
 * 
 * &lt;bean id="myServiceBean" 
 * 		class="net.solarnetwork.node.util.BeanConfigurationServiceRegistrationListener">
 * 		&lt;property name="serviceClass"
 * 			value="net.solarnetwork.node.impl.MyServiceImplementation"/>
 * 		&lt;property name="serviceInterfaces"
 * 			value="net.solarnetwork.node.MyService"/>
 * 		&lt;property name="bundleContext" ref="bundleContext"/>
 * &lt;/bean>
 * </pre>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>serviceClass</dt>
 * <dd>The type of service to create when
 * {@link #onBind(BeanConfiguration, Map)} is called.</dd>
 * 
 * <dt>serviceInterfaces</dt>
 * <dd>An array of interface names to register the OSGi service as.</dd>
 * 
 * <dt>serviceProperties</dt>
 * <dd>An optional Map of properties to register the OSGi service with.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Id$
 */
public class BeanConfigurationServiceRegistrationListener extends
		BaseServiceListener<BeanConfiguration, BeanConfigurationRegisteredService> {

	private Class<?> serviceClass = null;
	private String[] serviceInterfaces = null;
	private Map<String, Object> serviceProperties = null;

	/**
	 * Callback when an object has been registered.
	 * 
	 * <p>
	 * This method will instantiate a new instance of {@link #getServiceClass()}
	 * and configure its properties via the Map returned by
	 * {@link BeanConfiguration#getConfiguration()}. Afterwards it will register
	 * the instance as a service, using the {@link #getServiceInterfaces()} as
	 * the service interfaces and {@link #getServiceProperties()} as the service
	 * properties (if available) combined with the
	 * {@link BeanConfiguration#getAttributes()} (if available).
	 * </p>
	 * 
	 * @param config
	 *        the configuration object
	 * @param properties
	 *        the service properties
	 */
	public void onBind(BeanConfiguration config, Map<String, ?> properties) {
		if ( log.isDebugEnabled() ) {
			log.debug("Bind called on [" + config + "] with props " + properties);
		}
		Object service;
		try {
			service = serviceClass.newInstance();
		} catch ( InstantiationException e ) {
			throw new RuntimeException(e);
		} catch ( IllegalAccessException e ) {
			throw new RuntimeException(e);
		}
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		if ( serviceProperties != null ) {
			props.putAll(serviceProperties);
		}
		if ( config.getAttributes() != null ) {
			props.putAll(config.getAttributes());
		}
		props.put(org.osgi.framework.Constants.SERVICE_RANKING, config.getOrdering());
		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(service);
		wrapper.setPropertyValues(config.getConfiguration());

		addRegisteredService(new BeanConfigurationRegisteredService(config, properties), service,
				serviceInterfaces, props);
	}

	/**
	 * Callback when a trigger has been un-registered.
	 * 
	 * <p>
	 * This method will attempt to un-register a previously registered service.
	 * </p>
	 * 
	 * @param config
	 *        the configuration object
	 * @param properties
	 *        the service properties
	 */
	public void onUnbind(BeanConfiguration config, Map<String, ?> properties) {
		if ( config == null ) {
			// Gemini Blueprint calls this when availability="optional" and no services available
			return;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Unbind called on [" + config + "] with props " + properties);
		}
		removeRegisteredService(config, properties);
	}

	public static class BeanConfigurationRegisteredService extends RegisteredService<BeanConfiguration> {

		public BeanConfigurationRegisteredService(BeanConfiguration config, Map<String, ?> properties) {
			super(config, properties);
		}

		@Override
		public boolean isSameAs(BeanConfiguration other, Map<String, ?> properties) {
			if ( super.isSameAs(other, properties) ) {
				if ( !areMapsSame(getConfig().getConfiguration(), other.getConfiguration()) ) {
					return false;
				}
				if ( !areMapsSame(getConfig().getAttributes(), other.getAttributes()) ) {
					return false;
				}
				return true;
			}
			return false;
		}
	}

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
	}

	public String[] getServiceInterfaces() {
		return serviceInterfaces;
	}

	public void setServiceInterfaces(String[] serviceInterfaces) {
		this.serviceInterfaces = serviceInterfaces;
	}

	public Map<String, Object> getServiceProperties() {
		return serviceProperties;
	}

	public void setServiceProperties(Map<String, Object> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

}
