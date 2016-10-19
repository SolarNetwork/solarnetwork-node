/* ==================================================================
 * BaseStaticSetupResource.java - 21/09/2016 9:01:04 AM
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
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.Resource;

/**
 * Abstract base class for static {@link SetupResource} implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseStaticSetupResource implements SetupResource {

	private final String uid;
	private final String contentType;
	private final int cacheSeconds;
	private final Set<String> consumerTypes;
	private final Set<String> roles;
	private final long contentLength;
	private final long lastModified;
	private final Locale locale;

	/**
	 * Construct with values. Caching will be disabled and the content length
	 * and last modified timestamp will be set to {@code -1}.
	 * 
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 */
	public BaseStaticSetupResource(String uid, String contentType, Set<String> consumerTypes,
			Set<String> roles) {
		this(uid, contentType, null, CACHE_DISABLED, -1, -1, roles, consumerTypes);
	}

	/**
	 * Construct with values.
	 * 
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param contentLength
	 *        the content length
	 * @param lastModified
	 *        the last modified date
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 */
	public BaseStaticSetupResource(String uid, String contentType, Locale locale, int cacheSeconds,
			long contentLength, long lastModified, Set<String> consumerTypes, Set<String> roles) {
		super();
		this.uid = uid;
		this.contentType = contentType;
		this.locale = locale;
		this.cacheSeconds = cacheSeconds;
		this.roles = roles;
		this.contentLength = contentLength;
		this.lastModified = lastModified;
		this.consumerTypes = consumerTypes;
	}

	/**
	 * Construct with values. Caching will be disabled and the content length
	 * and last modified timestamp will be set to {@code -1}.
	 * 
	 * @param uid
	 *        the {@code resourceUID}
	 * @param contentType
	 *        the content type
	 * @param locale
	 *        the locale
	 * @param cacheSeconds
	 *        the maximum cache seconds
	 * @param consumerTypes
	 *        the optional consumer types
	 * @param roles
	 *        the optional required roles
	 * @param resource
	 *        A {@link Resource} to get {@code contentLength} and
	 *        {@code lastModified} values from.
	 * @throws IOException
	 *         if the {@link Resource} throws one when accessed
	 */
	public BaseStaticSetupResource(String uid, String contentType, Locale locale, int cacheSeconds,
			Set<String> roles, Set<String> consumerTypes, Resource resource) throws IOException {
		this(uid, contentType, locale, cacheSeconds, resource.contentLength(), resource.lastModified(),
				roles, consumerTypes);
	}

	@Override
	public String getResourceUID() {
		return uid;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public Set<String> getRequiredRoles() {
		return roles;
	}

	@Override
	public int getCacheMaximumSeconds() {
		return cacheSeconds;
	}

	@Override
	public long contentLength() throws IOException {
		return contentLength;
	}

	@Override
	public long lastModified() throws IOException {
		return lastModified;
	}

	@Override
	public Set<String> getSupportedConsumerTypes() {
		return consumerTypes;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

}
