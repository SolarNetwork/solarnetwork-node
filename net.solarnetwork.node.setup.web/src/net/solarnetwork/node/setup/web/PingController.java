/* ==================================================================
 * PingController.java - 14/12/2021 11:46:43 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.node.service.SystemHealthService;
import net.solarnetwork.node.service.SystemHealthService.PingTestResults;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Web API for ping tests.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
@Controller
@RequestMapping("/ping")
public class PingController {

	private final SystemHealthService systemHealthService;

	/**
	 * Constructor.
	 * 
	 * @param systemHealthService
	 *        the system health service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public PingController(SystemHealthService systemHealthService) {
		super();
		this.systemHealthService = requireNonNullArgument(systemHealthService, "systemHealthService");
	}

	/**
	 * Execute a ping test.
	 * 
	 * @param ids
	 *        the IDs of the tests to execute
	 * @return the result
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Response<PingTestResults> executePingTest(
			@RequestParam(name = "ids", required = false) Set<String> ids) {
		return Response.response(systemHealthService.performPingTests(ids));
	}

	/**
	 * Execute a ping test.
	 * 
	 * @param ids
	 *        the IDs of the tests to execute
	 * @param model
	 *        the model
	 * @return the result view
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String executePingTest(@RequestParam(name = "ids", required = false) Set<String> ids,
			Model model) {
		PingTestResults results = systemHealthService.performPingTests(ids);
		model.addAttribute("results", results);
		return "ping";
	}

}
