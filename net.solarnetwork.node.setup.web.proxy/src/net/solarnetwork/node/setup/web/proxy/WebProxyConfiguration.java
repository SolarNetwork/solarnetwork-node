/* ==================================================================
 * WebProxyConfiguration.java - 25/03/2019 9:35:12 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.proxy;

import net.solarnetwork.service.Identifiable;

/**
 * A dynamic web proxy configuration.
 * 
 * @author matt
 * @version 2.0
 */
public interface WebProxyConfiguration extends Identifiable {

	/**
	 * Get the URI to proxy.
	 * 
	 * @return the proxy URI, e.g. {@literal http://192.168.1.2:8888}
	 */
	String getProxyTargetUri();

	/**
	 * Get a setup path fragment to use as the proxy path.
	 * 
	 * <p>
	 * This should never return a {@literal null} value. A UUID string could be
	 * used, for example, or a more friendly-facing path could be used.
	 * </p>
	 * 
	 * @return the proxy path
	 */
	String getProxyPath();

	/**
	 * Flag to control response body content link rewriting.
	 * 
	 * @return {@literal true} to rewrite links in response body content,
	 *         {@literal false} otherwise
	 */
	boolean isContentLinksRewrite();

}
