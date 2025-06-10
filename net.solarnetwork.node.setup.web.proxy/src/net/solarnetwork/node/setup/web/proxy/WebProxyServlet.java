/* ==================================================================
 * WebProxyServlet.java - 25/03/2019 9:10:44 am
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP proxy servlet configured via a {@link WebProxyConfiguration}.
 * 
 * @author matt
 * @version 2.0
 */
public class WebProxyServlet extends ProxyServlet {

	private static final long serialVersionUID = -7375696361855326998L;

	private static final Logger LOG = LoggerFactory.getLogger(WebProxyServlet.class);

	private static final ServletConfig GLOBAL_SERVLET_CONFIG = new StaticServletConfig();

	private final ServletConfig servletConfig;
	private final WebProxyConfiguration configuration;
	private final String proxyPath;

	private final String configProxyPath;
	private final String proxyTargetUri;

	private static class StaticServletConfig implements ServletConfig {

		@Override
		public String getServletName() {
			return "WebProxyServlet";
		}

		@Override
		public ServletContext getServletContext() {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return null;
		}

		@Override
		public String getInitParameter(String name) {
			return null;
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param configuration
	 *        the configuration
	 * @param proxyPathPrefix
	 *        a prefix to use for the proxy path
	 */
	public WebProxyServlet(WebProxyConfiguration configuration, String proxyPathPrefix) {
		super();
		if ( configuration == null ) {
			throw new IllegalArgumentException("WebProxyConfiguration is required");
		}
		if ( configuration.getUid() == null ) {
			throw new IllegalArgumentException("WebProxyConfiguration.uid is required");
		}
		if ( proxyPathPrefix == null ) {
			throw new IllegalArgumentException("A proxy path prefix is required");
		}
		this.servletConfig = GLOBAL_SERVLET_CONFIG;
		this.configuration = configuration;
		this.proxyPath = (proxyPathPrefix + (proxyPathPrefix.endsWith("/") ? "" : "/")
				+ configuration.getProxyPath());

		// stash copies of this, in case configuration changes
		this.configProxyPath = configuration.getProxyPath();
		this.proxyTargetUri = configuration.getProxyTargetUri();
	}

	@Override
	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	@Override
	protected HttpClient createHttpClient() {
		// @formatter:off
	    return HttpClientBuilder.create()
	        .setDefaultRequestConfig(buildRequestConfig())
	        .setConnectionTimeToLive(1, TimeUnit.MINUTES)
	        .disableCookieManagement()
	        .disableRedirectHandling()
	        .build();
	    // @formatter:on
	}

	@Override
	protected void copyProxyCookie(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, String headerValue) {
		List<HttpCookie> cookies = HttpCookie.parse(headerValue);
		String path = proxyPath;
		if ( !path.endsWith("/") ) {
			path += "/";
		}
		for ( HttpCookie cookie : cookies ) {
			Cookie responseCookie = new Cookie(cookie.getName(), cookie.getValue());
			responseCookie.setComment(cookie.getComment());
			responseCookie.setMaxAge((int) cookie.getMaxAge());
			responseCookie.setPath(path);
			responseCookie.setHttpOnly(cookie.isHttpOnly());
			responseCookie.setSecure(cookie.getSecure());
			responseCookie.setVersion(cookie.getVersion());
			LOG.debug("Remapped cookie {} path to {}", cookie, proxyPath);
			servletResponse.addCookie(responseCookie);
		}
	}

	@Override
	protected String rewritePathInfoFromRequest(HttpServletRequest servletRequest) {
		String pathInfo = servletRequest.getPathInfo();
		if ( pathInfo != null && pathInfo.startsWith(proxyPath)
				&& pathInfo.length() >= proxyPath.length() ) {
			pathInfo = pathInfo.substring(proxyPath.length());
		}
		return pathInfo;
	}

	@Override
	protected String rewriteUrlFromResponse(HttpServletRequest servletRequest, String theUrl) {
		int redirectUrlPos = theUrl.indexOf("://");
		if ( redirectUrlPos >= 0 ) {
			redirectUrlPos = theUrl.indexOf("/", redirectUrlPos + 3);
		}
		if ( redirectUrlPos < 0 ) {
			redirectUrlPos = 0;
		}

		StringBuffer curUrl = servletRequest.getRequestURL();
		int pos = curUrl.indexOf("://");
		if ( pos >= 0 ) {
			if ( (pos = curUrl.indexOf("/", pos + 3)) >= 0 ) {
				curUrl.setLength(pos);
			}
		}
		if ( !theUrl.startsWith(proxyPath, redirectUrlPos) ) {
			curUrl.append(proxyPath);
		}
		curUrl.append(theUrl, redirectUrlPos, theUrl.length());
		theUrl = curUrl.toString();

		return theUrl;
	}

	private enum SearchMode {

		TagOpen(new char[] { '<' }, new char[] { '<' }),

		Action(
				new char[] { 'a', 'c', 't', 'i', 'o', 'n' },
				new char[] { 'A', 'C', 'T', 'I', 'O', 'N' },
				true),

		Href(new char[] { 'h', 'r', 'e', 'f' }, new char[] { 'H', 'R', 'E', 'F' }, true),

		Src(new char[] { 's', 'r', 'c' }, new char[] { 'S', 'R', 'C' }, true),

		Equal(new char[] { '=' }, new char[] { '=' }),

		Quote(new char[] { '"' }, new char[] { '\'' }),

		Slash(new char[] { '/' }, new char[] { '/' });

		public static final Set<SearchMode> ATTRIBUTES = attributeSet();

		private final int len;
		private final char[] lower;
		private final char[] upper;
		private final boolean attribute;

		private SearchMode(char[] lower, char[] upper) {
			this(lower, upper, false);
		}

		private SearchMode(char[] lower, char[] upper, boolean attribute) {
			this.len = lower.length;
			this.lower = lower;
			this.upper = upper;
			this.attribute = attribute;
		}

		public boolean isMatch(char c, int pos) {
			if ( pos >= len ) {
				return false;
			}
			return (c == lower[pos] || c == upper[pos]);
		}

		public int getLength() {
			return len;
		}

		public boolean isAttribute() {
			return attribute;
		}

		public static Set<SearchMode> attributeSet() {
			Set<SearchMode> s = new HashSet<>(SearchMode.values().length);
			for ( SearchMode m : SearchMode.values() ) {
				if ( m.isAttribute() ) {
					s.add(m);
				}
			}
			return EnumSet.copyOf(s);
		}

	}

	@Override
	protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest) throws IOException {
		final String forwardPath = servletRequest.getHeader("X-Forwarded-Path"); // support SolarSSH proxy
		final HttpEntity entity = proxyResponse.getEntity();
		final BufferedReader reader = rewritableContentReader(entity);
		if ( reader != null ) {
			try (PrintWriter out = servletResponse.getWriter()) {
				SearchMode mode = SearchMode.TagOpen;
				char[] buf = new char[4096];
				int len = 0;
				int modePtr = 0;
				boolean afterWhitespace = false;
				boolean attributeMode = false;
				while ( (len = reader.read(buf)) > 0 ) {
					for ( int i = 0; i < len; i++ ) {
						char c = buf[i];
						if ( c == '>' ) {
							// end tag; always switch to TagOpen
							mode = SearchMode.TagOpen;
							attributeMode = false;
						} else if ( attributeMode ) {
							if ( afterWhitespace ) {
								for ( SearchMode m : SearchMode.ATTRIBUTES ) {
									if ( m.isMatch(c, 0) ) {
										mode = m;
										modePtr++;
										attributeMode = false;
									}
								}
							}
						} else if ( mode.isMatch(c, modePtr) ) {
							modePtr++;
							if ( modePtr >= mode.getLength() ) {
								// found current mode match
								modePtr = 0;
								switch (mode) {
									case TagOpen:
										attributeMode = true;
										break;

									case Action:
									case Href:
									case Src:
										mode = SearchMode.Equal;
										break;

									case Equal:
										mode = SearchMode.Quote;
										break;

									case Quote:
										mode = SearchMode.Slash;
										break;

									case Slash:
										// got a href="/ so insert proxy path
										if ( forwardPath != null ) {
											out.write(forwardPath);
										}
										out.write(proxyPath);
										mode = SearchMode.TagOpen;
										break;

									default:
										// nothing
								}
							}
						} else {
							modePtr = 0;
							if ( mode == SearchMode.Slash ) {
								// not a slash character, so no link re-writing
								mode = SearchMode.TagOpen;
							} else if ( mode.isAttribute() ) {
								// not an attribute match now, back to attribute search
								attributeMode = true;
							}
						}
						out.write(c);
						if ( attributeMode ) {
							afterWhitespace = Character.isWhitespace(c);
						}
					}
				}
			} finally {
				try {
					reader.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
		} else {
			OutputStream servletOutputStream = servletResponse.getOutputStream();
			entity.writeTo(servletOutputStream);
		}
	}

	private BufferedReader rewritableContentReader(HttpEntity response) throws IOException {
		if ( response == null || configuration == null || !configuration.isContentLinksRewrite() ) {
			return null;
		}
		ContentType contentType = ContentType.getLenientOrDefault(response);
		if ( !contentType.getMimeType().contains("html") ) {
			return null;
		}
		Charset charset = contentType.getCharset();
		if ( charset == null ) {
			charset = Charset.forName("UTF-8");
		}
		Header encoding = response.getContentEncoding();
		if ( encoding != null ) {
			return null;
		}
		InputStream in = response.getContent();
		if ( in == null ) {
			return null;
		}
		return new BufferedReader(new InputStreamReader(in, charset));
	}

	@Override
	protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
		String result = super.rewriteUrlFromRequest(servletRequest);
		LOG.info("Web proxy {} {} -> {}", servletRequest.getMethod(), servletRequest.getRequestURI(),
				result);
		return result;
	}

	@Override
	protected String getConfigParam(String key) {
		switch (key) {
			case ProxyServlet.P_TARGET_URI:
				return configuration.getProxyTargetUri();

			case ProxyServlet.P_CONNECTTIMEOUT:
				return "30000";

			case ProxyServlet.P_PRESERVECOOKIES:
				return Boolean.TRUE.toString();

			default:
				return null;
		}
	}

	/**
	 * Get the proxy configuration.
	 * 
	 * @return the configuration
	 */
	public WebProxyConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Get the in-use proxy path.
	 * 
	 * @return the proxy path
	 */
	public String getProxyPath() {
		return configProxyPath;
	}

	/**
	 * Get the in-use proxy target URI.
	 * 
	 * @return the proxy target URI
	 */
	public String getProxyTargetUri() {
		return proxyTargetUri;
	}

}
