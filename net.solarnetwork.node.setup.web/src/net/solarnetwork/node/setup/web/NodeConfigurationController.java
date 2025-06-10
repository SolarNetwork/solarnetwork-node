/* ==================================================================
 * NodeConfigurationController.java - 2/10/2017 12:18:04 PM
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

package net.solarnetwork.node.setup.web;

import static net.solarnetwork.web.domain.Response.response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * REST controller for node configuration.
 * 
 * 
 * @author matt
 * @version 1.0
 */
@RequestMapping("/a")
@RestController
public class NodeConfigurationController extends BaseSetupWebServiceController {

	private final SetupService setupService;

	/**
	 * Constructor.
	 * 
	 * @param setupService
	 *        the setup service
	 */
	@Autowired
	public NodeConfigurationController(SetupService setupService) {
		super();
		this.setupService = setupService;
	}

	/**
	 * Get the application configuration.
	 * 
	 * @return the result
	 */
	@RequestMapping(value = "/config", method = RequestMethod.GET)
	@ResponseBody
	public Response<NodeAppConfiguration> getAppConfiguration() {
		return response(setupService.getAppConfiguration());
	}

}
