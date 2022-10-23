/* ==================================================================
 * SimpleServiceProviderConfiguration.java - Nov 28, 2014 3:19:52 PM
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

import java.util.Map;

/**
 * Basic configuration bean for a service provider.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleServiceProviderConfiguration implements ServiceProvider.ServiceConfiguration {

	private Object service;
	private String[] interfaces;
	private Map<String, Object> properties;

	@Override
	public Object getService() {
		return service;
	}

	/**
	 * Set the service.
	 * 
	 * @param service
	 *        the service to set
	 */
	public void setService(Object service) {
		this.service = service;
	}

	@Override
	public String[] getInterfaces() {
		return interfaces;
	}

	/**
	 * Set the interfaces.
	 * 
	 * @param interfaces
	 *        the interfaces to set
	 */
	public void setInterfaces(String[] interfaces) {
		this.interfaces = interfaces;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Set the properties.
	 * 
	 * @param properties
	 *        the properties to set
	 */
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

}
