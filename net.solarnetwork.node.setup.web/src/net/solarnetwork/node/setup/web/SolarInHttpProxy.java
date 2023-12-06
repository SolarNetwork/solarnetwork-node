/* ==================================================================
 * SolarInHttpProxy.java - Nov 19, 2013 4:09:04 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.support.HttpClientSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.SSLService;

/**
 * Proxy HTTP requests to SolarIn.
 * 
 * <p>
 * This is designed to be used by the Settings app, to support calling SolarIn
 * web services without relying on the user's browser be configured to support
 * the SolarIn X.509 certificate.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
@Controller
public class SolarInHttpProxy extends HttpClientSupport {

	private static final String[] DEFAULT_PROXY_HEADERS_IGNORE = new String[] {
			"strict-transport-security", "transfer-encoding" };

	private Set<String> proxyHeadersIgnore = new LinkedHashSet<String>(
			Arrays.asList(DEFAULT_PROXY_HEADERS_IGNORE));

	/**
	 * Constructor.
	 * 
	 * @param identityService
	 *        the identity service
	 * @param sslService
	 *        the SSL service
	 */
	@Autowired
	public SolarInHttpProxy(@Qualifier("identityService") IdentityService identityService,
			@Qualifier("sslService") OptionalService<SSLService> sslService) {
		super();
		setIdentityService(identityService);
		setSslService(sslService);
	}

	/**
	 * Proxy an HTTP request to SolarIn and return the result on a given HTTP
	 * response.
	 * 
	 * @param request
	 *        the request to proxy
	 * @param response
	 *        the response to return the proxy response to
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = { "/a/location", "/a/location/price",
			"/a/location/weather" }, method = RequestMethod.GET)
	public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String context = request.getContextPath();
		String path = request.getRequestURI();
		if ( path.startsWith(context) ) {
			path = path.substring(context.length());
		}
		// map "/a" to "/api/v1/sec"
		path = "/api/v1/sec" + path.substring(2);
		String query = request.getQueryString();
		String url = getIdentityService().getSolarInBaseUrl() + path;
		if ( query != null ) {
			url += '?' + query;
		}
		String accept = request.getHeader("Accept");
		if ( accept == null ) {
			accept = ACCEPT_JSON;
		}
		try {
			URLConnection conn = getURLConnection(url, request.getMethod(), accept);
			if ( conn instanceof HttpURLConnection ) {
				final HttpURLConnection httpConn = (HttpURLConnection) conn;
				final Map<String, List<String>> headers = httpConn.getHeaderFields();
				log.debug("Proxying SolarIn headers: {}", headers);
				for ( Map.Entry<String, List<String>> me : headers.entrySet() ) {
					final String headerName = me.getKey();
					if ( headerName == null || proxyHeadersIgnore.contains(headerName.toLowerCase()) ) {
						log.debug("Not proxying header {}", headerName);
						continue;
					}
					for ( String val : me.getValue() ) {
						response.addHeader(headerName, val);
					}
				}
				response.setStatus(httpConn.getResponseCode());
			}
			FileCopyUtils.copy(conn.getInputStream(), response.getOutputStream());
			response.flushBuffer();
		} catch ( IOException e ) {
			log.debug("Error proxying SolarIn URL [{}]", url, e);
			response.sendError(502, "Problem communicating with SolarIn: " + e.getMessage());
		}
	}

	/**
	 * Configure a set of HTTP headers to <b>not</b> proxy.
	 * 
	 * @param proxyHeadersIgnore
	 *        the headers to ignore
	 */
	public void setProxyHeadersIgnore(Set<String> proxyHeadersIgnore) {
		Set<String> ignores = null;
		if ( proxyHeadersIgnore != null ) {
			ignores = new LinkedHashSet<String>(proxyHeadersIgnore.size());
			for ( String ignore : proxyHeadersIgnore ) {
				if ( ignore == null ) {
					continue;
				}
				ignores.add(ignore.toLowerCase());
			}
		}
		this.proxyHeadersIgnore = proxyHeadersIgnore;
	}

}
