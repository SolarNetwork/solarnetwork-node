/* ==================================================================
 * StringFormatObjectFactory.java - Dec 10, 2009 4:07:05 PM
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;

/**
 * Factory for String objects created from a format template.
 * 
 * <p>This method will call {@link String#format(String, Object...)} passing
 * in the {@code template} String and the configured {@code parameters}. If
 * any of the configured parameters are themselves {@link ObjectFactory}
 * instances, then {@link ObjectFactory#getObject()} will be passed as the 
 * parameter value rather than the {@code ObjectFactory} itself. This allows
 * dynamic data to be used as template parameters, for example the current
 * date could be used by way of the {@link CurrentDateObjectFactory}.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>template</dt>
 *   <dd>The base String template to use. This value will be passed to the
 *   {@link String#format(String, Object...)} method.</dd>
 *   
 *   <dt>parameters</dt>
 *   <dd>A list of parameters to pass to the 
 *   {@link String#format(String, Object...)} method. {@link ObjectFactory}
 *   objects are handled specially so that {@link ObjectFactory#getObject()}
 *   are passed as the parameter values instead of the {@code ObjectFactory}
 *   itself.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Id$
 */
public class StringFormatObjectFactory implements ObjectFactory<String> {

	private String template = null;
	private List<?> parameters = null;
	
	public String getObject() {
		List<Object> resolvedParameters = new ArrayList<Object>(parameters.size());
		for ( Object param : parameters ) {
			if ( param instanceof ObjectFactory<?> ) {
				ObjectFactory<?> factory = (ObjectFactory<?>)param;
				resolvedParameters.add(factory.getObject());
			} else {
				resolvedParameters.add(param);
			}
		}
		return String.format(template, resolvedParameters.toArray());
	}

	/**
	 * @return the template
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * @return the parameters
	 */
	public List<?> getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(List<?> parameters) {
		this.parameters = parameters;
	}

}
