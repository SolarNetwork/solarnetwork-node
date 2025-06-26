/* ==================================================================
 * DynamicOrRequestMatcher.java - 2/04/2019 10:24:53 am
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

package net.solarnetwork.node.setup.web.support;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.RequestMatcher;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.service.OptionalServiceCollection;

/**
 * {@link RequestMatcher} similar to
 * {@link org.springframework.security.web.util.matcher.OrRequestMatcher} but
 * uses an optional service collection.
 * 
 * @author matt
 * @version 2.0
 * @since 1.38
 */
public class DynamicOrRequestMatcher implements RequestMatcher {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalServiceCollection<RequestMatcher> requestMatchers;

	/**
	 * Creates a new instance
	 *
	 * @param requestMatchers
	 *        the {@link RequestMatcher} instances to try
	 */
	public DynamicOrRequestMatcher(OptionalServiceCollection<RequestMatcher> requestMatchers) {
		this.requestMatchers = requestMatchers;
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		Iterable<RequestMatcher> matchers = (requestMatchers != null ? requestMatchers.services()
				: Collections.emptyList());
		for ( RequestMatcher matcher : matchers ) {
			log.trace("Trying to match using {}", matcher);
			if ( matcher.matches(request) ) {
				log.trace("matched");
				return true;
			}
		}
		log.trace("No matches found");
		return false;
	}

	@Override
	public String toString() {
		Iterable<RequestMatcher> matchers = (requestMatchers != null ? requestMatchers.services()
				: Collections.emptyList());
		return "DynamicOrRequestMatcher{requestMatchers=" + matchers + "}";
	}

}
