/* ==================================================================
 * TestHttpHandler.java - 19/05/2017 4:09:05 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.tesla.powerwall.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
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

	/** A class-level logger. */
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

	/**
	 * Respond with JSON content.
	 * <p>
	 * If a {@code Content-Encoding} response header has been set to either
	 * {@code deflate} or {@code gzip} then the resource data will be compressed
	 * accordingly.
	 * </p>
	 * 
	 * @param response
	 *        the HTTP response
	 * @param json
	 *        the JSON to respond with
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected void respondWithJson(HttpServletResponse response, String json) throws IOException {
		respondWithContent(response, "application/json; charset=utf-8", json.getBytes("UTF-8"));
	}

	/**
	 * Respond with a JSON resource.
	 * 
	 * <p>
	 * If a {@code Content-Encoding} response header has been set to either
	 * {@code deflate} or {@code gzip} then the resource data will be compressed
	 * accordingly.
	 * </p>
	 * 
	 * @param response
	 *        the HTTP response
	 * @param resource
	 *        the resource name
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected void respondWithJsonResource(HttpServletResponse response, String resource)
			throws IOException {
		respondWithResource(response, resource, "application/json; charset=utf-8");
	}

	/**
	 * Respond with content.
	 * 
	 * <p>
	 * If a {@code Content-Encoding} response header has been set to either
	 * {@code deflate} or {@code gzip} then the resource data will be compressed
	 * accordingly.
	 * </p>
	 * 
	 * @param response
	 *        the HTTP response
	 * @param contentType
	 *        the content type
	 * @param data
	 *        the data
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected void respondWithContent(HttpServletResponse response, String contentType, byte[] data)
			throws IOException {
		response.setContentType(contentType);
		FileCopyUtils.copy(data, outputStream(response));
	}

	/**
	 * Respond with a resource.
	 * 
	 * <p>
	 * If a {@code Content-Encoding} response header has been set to either
	 * {@code deflate} or {@code gzip} then the resource data will be compressed
	 * accordingly.
	 * </p>
	 * 
	 * @param response
	 *        the HTTP response
	 * @param resource
	 *        the resource name
	 * @param contentType
	 *        the resource content type
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected void respondWithResource(HttpServletResponse response, String resource, String contentType)
			throws IOException {
		response.setContentType(contentType);
		InputStream in = getClass().getResourceAsStream(resource);
		if ( in == null ) {
			throw new FileNotFoundException(
					"Resource [" + resource + "] not found from class " + getClass().getName());
		}
		FileCopyUtils.copy(in, outputStream(response));
	}

	private OutputStream outputStream(HttpServletResponse response) throws IOException {
		OutputStream out = response.getOutputStream();
		String enc = response.getHeader("Content-Encoding");
		if ( "gzip".equals(enc) ) {
			out = new GZIPOutputStream(out);
		} else if ( "deflate".equals(enc) ) {
			out = new DeflaterOutputStream(out);
		}
		return out;
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
