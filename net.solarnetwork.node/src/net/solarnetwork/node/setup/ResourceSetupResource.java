/* ==================================================================
 * ResourceSetupResource.java - 23/09/2016 9:36:36 AM
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

import static net.solarnetwork.node.setup.SetupResourceUtils.localeForPath;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.Resource;

/**
 * A {@link SetupResource} that delegates to a {@link Resource}.
 * 
 * @author matt
 * @version 1.1
 */
public class ResourceSetupResource extends BaseStaticSetupResource {

	private final Resource resource;

	/**
	 * Construct from a resource, detecting the locale from the resource
	 * filename.
	 * 
	 * @param resource
	 *        the resource
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 */
	public ResourceSetupResource(Resource resource, String uid, String contentType, int cacheSeconds,
			Set<String> consumerTypes, Set<String> roles) throws IOException {
		super(uid, contentType, localeForPath(resource.getFilename()), cacheSeconds, consumerTypes,
				roles, resource);
		this.resource = resource;
	}

	/**
	 * Construct from a resource, detecting the locale from the resource
	 * filename.
	 * 
	 * @param resource
	 *        the resource
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 * @param scope
	 *        the scope to use
	 * @since 1.1
	 */
	public ResourceSetupResource(Resource resource, String uid, String contentType, int cacheSeconds,
			Set<String> consumerTypes, Set<String> roles, SetupResourceScope scope) throws IOException {
		super(uid, contentType, localeForPath(resource.getFilename()), cacheSeconds, consumerTypes,
				roles, resource, scope);
		this.resource = resource;
	}

	/**
	 * Construct from a resource.
	 * 
	 * @param resource
	 *        the resource
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param locale
	 *        the locale to use
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 */
	public ResourceSetupResource(Resource resource, String uid, String contentType, Locale locale,
			int cacheSeconds, Set<String> consumerTypes, Set<String> roles) throws IOException {
		super(uid, contentType, locale, cacheSeconds, consumerTypes, roles, resource);
		this.resource = resource;
	}

	/**
	 * Construct from a resource.
	 * 
	 * @param resource
	 *        the resource
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param locale
	 *        the locale to use
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 * @param scope
	 *        the scope to use
	 * @since 1.1
	 */
	public ResourceSetupResource(Resource resource, String uid, String contentType, Locale locale,
			int cacheSeconds, Set<String> consumerTypes, Set<String> roles, SetupResourceScope scope)
			throws IOException {
		super(uid, contentType, locale, cacheSeconds, consumerTypes, roles, resource, scope);
		this.resource = resource;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return resource.getInputStream();
	}

}
