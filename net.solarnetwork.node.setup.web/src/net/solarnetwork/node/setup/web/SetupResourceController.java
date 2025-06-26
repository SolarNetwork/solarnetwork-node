/* ==================================================================
 * SetupResourceController.java - 21/09/2016 6:17:04 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceService;

/**
 * Controller for serving setup resources.
 * 
 * @author matt
 * @version 1.1
 */
@Controller
public class SetupResourceController extends BaseSetupWebServiceController {

	private final SetupResourceService resourceService;

	/**
	 * Constructor.
	 * 
	 * @param resourceService
	 *        the setup resource service
	 */
	public SetupResourceController(SetupResourceService resourceService) {
		super();
		this.resourceService = requireNonNullArgument(resourceService, "resourceService");
	}

	/**
	 * Setup a resource.
	 * 
	 * @param id
	 *        the resource ID
	 * @param req
	 *        the request
	 * @param res
	 *        the response
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping({ "/rsrc/{id:.+}", "/a/rsrc/{id:.+}" })
	public void setupResource(@PathVariable("id") String id, WebRequest req, HttpServletResponse res)
			throws IOException {
		final SetupResource rsrc = resourceService.getSetupResource(id, req.getLocale());
		if ( rsrc == null ) {
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if ( rsrc.getRequiredRoles() != null && !hasRequiredyRole(req, rsrc) ) {
			res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		if ( req.checkNotModified(rsrc.lastModified()) ) {
			return;
		}
		respondWithSetupResource(rsrc, res);
	}

	/**
	 * Handle an {@link IOException}.
	 * 
	 * @param e
	 *        the exception
	 * @param res
	 *        the response
	 */
	@ExceptionHandler(IOException.class)
	public void handleIOException(IOException e, HttpServletResponse res) {
		log.warn("IOException serving setup resource: {}", e.getMessage());
		res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	private boolean hasRequiredyRole(WebRequest req, SetupResource rsrc) {
		Set<String> roles = rsrc.getRequiredRoles();
		if ( roles == null || roles.isEmpty() ) {
			return true;
		}
		for ( String role : roles ) {
			if ( req.isUserInRole(role) ) {
				return true;
			}
		}
		return false;
	}

	private void respondWithSetupResource(SetupResource rsrc, HttpServletResponse res)
			throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(rsrc.getContentType()));
		if ( rsrc.lastModified() > 0 ) {
			headers.setLastModified(rsrc.lastModified());
		}
		if ( rsrc.contentLength() > 0 ) {
			headers.setContentLength(rsrc.contentLength());
		}
		if ( rsrc.getCacheMaximumSeconds() > 0 ) {
			headers.setCacheControl(CacheControl.maxAge(rsrc.getCacheMaximumSeconds(), TimeUnit.SECONDS)
					.getHeaderValue());
		}

		res.setStatus(HttpServletResponse.SC_OK);

		for ( Map.Entry<String, List<String>> me : headers.entrySet() ) {
			for ( String value : me.getValue() ) {
				res.addHeader(me.getKey(), value);
			}
		}

		FileCopyUtils.copy(rsrc.getInputStream(), res.getOutputStream());
	}

}
