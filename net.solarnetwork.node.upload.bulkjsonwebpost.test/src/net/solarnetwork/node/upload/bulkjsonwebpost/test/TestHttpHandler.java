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

package net.solarnetwork.node.upload.bulkjsonwebpost.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;

/**
 * Extension of {@link AbstractHandler} to aid with unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class TestHttpHandler extends AbstractHandler {

	private ObjectMapper objectMapper = new ObjectMapper();
	private boolean handled = false;
	private Throwable exception;

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
	 * @return <em>true</em> if the request was handled successfully, and as
	 *         expected.
	 * @throws Exception
	 *         If any problem occurs.
	 */
	protected abstract boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

	protected void respondWithXmlResource(HttpServletResponse response, String resource)
			throws IOException {
		respondWithResource(response, resource, "text/xml); charset=utf-8");
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

	protected void respondWithJsonString(HttpServletResponse response, boolean compress, String json)
			throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		OutputStream out;
		if ( compress ) {
			response.setHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		} else {
			out = response.getOutputStream();
		}
		FileCopyUtils.copy(json.getBytes("UTF-8"), out);
	}

	private static final Pattern CHARSET_PAT = Pattern.compile("charset=([0-9a-z_-]+)",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Get the request body as a string.
	 * 
	 * @param req
	 *        the request
	 * @return the string
	 * @throws IOException
	 *         if any error occurs
	 */
	protected String getRequestBody(HttpServletRequest req) throws IOException {
		String encoding = "UTF-8";
		String contentType = req.getHeader("Content-Type");
		if ( contentType != null ) {
			Matcher m = CHARSET_PAT.matcher(contentType);
			if ( m.find() ) {
				encoding = m.group(1);
			}
		}
		Reader r;
		if ( req.getHeader("Content-Encoding").equalsIgnoreCase("gzip") ) {
			r = new InputStreamReader(new GZIPInputStream(req.getInputStream()), encoding);
		} else {
			r = new InputStreamReader(req.getInputStream(), encoding);
		}
		return FileCopyUtils.copyToString(r);
	}

	/**
	 * Get the request body as a map with string keys.
	 * 
	 * @param req
	 *        the request
	 * @return the map
	 * @throws IOException
	 *         if any error occurs
	 */
	protected Map<String, Object> getRequestBodyJsonMap(HttpServletRequest req) throws IOException {
		return JsonUtils.getStringMap(getRequestBody(req));
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

	/**
	 * Get the configured ObjectMapper.
	 * 
	 * @return the objectMapper
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Set the ObjectMapper to use.
	 * 
	 * @param objectMapper
	 *        the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

}
