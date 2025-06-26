/* ==================================================================
 * WebProxyController.java - 25/03/2019 9:30:51 am
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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import net.solarnetwork.service.OptionalServiceCollection;

/**
 * Proxy controller for SolarNode.
 * 
 * @author matt
 * @version 2.0
 */
@CrossOrigin
@Controller
public class WebProxyController {

	// @formatter:off
	private static final ConcurrentMap<String, WebProxyServlet> proxyServletMap 
	      = new ConcurrentHashMap<>(2, 0.9f, 1);
	// @formatter:on

	private final OptionalServiceCollection<WebProxyConfiguration> configurations;

	/**
	 * Constructor.
	 * 
	 * @param configurations
	 *        the configurations
	 */
	@Autowired
	public WebProxyController(
			@Qualifier("webProxyConfigurations") OptionalServiceCollection<WebProxyConfiguration> configurations) {
		super();
		if ( configurations == null ) {
			throw new IllegalArgumentException("WebProxyConfiguration list must not be null");
		}
		this.configurations = configurations;
	}

	/**
	 * Proxy a HTTP request to the SolarNode associated with a configuration.
	 * 
	 * @param configurationUid
	 *        the {@link WebProxyConfiguration} UID to proxy
	 * @param req
	 *        the request
	 * @param resp
	 *        the response
	 * @throws IOException
	 *         if any communication error occurs
	 * @throws ServletException
	 *         if the {@link WebProxyServlet} cannot be initialized
	 */
	@RequestMapping(value = "/a/webproxy/{uid}/**", method = { RequestMethod.DELETE, RequestMethod.GET,
			RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.POST,
			RequestMethod.PUT, RequestMethod.TRACE })
	public void proxyRequest(@PathVariable("uid") String configurationUid, HttpServletRequest req,
			HttpServletResponse resp) throws IOException, ServletException {
		Iterable<WebProxyConfiguration> configs = (configurations != null ? configurations.services()
				: null);
		WebProxyConfiguration config = (configs != null
				? StreamSupport.stream(configs.spliterator(), false)
						.filter(c -> configurationUid.equals(c.getProxyPath())
								|| configurationUid.equals(c.getUid()))
						.findFirst().orElse(null)
				: null);
		if ( config == null ) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		WebProxyServlet proxy = getOrCreateProxyServlet(config, req);
		if ( proxy == null || proxy.getConfiguration() == null ) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// verify the target URI has not changed
		if ( !proxy.getProxyTargetUri().equals(config.getProxyTargetUri())
				|| !proxy.getProxyPath().equals(config.getProxyPath()) ) {
			// configuration has changed... destroy servlet and recreate with current configuration
			proxy.destroy();
			proxy = recreateAndGetProxyServlet(config, req, proxy);
		}
		if ( proxy == null ) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		proxy.service(req, resp);
	}

	private Function<String, WebProxyServlet> proxyCreator(WebProxyConfiguration config,
			HttpServletRequest req) {
		return k -> {
			if ( config.getProxyTargetUri() == null || config.getProxyTargetUri().isEmpty() ) {
				// not fully configured yet
				return null;
			}
			WebProxyServlet s = new WebProxyServlet(config, req.getContextPath() + "/a/webproxy");
			try {
				s.init();
			} catch ( ServletException e ) {
				throw new RuntimeException(e);
			}
			return s;
		};
	}

	private WebProxyServlet getOrCreateProxyServlet(WebProxyConfiguration config,
			HttpServletRequest req) {
		if ( config == null ) {
			return null;
		}
		return proxyServletMap.computeIfAbsent(config.getUid(), proxyCreator(config, req));
	}

	private synchronized WebProxyServlet recreateAndGetProxyServlet(WebProxyConfiguration config,
			HttpServletRequest req, WebProxyServlet old) {
		if ( config == null ) {
			return null;
		}
		WebProxyServlet s = proxyCreator(config, req).apply(config.getUid());
		if ( s != null ) {
			proxyServletMap.replace(config.getUid(), old, s);
		} else {
			proxyServletMap.remove(config.getUid(), old);
		}
		return proxyServletMap.get(config.getUid());
	}

}
