/* ==================================================================
 * IdentityService.java - Feb 24, 2011 4:05:38 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import java.security.Principal;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.setup.SetupException;

/**
 * API for knowing information about the node's identity.
 *
 * @author matt
 * @version 1.2
 */
public interface IdentityService {

	/**
	 * Get the ID of the current node.
	 *
	 * @return node ID, or {@code null} if the ID is not known
	 */
	@Nullable
	Long getNodeId();

	/**
	 * Get a {@link Principal} for the current node.
	 *
	 * @return The node Principal, or {@code null} if none available.
	 */
	@Nullable
	Principal getNodePrincipal();

	/**
	 * Get the host name for the SolarNet central service.
	 *
	 * @return a host name
	 */
	@Nullable
	String getSolarNetHostName();

	/**
	 * Get the host port for the SolarNet central service.
	 *
	 * @return a host name
	 */
	@Nullable
	Integer getSolarNetHostPort();

	/**
	 * Get the URL prefix for the SolarIn service.
	 *
	 * @return a URL prefix
	 */
	@Nullable
	String getSolarNetSolarInUrlPrefix();

	/**
	 * Return an absolute URL to the SolarIn service.
	 *
	 * <p>
	 * This is a convenience method for generating a URL for the correct
	 * SolarNet host, SolarNet port, and SolarIn URL prefix as a single absolute
	 * URL string.
	 * </p>
	 *
	 * @return SolarIn base URL
	 * @throws SetupException
	 *         if the URL is not available
	 */
	String getSolarInBaseUrl() throws SetupException;

	/**
	 * Get the URL to the SolarIn MQTT service.
	 *
	 * @return the SolarIn MQTT URL, or {@code null} if not available
	 * @since 1.2
	 */
	@Nullable
	String getSolarInMqttUrl();

}
