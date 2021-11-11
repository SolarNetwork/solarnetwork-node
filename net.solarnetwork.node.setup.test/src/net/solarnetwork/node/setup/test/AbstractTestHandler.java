/* ==================================================================
 * AbstractTestHandler.java - 3/06/2015 2:42:08 pm
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

package net.solarnetwork.node.setup.test;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link AbstractHandler} to aid with unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractTestHandler extends AbstractHandler {

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
			handled = handleInternal(target, request, response, dispatch);
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
	 * @param target
	 * @param request
	 * @param response
	 * @param dispatch
	 * @return {@literal true} if the request was handled successfully, and as
	 *         expected.
	 * @throws Exception
	 *         If any problem occurs.
	 */
	protected abstract boolean handleInternal(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws Exception;

	/**
	 * Test if the handler was called.
	 * 
	 * @return
	 */
	public boolean isHandled() {
		return handled;
	}

}
