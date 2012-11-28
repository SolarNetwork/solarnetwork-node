/* ==================================================================
 * NodeAssociationController.java - Sep 6, 2011 1:34:13 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup.web;

import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.web.support.AssociateNodeCommand;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.util.WebUtils;

/**
 * Controller used to associate a node with a SolarNet account.
 * 
 * @author maxieduncan
 * @version 1.0
 */
@Controller
@SessionAttributes("details")
@RequestMapping("/associate")
public class NodeAssociationController extends BaseSetupController {

	private static final String KEY_DETAILS = "details";

	/**
	 * Request entry point.
	 * 
	 * @param cmd
	 *        the command
	 * @param request
	 *        the request
	 * @return the model and view
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String setupForm(HttpServletRequest request, ModelMap model) {

		AssociateNodeCommand cmd = new AssociateNodeCommand();
		// jump to initial setup
		model.put("command", cmd);

		return "verify-code";
	}

	/**
	 * Decodes the supplied verification code storing the details for the user
	 * to validation.
	 * 
	 * @param command
	 * @param result
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/verify", method = RequestMethod.POST)
	public String verifyCode(@ModelAttribute("command") AssociateNodeCommand command,
			BindingResult result, HttpServletRequest request, ModelMap model) {
		String code = command.getVerificationCode();

		ValidationUtils.rejectIfEmptyOrWhitespace(result, "verificationCode", "field.required");
		if ( result.hasErrors() ) {
			return "verify-code";
		}

		try {
			// Decode the verification code
			NetworkAssociationDetails details = this.getSetupBiz().decodeVerificationCode(code);

			// Check expiration date
			if ( details.getExpiration().getTime() < System.currentTimeMillis() ) {
				result.rejectValue("verificationCode", "verificationCode.expired", null, null);
				return "verify-code";
			}

			try {
				// Retrieve the identity from the server
				this.getSetupBiz().populateServerIdentity(details);
			} catch ( Exception e ) {
				// We have to assume any exception thrown here is caused by the server being down (Runtime exception will be thrown), but there's no guarantee this is the case
				result.reject("node.setup.identity.error", new Object[] { details.getHost() }, null);
				return "verify-code";
			}

			model.put(KEY_DETAILS, details);
			return "verify-identity";
		} catch ( InvalidVerificationCodeException e ) {
			// The verification code appears to be invalid
			result.rejectValue("verificationCode", "verificationCode.invalid", null, null);
			return "verify-code";
		}

	}

	/**
	 * Confirms the node association with the SolarNet server supplied in the
	 * verification code.
	 * 
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/confirm", method = RequestMethod.POST)
	public String confirmIdentity(@ModelAttribute("command") AssociateNodeCommand command,
			Errors errors, HttpServletRequest request, ModelMap model) {
		if ( WebUtils.hasSubmitParameter(request, "cancel") ) {
			// If the operation has been cancelled we go back to the start
			return this.setupForm(request, model);
		}

		NetworkAssociationDetails details = (NetworkAssociationDetails) model.get(KEY_DETAILS);
		try {

			// now that the association has been confirmed get send confirmation to the server
			this.getSetupBiz().acceptSolarNetHost(details);

			return "setup-success";
		} catch ( Exception e ) {
			errors.reject("node.setup.success.error", new Object[] { details.getHost() }, null);
			return "verify-identity";
		}
	}

}
