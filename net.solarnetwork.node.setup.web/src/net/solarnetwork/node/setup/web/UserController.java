/* ==================================================================
 * UserController.java - 27/07/2016 11:32:19 AM
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.node.setup.web.support.SettingsUserService;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for user related tasks.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/a/user")
public class UserController extends BaseSetupWebServiceController {

	@Autowired
	private SettingsUserService userService;

	@RequestMapping(value = "/change-password", method = RequestMethod.GET)
	public String changePasswordForm() {
		return "user/change-password";
	}

	/**
	 * Change the active user's password.
	 * 
	 * @param existingPassword
	 *        The existing password.
	 * @param newPassword
	 *        The new password to set.
	 * @param newPasswordAgain
	 *        The new password, repeated.
	 * @return An empty response on success.
	 */
	@RequestMapping(value = "/change-password", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> changePassword(@RequestParam("old") String existingPassword,
			@RequestParam("password") String newPassword,
			@RequestParam("passwordAgain") String newPasswordAgain) {
		userService.changePassword(existingPassword, newPassword, newPasswordAgain);
		return Response.response(null);
	}

}
