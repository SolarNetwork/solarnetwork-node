/* ==================================================================
 * BasicBeanConfiguration.java - Dec 8, 2009 11:49:53 AM
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

package net.solarnetwork.node.support;

import java.util.Map;

import net.solarnetwork.node.util.BeanConfiguration;

/**
 * Basic implementation of {@link BeanConfiguration}.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>attributes</dt>
 *   <dd>A Map of attributes for the configuration.</dd>
 *   
 *   <dt>configuration</dt>
 *   <dd>A Map of bean property values.</dd>
 *   
 *   <dt>ordering</dt>
 *   <dd>An ordering value. Defaults to {@link #DEFAULT_ORDERING}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Id$
 */
public class BasicBeanConfiguration implements BeanConfiguration {
	
	/** The default value for the {@code ordering} property. */
	public static final Integer DEFAULT_ORDERING = Integer.valueOf(0);
	
	private Map<String, Object> configuration;
	private Map<String, Object> attributes;
	private Integer ordering = DEFAULT_ORDERING;

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	/**
	 * @param configuration the configuration to set
	 */
	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the ordering
	 */
	public Integer getOrdering() {
		return ordering;
	}

	/**
	 * @param ordering the ordering to set
	 */
	public void setOrdering(Integer ordering) {
		this.ordering = ordering;
	}

}
