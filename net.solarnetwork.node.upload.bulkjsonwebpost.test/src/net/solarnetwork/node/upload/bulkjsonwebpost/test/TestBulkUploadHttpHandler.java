/* ==================================================================
 * TestBulkUploadHttpHandler.java - 8/08/2017 11:11:52 AM
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

package net.solarnetwork.node.upload.bulkjsonwebpost.test;

import static org.junit.Assert.assertEquals;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Helper HTTP handler for bulk JSON upload tests.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class TestBulkUploadHttpHandler extends TestHttpHandler {

	@Override
	protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		assertEquals("/solarin/bulkupload", request.getPathInfo());
		assertEquals("application/json;charset=UTF-8", request.getHeader("Content-Type"));
		assertEquals("gzip", request.getHeader("Content-Encoding"));

		String json = getRequestBody(request);

		log.debug("Received JSON body: {}", json);

		handleJsonPost(request, response, json);
		return true;
	}

	protected abstract void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
			String json) throws Exception;

}
