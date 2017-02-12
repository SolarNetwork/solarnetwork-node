/* ==================================================================
 * HomeController.java - 13/02/2017 10:15:23 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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
import javax.annotation.Resource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.node.SystemService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Controller to manage the initial home screen.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
@ServiceAwareController
@RequestMapping("/a/home")
public class HomeController {

	@Resource(name = "systemService")
	private OptionalService<SystemService> systemService;

	/**
	 * Setup the home view.
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String home(Model model) {
		return "a/home";
	}

	/**
	 * Restart or reboot SolarNode.
	 * 
	 * @param reboot
	 *        If {@code true} then call {@link SystemService#reboot()},
	 *        otherwise call {@link SystemService#exit(boolean)}
	 * @param saveState
	 *        Flag to pass to {@link SystemService#exit(boolean)}
	 * @return A status result.
	 */
	@RequestMapping(value = "/restart", method = RequestMethod.POST)
	@ResponseBody
	public Response<Boolean> restart(
			@RequestParam(name = "reboot", required = false, defaultValue = "false") final boolean reboot,
			@RequestParam(name = "saveState", required = false, defaultValue = "false") final boolean saveState) {
		final SystemService sysService = (systemService != null ? systemService.service() : null);
		if ( sysService == null ) {
			return new Response<Boolean>(false, null, "No service available", Boolean.FALSE);
		} else {
			if ( reboot ) {
				sysService.reboot();
			} else {
				sysService.exit(false);
			}
		}
		return response(Boolean.TRUE);
	}

}
