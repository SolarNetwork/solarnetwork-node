/* ==================================================================
 * SetupResource.java - 21/09/2016 5:56:17 AM
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
import java.util.Collections;
import java.util.Set;

/**
 * API for a setting resource.
 * 
 * @author matt
 * @version 1.0
 */
public interface SetupResource {

	/** The role of an authenticated user. */
	String AUTHENTICATED_USER_ROLE = "USER";

	/** A {@code contentType} for CSS. */
	String CSS_CONTENT_TYPE = "text/css";

	/** A {@code contentType} for JavaScript. */
	String JAVASCRIPT_CONTENT_TYPE = "application/javascript";

	/** A {@code contentType} for JSON data. */
	String JSON_CONTENT_TYPE = "application/json";

	/**
	 * A value to return from {@link #getCacheMaximumSeconds()} if caching
	 * should be disabled.
	 */
	int CACHE_DISABLED = -1;

	/**
	 * A convenient value for the {@link #getSupportedConsumerTypes()} method to
	 * support web consumers.
	 */
	Set<String> WEB_CONSUMER_TYPES = Collections.singleton(SetupResourceProvider.WEB_CONSUMER_TYPE);

	/**
	 * A convenient value for the {@link #getRequiredRoles()} method to support
	 * any authenticated user.
	 */
	Set<String> USER_ROLES = Collections.singleton(AUTHENTICATED_USER_ROLE);

	/**
	 * Get a globally unique identifier for this resource.
	 * 
	 * @return The identifier.
	 */
	String getResourceUID();

	/**
	 * Get the content type of this resource.
	 * 
	 * @return The content type.
	 */
	String getContentType();

	/**
	 * Get a set of required security roles, or {@code null} if none required.
	 * The set is treated such that <em>any</em> matching role is allowed
	 * access, that is the roles are logically {@code OR}'ed together.
	 * 
	 * @return A set of required roles, or {@code null} if no role required.
	 */
	Set<String> getRequiredRoles();

	/**
	 * Get a set of supported consumer types, or {@code null} if <b>all</b>
	 * types are supported.
	 * 
	 * @return A set of supported consumer types, or {@code null} if all types
	 *         are supported.
	 */
	Set<String> getSupportedConsumerTypes();

	/**
	 * Get a maximum number of seconds this resource may be cached for, or
	 * {@code -1} if no caching should be allowed.
	 * 
	 * @return The maximum number of seconds the resource may be cached for.
	 */
	int getCacheMaximumSeconds();

	/**
	 * Determine the content length for this resource.
	 * 
	 * @return the content length, or -1 if not known
	 * @throws IOException
	 *         if the resource cannot be resolved (in the file system or as some
	 *         other known physical resource type)
	 */
	long contentLength() throws IOException;

	/**
	 * Determine the last-modified timestamp for this resource.
	 * 
	 * @return the last modified timestamp, or -1 if not known
	 * @throws IOException
	 *         if the resource cannot be resolved (in the file system or as some
	 *         other known physical resource type)
	 */
	long lastModified() throws IOException;

	/**
	 * Return a new {@link InputStream}.
	 * 
	 * This method should return a new stream each time it is called.
	 * 
	 * @return the input stream for the underlying resource (must not be
	 *         {@code null})
	 * @throws IOException
	 *         if the stream could not be opened
	 */
	InputStream getInputStream() throws IOException;

}
