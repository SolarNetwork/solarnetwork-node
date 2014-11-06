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

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Utility methods for dealing with classes at runtime.
 * 
 * @author matt
 * @version 1.1
 */
public final class ClassUtils {

	private static final Set<String> DEFAULT_BEAN_PROP_NAME_IGNORE = new HashSet<String>(
			Arrays.asList(new String[] { "class" }));
	private static final Logger LOG = LoggerFactory.getLogger(ClassUtils.class);

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
	 */
	public static <T> T instantiateClass(String className, Class<T> type) {
		Class<? extends T> clazz = loadClass(className, type);
		try {
			T o = clazz.newInstance();
			return o;
		} catch ( Exception e ) {
			throw new RuntimeException("Unable to instantiate class [" + className + ']', e);
		}
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
	 */
	public static <T> Class<? extends T> loadClass(String className, Class<T> type) {
		try {
			ClassLoader loader = type.getClassLoader();
			if ( loader == null ) {
				loader = Thread.currentThread().getContextClassLoader();
			}
			Class<?> clazz = loader.loadClass(className);
			if ( !type.isAssignableFrom(clazz) ) {
				throw new RuntimeException("Class [" + clazz + "] is not a [" + type + ']');
			}
			return clazz.asSubclass(type);
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException("Unable to load class [" + className + ']', e);
		}
	}

	/**
	 * Set bean property values on an object from a Map.
	 * 
	 * @param o
	 *        the bean to set JavaBean properties on
	 * @param values
	 *        a Map of JavaBean property names and their corresponding values to
	 *        set
	 */
	public static void setBeanProperties(Object o, Map<String, ?> values) {
		BeanWrapper bean = new BeanWrapperImpl(o);
		bean.setPropertyValues(values);
	}

	/**
	 * Get a Map of non-null bean properties for an object.
	 * 
	 * @param o
	 *        the object to inspect
	 * @param ignore
	 *        a set of property names to ignore (optional)
	 * @return Map (never null)
	 */
	public static Map<String, Object> getBeanProperties(Object o, Set<String> ignore) {
		if ( ignore == null ) {
			ignore = DEFAULT_BEAN_PROP_NAME_IGNORE;
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		BeanWrapper bean = new BeanWrapperImpl(o);
		PropertyDescriptor[] props = bean.getPropertyDescriptors();
		for ( PropertyDescriptor prop : props ) {
			if ( prop.getReadMethod() == null ) {
				continue;
			}
			String propName = prop.getName();
			if ( ignore != null && ignore.contains(propName) ) {
				continue;
			}
			Object propValue = bean.getPropertyValue(propName);
			if ( propValue == null ) {
				continue;
			}
			result.put(propName, propValue);
		}
		return result;
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
	 */
	public static Map<String, Object> getSimpleBeanProperties(Object o, Set<String> ignore) {
		if ( ignore == null ) {
			ignore = DEFAULT_BEAN_PROP_NAME_IGNORE;
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		BeanWrapper bean = new BeanWrapperImpl(o);
		PropertyDescriptor[] props = bean.getPropertyDescriptors();
		for ( PropertyDescriptor prop : props ) {
			if ( prop.getReadMethod() == null ) {
				continue;
			}
			String propName = prop.getName();
			if ( ignore != null && ignore.contains(propName) ) {
				continue;
			}
			Class<?> propType = bean.getPropertyType(propName);
			if ( !(propType.isPrimitive() || propType.isEnum()
					|| String.class.isAssignableFrom(propType)
					|| Number.class.isAssignableFrom(propType)
					|| Character.class.isAssignableFrom(propType)
					|| Byte.class.isAssignableFrom(propType) || Date.class.isAssignableFrom(propType)) ) {
				continue;
			}
			Object propValue = bean.getPropertyValue(propName);
			if ( propValue == null ) {
				continue;
			}
			if ( propType.isEnum() ) {
				propValue = propValue.toString();
			} else if ( Date.class.isAssignableFrom(propType) ) {
				propValue = ((Date) propValue).getTime();
			}
			result.put(propName, propValue);
		}
		return result;
	}

	/**
	 * Load a classpath SQL resource into a String.
	 * 
	 * @param resourceName
	 *        the SQL resource to load
	 * @param clazz
	 *        the Class to load the resource from
	 * @return the String
	 */
	public static String getResourceAsString(String resourceName, Class<?> clazz) {
		InputStream in = clazz.getResourceAsStream(resourceName);
		if ( in == null ) {
			throw new RuntimeException("Resource [" + resourceName + "] not found");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			while ( (bytesRead = in.read(buffer)) != -1 ) {
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
			return out.toString();
		} catch ( IOException e ) {
			throw new RuntimeException("Error reading resource [" + resourceName + ']', e);
		} finally {
			try {
				in.close();
			} catch ( IOException ex ) {
				LOG.warn("Could not close InputStream", ex);
			}
			try {
				out.close();
			} catch ( IOException ex ) {
				LOG.warn("Could not close OutputStream", ex);
			}
		}
	}

}
