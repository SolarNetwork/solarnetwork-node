/* ==================================================================
 * TestHttpHandler.java
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.ibm.wc.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
	private Throwable exception;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public final void handle(String target, HttpServletRequest request, HttpServletResponse response,
			int dispatch) throws IOException, ServletException {

		try {
			handled = handleInternal(request, response);
			((Request) request).setHandled(handled);
		} catch ( Throwable e ) {
			exception = e;
			if ( e instanceof IOException ) {
				throw (IOException) e;
			} else if ( e instanceof ServletException ) {
				throw (ServletException) e;
			} else if ( e instanceof Error ) {
				throw (Error) e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * HTTP invocation.
	 * 
	 * @param request
	 *        the request
	 * @param response
	 *        the response
	 * @return {@literal true} if the request was handled successfully, and as
	 *         expected.
	 * @throws Exception
	 *         If any problem occurs.
	 */
	protected abstract boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

	protected void respondWithResource(HttpServletResponse response, String resource)
			throws IOException {
		respondWithResource(response, resource, "text/json); charset=utf-8");
	}

	protected void respondWithResource(HttpServletResponse response, String resource, String contentType)
			throws IOException {
		response.setContentType(contentType);
		InputStream in = getClass().getResourceAsStream(resource);
		if ( in == null ) {
			throw new FileNotFoundException(
					"Resource [" + resource + "] not found from class " + getClass().getName());
		}
		FileCopyUtils.copy(in, response.getOutputStream());
	}

	/**
	 * Test if the handler was called.
	 * 
	 * @return boolean
	 * @throws Throwable
	 *         if the handler threw an exception or JUnit assertion
	 */
	public boolean isHandled() throws Exception {
		if ( exception != null ) {
			if ( exception instanceof Error ) {
				throw (Error) exception;
			} else if ( exception instanceof Exception ) {
				throw (Exception) exception;
			}
			throw new RuntimeException(exception);
		}
		return handled;
	}

}
