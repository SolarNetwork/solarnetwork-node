/* ==================================================================
 * PlaceholderService.java - 25/08/2020 10:38:55 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import java.util.Map;
import net.solarnetwork.service.OptionalService;

/**
 * API for a service that can resolve "placeholder" variables in strings.
 * 
 * @author matt
 * @version 2.0
 * @since 1.76
 */
public interface PlaceholderService {

	/**
	 * Resolve all placeholders.
	 * 
	 * @param s
	 *        the string to resolve placeholders in
	 * @param parameters
	 *        parameters to use while resolving placeholders, or {@literal null}
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 */
	String resolvePlaceholders(String s, Map<String, ?> parameters);

	/**
	 * Register a set of parameters for future use.
	 * 
	 * @param parameters
	 *        the parameters to register
	 */
	void registerParameters(Map<String, ?> parameters);

	/**
	 * Helper to resolve placeholders from an optional
	 * {@link PlaceholderService}.
	 * 
	 * @param service
	 *        the optional service
	 * @param s
	 *        the string to resolve placeholders
	 * @param parameters
	 *        to use while resolving placeholders, or {@literal null}
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 */
	static String resolvePlaceholders(OptionalService<PlaceholderService> service, String s,
			Map<String, ?> parameters) {
		PlaceholderService ps = OptionalService.service(service);
		return (ps != null ? ps.resolvePlaceholders(s, parameters) : s);
	}

}
