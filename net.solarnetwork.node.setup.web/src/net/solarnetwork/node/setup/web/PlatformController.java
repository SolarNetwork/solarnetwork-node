/* ==================================================================
 * PlatformController.java - 21/11/2017 11:38:23 AM
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

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.node.PlatformService;
import net.solarnetwork.web.domain.Response;

/**
 * Web controller for platform service support.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class PlatformController extends BaseSetupWebServiceController {

	private final PlatformService platformService;

	@Autowired
	public PlatformController(PlatformService platformService) {
		super();
		this.platformService = platformService;
	}

	@RequestMapping(value = "/platform/state", method = RequestMethod.GET)
	public Response<PlatformService.PlatformState> activePlatformState() {
		PlatformService.PlatformState state = platformService.activePlatformState();
		return Response.response(state);
	}

	@RequestMapping(value = "/platform/task", method = RequestMethod.GET)
	public Response<PlatformService.PlatformTaskInfo> activePlatformTaskInfo(Locale locale) {
		PlatformService.PlatformTaskInfo info = platformService.activePlatformTaskInfo(locale);
		return Response.response(info);
	}

}
