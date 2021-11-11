/* ==================================================================
 * NodeAssociationFilter.java - Nov 28, 2012 8:37:07 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.GenericFilterBean;
import net.solarnetwork.node.service.IdentityService;

/**
 * Filter to force the user to the node association setup if not already
 * associated.
 * 
 * @author matt
 * @version 2.0
 */
public class NodeAssociationFilter extends GenericFilterBean implements Filter {

	private static final String NODE_ASSOCIATE_PATH = "/associate";
	private static final String CSRF_PATH = "/csrf";
	private static final String WEBSOCKET_PATH = "/ws";
	private static final String PUB_PATH = "/pub/";

	@Autowired
	private IdentityService identityService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ( request instanceof HttpServletRequest && response instanceof HttpServletResponse ) {
			doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
		} else {
			chain.doFilter(request, response);
		}
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final String path = request.getPathInfo();
		final Long nodeId = identityService.getNodeId();
		if ( !(path.startsWith(NODE_ASSOCIATE_PATH) || path.equals(CSRF_PATH)
				|| path.equals(WEBSOCKET_PATH) || path.startsWith(PUB_PATH)) && nodeId == null ) {
			// not associated yet, so redirect to associate start
			response.sendRedirect(request.getContextPath() + NODE_ASSOCIATE_PATH);
		} else if ( nodeId != null && path.startsWith(NODE_ASSOCIATE_PATH) ) {
			// not allowed to visit association URLs once associated
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} else {
			chain.doFilter(request, response);
		}
	}

	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

}
