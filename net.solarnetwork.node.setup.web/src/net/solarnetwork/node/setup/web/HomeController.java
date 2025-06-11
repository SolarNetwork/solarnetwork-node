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

import static net.solarnetwork.domain.Result.success;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller to manage the initial home screen.
 *
 * @author matt
 * @version 3.0
 * @since 1.23
 */
@ServiceAwareController
@RequestMapping("/a/home")
public class HomeController {

	@Resource(name = "systemService")
	private OptionalService<SystemService> systemService;

	/**
	 * Default controller.
	 */
	public HomeController() {
		super();
	}

	/**
	 * Setup the home view.
	 *
	 * @return the view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String home() {
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
	public Result<Boolean> restart(
			@RequestParam(name = "reboot", required = false,
					defaultValue = "false") final boolean reboot,
			@RequestParam(name = "saveState", required = false,
					defaultValue = "false") final boolean saveState) {
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
		return success(Boolean.TRUE);
	}

	/**
	 * Reset SolarNode.
	 *
	 * @param applicationOnly
	 *        the flag to pass to {@link SystemService#reset(boolean)}
	 * @return A status result.
	 * @since 1.1
	 */
	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	@ResponseBody
	public Result<Boolean> reset(@RequestParam(name = "applicationOnly", required = false,
			defaultValue = "false") final boolean applicationOnly) {
		final SystemService sysService = (systemService != null ? systemService.service() : null);
		if ( sysService == null ) {
			return new Response<Boolean>(false, null, "No service available", Boolean.FALSE);
		} else {
			sysService.reset(applicationOnly);
		}
		return success(Boolean.TRUE);
	}

}
