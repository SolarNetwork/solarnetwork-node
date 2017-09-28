/* ===================================================================
 * ClassUtils.java
 * 
 * Created Jul 15, 2008 8:20:38 AM
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
 * ===================================================================
 */

package net.solarnetwork.node.util;

import java.util.Map;
import java.util.Set;

/**
 * Utility methods for dealing with classes at runtime.
 * 
 * @author matt
 * @version 1.3
 * @deprecated see {@link net.solarnetwork.util.ClassUtils}
 */
@Deprecated
public final class ClassUtils {

	/* Do not instantiate me. */
	private ClassUtils() {
		super();
	}

	/**
	 * Instantiate a class of a specific interface type.
	 * 
	 * @param <T>
	 *        the desired interface type
	 * @param className
	 *        the class name that implements the interface
	 * @param type
	 *        the desired interface
	 * @return new instance of the desired type
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#instantiateClass(String, Class)}
	 */
	@Deprecated
	public static <T> T instantiateClass(String className, Class<T> type) {
		return net.solarnetwork.util.ClassUtils.instantiateClass(className, type);
	}

	/**
	 * Load a class of a particular type.
	 * 
	 * <p>
	 * This uses the {@code type}'s ClassLoader to load the class. If that is
	 * not available, it will use the current thread's context class loader.
	 * </p>
	 * 
	 * @param <T>
	 *        the desired interface type
	 * @param className
	 *        the class name that implements the interface
	 * @param type
	 *        the desired interface
	 * @return the class
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#loadClass(String, Class)}
	 */
	@Deprecated
	public static <T> Class<? extends T> loadClass(String className, Class<T> type) {
		return net.solarnetwork.util.ClassUtils.loadClass(className, type);
	}

	/**
	 * Set bean property values on an object from a Map.
	 * 
	 * @param o
	 *        the bean to set JavaBean properties on
	 * @param values
	 *        a Map of JavaBean property names and their corresponding values to
	 *        set
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#setBeanProperties(Object, Map)}
	 */
	@Deprecated
	public static void setBeanProperties(Object o, Map<String, ?> values) {
		net.solarnetwork.util.ClassUtils.setBeanProperties(o, values);
	}

	/**
	 * Set bean property values on an object from a Map.
	 * 
	 * @param o
	 *        The bean to set JavaBean properties on.
	 * @param values
	 *        A Map of JavaBean property names and their corresponding values to
	 *        set.
	 * @param ignoreErrors
	 *        Flag to ignore unknown and invalid properties.
	 * @since 1.2
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#setBeanProperties(Object, Map, boolean)}
	 */
	@Deprecated
	public static void setBeanProperties(Object o, Map<String, ?> values, boolean ignoreErrors) {
		net.solarnetwork.util.ClassUtils.setBeanProperties(o, values, ignoreErrors);
	}

	/**
	 * Get a Map of non-null bean properties for an object.
	 * 
	 * @param o
	 *        the object to inspect
	 * @param ignore
	 *        a set of property names to ignore (optional)
	 * @return Map (never null)
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#getBeanProperties(Object, Set)}
	 */
	@Deprecated
	public static Map<String, Object> getBeanProperties(Object o, Set<String> ignore) {
		return net.solarnetwork.util.ClassUtils.getBeanProperties(o, ignore);
	}

	/**
	 * Get a Map of non-null <em>simple</em> bean properties for an object.
	 * 
	 * @param o
	 *        the object to inspect
	 * @param ignore
	 *        a set of property names to ignore (optional)
	 * @return Map (never <em>null</em>)
	 * @since 1.1
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#getSimpleBeanProperties(Object, Set)}
	 */
	@Deprecated
	public static Map<String, Object> getSimpleBeanProperties(Object o, Set<String> ignore) {
		return net.solarnetwork.util.ClassUtils.getSimpleBeanProperties(o, ignore);
	}

	/**
	 * Load a classpath resource into a String.
	 * 
	 * @param resourceName
	 *        the resource to load
	 * @param clazz
	 *        the Class to load the resource from
	 * @return the String
	 * @deprecated see
	 *             {@link net.solarnetwork.util.ClassUtils#getResourceAsString}
	 */
	@Deprecated
	public static String getResourceAsString(String resourceName, Class<?> clazz) {
		return net.solarnetwork.util.ClassUtils.getResourceAsString(resourceName, clazz);
	}

}
