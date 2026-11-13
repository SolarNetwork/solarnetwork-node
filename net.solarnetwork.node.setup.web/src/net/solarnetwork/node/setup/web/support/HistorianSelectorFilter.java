/* ==================================================================
 * HistorianSelectorFilter.java - 13 Nov 2026 2:50:15 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.MappingMatch;

/**
 * Servlet filter that selects the unfiltered datum historian with a URL path
 * prefix.
 *
 * <p>
 * A request for an API path under the {@link #UNFILTERED_PATH_PREFIX} prefix,
 * for example {@code /unfiltered/solarquery/api/v1/sec/nodes}, is passed on
 * with the prefix removed from its path, and with the
 * {@link #HISTORIAN_ROLE_ATTR} request attribute set to
 * {@link #UNFILTERED_ROLE}. The prefix is removed from the request URI, the
 * request URL, the servlet path or path info, and the servlet mapping, so
 * everything after this filter sees an ordinary API request: request security,
 * the verification of request signatures, and request dispatching. Clients
 * therefore sign the path without the prefix, and send the request to the
 * prefixed path.
 * </p>
 *
 * <p>
 * All other requests are passed on unchanged.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public class HistorianSelectorFilter implements Filter {

	/** The path prefix that selects the unfiltered historian. */
	public static final String UNFILTERED_PATH_PREFIX = "/unfiltered";

	/** The request attribute with the role of the historian to use. */
	public static final String HISTORIAN_ROLE_ATTR = HistorianSelectorFilter.class.getName()
			+ ".historianRole";

	/** The role of the filtered datum historian, the default. */
	public static final String FILTERED_ROLE = "filtered";

	/** The role of the unfiltered datum historian. */
	public static final String UNFILTERED_ROLE = "unfiltered";

	/**
	 * The API paths that can be prefixed with {@link #UNFILTERED_PATH_PREFIX}.
	 */
	private static final List<String> API_PATHS = List.of("/api/v1/sec/", "/solarquery/api/v1/sec/");

	/**
	 * Constructor.
	 */
	public HistorianSelectorFilter() {
		super();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ( request instanceof HttpServletRequest req && isUnfilteredApiRequest(req) ) {
			final UnfilteredRequest unfiltered = new UnfilteredRequest(req);
			unfiltered.setAttribute(HISTORIAN_ROLE_ATTR, UNFILTERED_ROLE);
			chain.doFilter(unfiltered, response);
			return;
		}
		chain.doFilter(request, response);
	}

	/**
	 * Get the role of the datum historian to use for a request.
	 *
	 * @param request
	 *        the request
	 * @return the role, {@link #FILTERED_ROLE} unless the request was selected
	 *         for another role by this filter
	 */
	public static String historianRole(ServletRequest request) {
		return (request.getAttribute(HISTORIAN_ROLE_ATTR) instanceof String role ? role : FILTERED_ROLE);
	}

	private static boolean isUnfilteredApiRequest(HttpServletRequest req) {
		final String path = req.getRequestURI().substring(req.getContextPath().length());
		if ( !path.startsWith(UNFILTERED_PATH_PREFIX) ) {
			return false;
		}
		final String apiPath = path.substring(UNFILTERED_PATH_PREFIX.length());
		for ( String p : API_PATHS ) {
			if ( apiPath.startsWith(p) ) {
				return true;
			}
		}
		return false;
	}

	private static @Nullable String withoutPrefix(@Nullable String path) {
		if ( path != null && path.startsWith(UNFILTERED_PATH_PREFIX)
				&& (path.length() == UNFILTERED_PATH_PREFIX.length()
						|| path.charAt(UNFILTERED_PATH_PREFIX.length()) == '/') ) {
			return path.substring(UNFILTERED_PATH_PREFIX.length());
		}
		return path;
	}

	/**
	 * A request with the {@link #UNFILTERED_PATH_PREFIX} removed from its path.
	 */
	private static final class UnfilteredRequest extends HttpServletRequestWrapper {

		private final String requestUri;
		private final StringBuffer requestUrl;
		private final String servletPath;
		private final @Nullable String pathInfo;
		private final HttpServletMapping mapping;

		private UnfilteredRequest(HttpServletRequest request) {
			super(request);
			final String uri = request.getRequestURI();
			final String contextPath = request.getContextPath();
			this.requestUri = contextPath
					+ uri.substring(contextPath.length() + UNFILTERED_PATH_PREFIX.length());

			final String url = request.getRequestURL().toString();
			this.requestUrl = new StringBuffer(
					url.endsWith(uri) ? url.substring(0, url.length() - uri.length()) + requestUri
							: url);

			// the prefix is in the servlet path for a servlet mapped under it,
			// otherwise in the path info
			final String origServletPath = request.getServletPath();
			final String origPathInfo = request.getPathInfo();
			final String newServletPath = withoutPrefix(origServletPath);
			if ( newServletPath != null && !newServletPath.equals(origServletPath) ) {
				this.servletPath = newServletPath;
				this.pathInfo = origPathInfo;
			} else {
				this.servletPath = (origServletPath != null ? origServletPath : "");
				this.pathInfo = withoutPrefix(origPathInfo);
			}

			this.mapping = new UnfilteredMapping(request.getHttpServletMapping());
		}

		@Override
		public String getRequestURI() {
			return requestUri;
		}

		@Override
		public StringBuffer getRequestURL() {
			return new StringBuffer(requestUrl);
		}

		@Override
		public String getServletPath() {
			return servletPath;
		}

		@Override
		public @Nullable String getPathInfo() {
			return pathInfo;
		}

		@Override
		public @Nullable String getPathTranslated() {
			final String path = this.pathInfo;
			return (path != null ? getServletContext().getRealPath(path) : null);
		}

		@Override
		public HttpServletMapping getHttpServletMapping() {
			return mapping;
		}

	}

	/**
	 * A servlet mapping with the {@link #UNFILTERED_PATH_PREFIX} removed from
	 * its match value.
	 */
	private static final class UnfilteredMapping implements HttpServletMapping {

		private final HttpServletMapping delegate;
		private final String matchValue;

		private UnfilteredMapping(HttpServletMapping delegate) {
			super();
			this.delegate = delegate;
			final String value = delegate.getMatchValue();
			final String prefix = UNFILTERED_PATH_PREFIX.substring(1) + '/';
			this.matchValue = (value != null && value.startsWith(prefix)
					? value.substring(prefix.length())
					: value);
		}

		@Override
		public String getMatchValue() {
			return matchValue;
		}

		@Override
		public String getPattern() {
			return delegate.getPattern();
		}

		@Override
		public String getServletName() {
			return delegate.getServletName();
		}

		@Override
		public @Nullable MappingMatch getMappingMatch() {
			return delegate.getMappingMatch();
		}

	}

}
