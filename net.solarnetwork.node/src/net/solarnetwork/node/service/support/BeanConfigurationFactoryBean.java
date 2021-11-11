/* ==================================================================
 * BeanConfigurationFactoryBean.java - Dec 10, 2009 10:45:34 AM
 * 
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import java.util.Map;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.FactoryBean;
import net.solarnetwork.node.service.BeanConfiguration;

/**
 * {@link FactoryBean} implementation that creates objects based on properties
 * specified on a {@link BeanConfiguration} object.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>beanClass</dt>
 * <dd>The type of object to create when {@link #getObject()} is called.</dd>
 * 
 * <dt>config</dt>
 * <dd>The {@link BeanConfiguration} to use for configuring the created object
 * instance. The {@link BeanConfiguration#getConfiguration()} will be used to
 * configure properties of any created object.</dd>
 * 
 * <dt>singleton</dt>
 * <dd>If {@literal true} (the default) then only ever create one object instance.
 * Otherwise a new object instance will be created each time
 * {@link #getObject()} is called.</dd>
 * 
 * <dt>staticProperties</dt>
 * <dd>An optional Map of additional properties to configure on the created
 * object(s). These properties will be applied first if configured, followed by
 * the {@link BeanConfiguration} properties, so these can serve as default
 * values if needed.</dd>
 * </dl>
 * 
 * @param <T>
 *        the object type
 * @author matt
 * @version 1.0
 */
public class BeanConfigurationFactoryBean<T> implements FactoryBean<T> {

	private BeanConfiguration config = null;
	private Class<T> beanClass = null;
	private boolean singleton = true;
	private Map<String, ?> staticProperties = null;

	private T singletonObject = null;

	private final Object monitor = new Object();

	@Override
	public T getObject() throws Exception {
		if ( isSingleton() ) {
			synchronized ( monitor ) {
				if ( singletonObject == null ) {
					singletonObject = createObject();
				}
				return singletonObject;
			}
		}
		return createObject();
	}

	private T createObject() throws InstantiationException, IllegalAccessException {
		T obj = beanClass.newInstance();
		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(obj);
		if ( this.staticProperties != null ) {
			wrapper.setPropertyValues(this.staticProperties);
		}
		wrapper.setPropertyValues(config.getConfiguration());
		return obj;
	}

	@Override
	public Class<T> getObjectType() {
		return beanClass;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	/**
	 * @return the config
	 */
	public BeanConfiguration getConfig() {
		return config;
	}

	/**
	 * @param config
	 *        the config to set
	 */
	public void setConfig(BeanConfiguration config) {
		this.config = config;
	}

	/**
	 * @return the beanClass
	 */
	public Class<?> getBeanClass() {
		return beanClass;
	}

	/**
	 * @param beanClass
	 *        the beanClass to set
	 */
	public void setBeanClass(Class<T> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * @param singleton
	 *        the singleton to set
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * @return the staticProperties
	 */
	public Map<String, ?> getStaticProperties() {
		return staticProperties;
	}

	/**
	 * @param staticProperties
	 *        the staticProperties to set
	 */
	public void setStaticProperties(Map<String, ?> staticProperties) {
		this.staticProperties = staticProperties;
	}

}
