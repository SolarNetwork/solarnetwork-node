/* ==================================================================
 * MethodAndIgnoreRequestMatcher.java - 2/04/2019 10:42:14 am
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.web.util.matcher.RequestMatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Request matcher for a set of request methods and an optional "ignore"
 * delegate matcher.
 * 
 * @author matt
 * @version 1.0
 * @since 1.38
 */
public class MethodAndIgnoreRequestMatcher implements RequestMatcher {

	/** The default allowed methods. */
	public static final Set<String> DEFAULT_ALLOWED_METHODS = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS")));

	private final Set<String> allowedMethods;
	private final RequestMatcher ignoreMatcher;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * Uses the {@link #DEFAULT_ALLOWED_METHODS} allowed methods.
	 * </p>
	 */
	public MethodAndIgnoreRequestMatcher() {
		this(DEFAULT_ALLOWED_METHODS, null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Uses the {@link #DEFAULT_ALLOWED_METHODS} allowed methods.
	 * </p>
	 * 
	 * @param ignoreMatcher
	 *        the matcher to filter requests
	 */
	public MethodAndIgnoreRequestMatcher(RequestMatcher ignoreMatcher) {
		this(DEFAULT_ALLOWED_METHODS, ignoreMatcher);
	}

	/**
	 * Constructor.
	 * 
	 * @param allowedMethods
	 *        the allowed methods
	 * @param ignoreMatcher
	 *        an optional matcher to ignore enforcement on
	 */
	public MethodAndIgnoreRequestMatcher(Set<String> allowedMethods, RequestMatcher ignoreMatcher) {
		super();
		this.allowedMethods = allowedMethods;
		this.ignoreMatcher = ignoreMatcher;
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		boolean match = !this.allowedMethods.contains(request.getMethod());
		if ( match && ignoreMatcher != null ) {
			match = ignoreMatcher.matches(request);
		}
		return match;
	}

}
