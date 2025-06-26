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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.node.setup.UserProfile;
import net.solarnetwork.node.setup.UserService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller for user related tasks.
 * 
 * @author matt
 * @version 1.1
 */
@ServiceAwareController
@RequestMapping("/a/user")
public class UserController extends BaseSetupWebServiceController {

	@Autowired
	private UserService userService;

	/**
	 * Default constructor.
	 */
	public UserController() {
		super();
	}

	/**
	 * Render the change password form.
	 * 
	 * @param oldPassword
	 *        An optional old password to pre-populate in the form.
	 * @param model
	 *        The model object.
	 * @return The view name to render.
	 */
	@RequestMapping(value = "/change-password", method = RequestMethod.GET)
	public String changePasswordForm(@RequestParam(value = "old", required = false) String oldPassword,
			Model model) {
		UserProfile user = new UserProfile();
		if ( oldPassword != null ) {
			user.setOldPassword(oldPassword);
		}
		model.addAttribute("user", user);
		return "user/change-password";
	}

	/**
	 * Change the active user's password.
	 * 
	 * @param userProfile
	 *        the user profile
	 * @return An empty response on success.
	 */
	@RequestMapping(value = "/change-password", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> changePassword(UserProfile userProfile) {
		userService.changePassword(userProfile.getOldPassword(), userProfile.getPassword(),
				userProfile.getPasswordAgain());
		return Response.response(null);
	}

	/**
	 * Render the change username form.
	 * 
	 * @return The view name to render.
	 */
	@RequestMapping(value = "/change-username", method = RequestMethod.GET)
	public String changeUsernameForm() {
		return "user/change-username";
	}

	/**
	 * Change the active user's username.
	 * 
	 * @param username
	 *        the new username
	 * @param usernameAgain
	 *        the username again
	 * @return An empty response on success.
	 */
	@RequestMapping(value = "/change-username", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> changeUsername(@RequestParam("username") String username,
			@RequestParam("usernameAgain") String usernameAgain) {
		userService.changeUsername(username, usernameAgain);
		return Response.response(null);
	}

}
