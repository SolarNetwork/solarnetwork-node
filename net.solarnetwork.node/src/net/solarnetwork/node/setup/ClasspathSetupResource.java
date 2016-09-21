/* ==================================================================
 * ClasspathSetupResource.java - 21/09/2016 7:34:55 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;

/**
 * Static classpath based implementation of {@link SetupResource}.
 * 
 * @author matt
 * @version 1.0
 */
public class ClasspathSetupResource extends BaseStaticSetupResource {

	private final String path;
	private final Class<?> clazz;

	/**
	 * Construct with values. Caching will be one day, and all consumer types
	 * will be supported.
	 * 
	 * @param uid
	 *        the {@code resourceUID} value
	 * @param path
	 *        the classpath resource path
	 * @param clazz
	 *        the class to load the resource relative to
	 * @param contentType
	 *        the content tpye
	 * @throws IOException
	 *         if an error occurs accessing the resource
	 */
	public ClasspathSetupResource(String uid, String path, Class<?> clazz, String contentType)
			throws IOException {
		this(uid, path, clazz, contentType, 86400, null, null);
	}

	/**
	 * Construct with values. Caching will be one day.
	 * 
	 * @param uid
	 *        the {@code resourceUID} value
	 * @param path
	 *        the classpath resource path
	 * @param clazz
	 *        the class to load the resource relative to
	 * @param contentType
	 *        the content type
	 * @param consumerTypes
	 *        the optional consumer types
	 * @throws IOException
	 *         if an error occurs accessing the resource
	 */
	public ClasspathSetupResource(String uid, String path, Class<?> clazz, String contentType,
			Set<String> consumerTypes) throws IOException {
		this(uid, path, clazz, contentType, 86400, consumerTypes, null);
	}

	/**
	 * Construct with values. Caching will be one day.
	 * 
	 * @param uid
	 *        the {@code resourceUID} value
	 * @param path
	 *        the classpath resource path
	 * @param clazz
	 *        the class to load the resource relative to
	 * @param contentType
	 *        the content type
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 * @throws IOException
	 *         if an error occurs accessing the resource
	 */
	public ClasspathSetupResource(String uid, String path, Class<?> clazz, String contentType,
			Set<String> consumerTypes, Set<String> roles) throws IOException {
		this(uid, path, clazz, contentType, 86400, consumerTypes, roles);
	}

	/**
	 * Construct with values.
	 * 
	 * @param uid
	 *        the {@code resourceUID} value
	 * @param path
	 *        the classpath resource path
	 * @param clazz
	 *        the class to load the resource relative to
	 * @param contentType
	 *        the content tpye
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @throws IOException
	 *         if an error occurs accessing the resource
	 */
	public ClasspathSetupResource(String uid, String path, Class<?> clazz, String contentType,
			int cacheSeconds) throws IOException {
		this(uid, path, clazz, contentType, cacheSeconds, null, null);
	}

	/**
	 * Full constructor.
	 * 
	 * @param uid
	 *        the {@code resourceUID} value
	 * @param path
	 *        the classpath resource path
	 * @param clazz
	 *        the class to load the resource relative to
	 * @param contentType
	 *        the content type
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 * @throws IOException
	 *         if an error occurs accessing the resource
	 */
	public ClasspathSetupResource(String uid, String path, Class<?> clazz, String contentType,
			int cacheSeconds, Set<String> consumerTypes, Set<String> roles) throws IOException {
		super(uid, contentType, cacheSeconds, consumerTypes, roles, new ClassPathResource(path, clazz));
		this.path = path;
		this.clazz = clazz;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return clazz.getResourceAsStream(path);
	}

}
