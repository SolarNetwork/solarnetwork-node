/* ==================================================================
 * TestHttpHandler.java - 7/04/2017 4:46:32 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.wu.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

/**
 * Extension of {@link AbstractHandler} to aid with unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class TestHttpHandler extends AbstractHandler {

	private boolean handled = false;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public final void handle(String target, HttpServletRequest request, HttpServletResponse response,
			int dispatch) throws IOException, ServletException {
		log.trace("HTTP target {} request {}", target, request.getRequestURI());
		Enumeration<String> headerNames = request.getHeaderNames();
		while ( headerNames.hasMoreElements() ) {
			String headerName = headerNames.nextElement();
			log.trace("HTTP header {} = {}", headerName, request.getHeader(headerName));
		}
		try {
			handled = handleInternal(request, response);
			((Request) request).setHandled(handled);
		} catch ( IOException e ) {
			throw e;
		} catch ( ServletException e ) {
			throw e;
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * HTTP invocation.
	 * 
	 * @param request
	 *        the request
	 * @param response
	 *        the response
	 * @return <em>true</em> if the request was handled successfully, and as
	 *         expected.
	 * @throws Exception
	 *         If any problem occurs.
	 */
	protected abstract boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

	protected void respondWithJsonResource(HttpServletResponse response, String resource)
			throws IOException {
		respondWithResource(response, resource, "application/json); charset=utf-8");
	}

	protected void respondWithResource(HttpServletResponse response, String resource, String contentType)
			throws IOException {
		response.setContentType(contentType);
		InputStream in = getClass().getResourceAsStream(resource);
		FileCopyUtils.copy(in, response.getOutputStream());
	}

	/**
	 * Test if the handler was called.
	 * 
	 * @return boolean
	 */
	public boolean isHandled() {
		return handled;
	}

}
