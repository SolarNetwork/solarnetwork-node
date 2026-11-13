/* ==================================================================
 * HistorianSelectorFilterTests.java - 13 Nov 2026 4:21:48 pm
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

package net.solarnetwork.node.setup.web.support.test;

import static net.solarnetwork.node.setup.web.support.HistorianSelectorFilter.FILTERED_ROLE;
import static net.solarnetwork.node.setup.web.support.HistorianSelectorFilter.UNFILTERED_ROLE;
import static net.solarnetwork.node.setup.web.support.HistorianSelectorFilter.historianRole;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletMapping;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.MappingMatch;
import net.solarnetwork.node.setup.web.support.HistorianSelectorFilter;

/**
 * Test cases for the {@link HistorianSelectorFilter} class.
 *
 * @author matt
 * @version 1.0
 */
public class HistorianSelectorFilterTests {

	private HistorianSelectorFilter filter;

	@Before
	public void setup() {
		filter = new HistorianSelectorFilter();
	}

	/**
	 * Create a request as dispatched to a servlet mapped to {@code /*}.
	 */
	private static MockHttpServletRequest request(String contextPath, String path) {
		final MockHttpServletRequest req = new MockHttpServletRequest("GET", contextPath + path);
		req.setContextPath(contextPath);
		req.setServletPath("");
		req.setPathInfo(path);
		req.setHttpServletMapping(
				new MockHttpServletMapping(path.substring(1), "/*", "setup", MappingMatch.PATH));
		return req;
	}

	private HttpServletRequest filter(HttpServletRequest req) throws Exception {
		final MockFilterChain chain = new MockFilterChain();
		filter.doFilter(req, new MockHttpServletResponse(), chain);
		return (HttpServletRequest) chain.getRequest();
	}

	@Test
	public void unfiltered_solarQueryPath() throws Exception {
		// GIVEN
		final MockHttpServletRequest req = request("",
				"/unfiltered/solarquery/api/v1/sec/nodes/sources");
		req.setQueryString("nodeIds=123");

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result).as("Request wrapped").isNotSameAs(req);
		then(result.getRequestURI()).as("Prefix removed from request URI")
				.isEqualTo("/solarquery/api/v1/sec/nodes/sources");
		then(result.getRequestURL()).as("Prefix removed from request URL")
				.hasToString("http://localhost/solarquery/api/v1/sec/nodes/sources");
		then(result.getContextPath()).as("Context path unchanged").isEmpty();
		then(result.getServletPath()).as("Servlet path unchanged").isEmpty();
		then(result.getPathInfo()).as("Prefix removed from path info")
				.isEqualTo("/solarquery/api/v1/sec/nodes/sources");
		then(result.getHttpServletMapping().getMatchValue())
				.as("Prefix removed from servlet mapping match value")
				.isEqualTo("solarquery/api/v1/sec/nodes/sources");
		then(result.getHttpServletMapping().getPattern()).as("Servlet mapping pattern unchanged")
				.isEqualTo("/*");
		then(result.getHttpServletMapping().getMappingMatch())
				.as("Servlet mapping match unchanged").isEqualTo(MappingMatch.PATH);
		then(result.getQueryString()).as("Query string unchanged").isEqualTo("nodeIds=123");
		then(historianRole(result)).as("Unfiltered historian selected").isEqualTo(UNFILTERED_ROLE);
	}

	@Test
	public void unfiltered_apiPath() throws Exception {
		// GIVEN
		final MockHttpServletRequest req = request("", "/unfiltered/api/v1/sec/datum/stream/datum");

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result.getRequestURI()).as("Prefix removed from request URI")
				.isEqualTo("/api/v1/sec/datum/stream/datum");
		then(result.getPathInfo()).as("Prefix removed from path info")
				.isEqualTo("/api/v1/sec/datum/stream/datum");
		then(historianRole(result)).as("Unfiltered historian selected").isEqualTo(UNFILTERED_ROLE);
	}

	@Test
	public void unfiltered_contextPath() throws Exception {
		// GIVEN
		final MockHttpServletRequest req = request("/ctx", "/unfiltered/solarquery/api/v1/sec/nodes");

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result.getRequestURI()).as("Prefix removed from request URI after context path")
				.isEqualTo("/ctx/solarquery/api/v1/sec/nodes");
		then(result.getRequestURL()).as("Prefix removed from request URL after context path")
				.hasToString("http://localhost/ctx/solarquery/api/v1/sec/nodes");
		then(result.getContextPath()).as("Context path unchanged").isEqualTo("/ctx");
		then(result.getPathInfo()).as("Prefix removed from path info")
				.isEqualTo("/solarquery/api/v1/sec/nodes");
		then(historianRole(result)).as("Unfiltered historian selected").isEqualTo(UNFILTERED_ROLE);
	}

	@Test
	public void unfiltered_defaultServletMapping() throws Exception {
		// GIVEN
		final String path = "/unfiltered/solarquery/api/v1/sec/nodes";
		final MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
		req.setServletPath(path);
		req.setPathInfo(null);
		req.setHttpServletMapping(new MockHttpServletMapping("", "/", "setup", MappingMatch.DEFAULT));

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result.getRequestURI()).as("Prefix removed from request URI")
				.isEqualTo("/solarquery/api/v1/sec/nodes");
		then(result.getServletPath()).as("Prefix removed from servlet path")
				.isEqualTo("/solarquery/api/v1/sec/nodes");
		then(result.getPathInfo()).as("Path info unchanged").isNull();
		then(result.getHttpServletMapping().getMatchValue()).as("Mapping match value unchanged")
				.isEmpty();
		then(historianRole(result)).as("Unfiltered historian selected").isEqualTo(UNFILTERED_ROLE);
	}

	@Test
	public void unfiltered_notApiPath() throws Exception {
		// GIVEN
		final MockHttpServletRequest req = request("", "/unfiltered/a/home");

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result).as("Request not wrapped").isSameAs(req);
		then(historianRole(result)).as("Filtered historian selected").isEqualTo(FILTERED_ROLE);
	}

	@Test
	public void unfiltered_prefixNotPathSegment() throws Exception {
		// GIVEN
		final MockHttpServletRequest req = request("", "/unfilteredx/solarquery/api/v1/sec/nodes");

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result).as("Request not wrapped").isSameAs(req);
		then(historianRole(result)).as("Filtered historian selected").isEqualTo(FILTERED_ROLE);
	}

	@Test
	public void apiPath() throws Exception {
		// GIVEN
		final MockHttpServletRequest req = request("", "/solarquery/api/v1/sec/nodes");

		// WHEN
		final HttpServletRequest result = filter(req);

		// THEN
		then(result).as("Request not wrapped").isSameAs(req);
		then(historianRole(result)).as("Filtered historian selected").isEqualTo(FILTERED_ROLE);
	}

}
