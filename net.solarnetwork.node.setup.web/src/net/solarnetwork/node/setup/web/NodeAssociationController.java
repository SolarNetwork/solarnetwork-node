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

import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.web.support.AssociateNodeCommand;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

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

	private static final String PAGE_ENTER_CODE = "associate/enter-code";
	private static final String KEY_DETAILS = "details";

	/**
	 * Node association entry point.
	 * 
	 * @param model
	 *        the model
	 * @return the view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String setupForm(Model model) {
		model.addAttribute("command", new AssociateNodeCommand());
		return PAGE_ENTER_CODE;
	}

	/**
	 * Decode the invitation code, and present the decoded information for the
	 * user to verify.
	 * 
	 * @param command
	 *        the command
	 * @param result
	 *        the binding result
	 * @param model
	 *        the model
	 * @return the view name
	 */
	@RequestMapping(value = "/preview", method = RequestMethod.POST)
	public String previewInvitation(@ModelAttribute("command") AssociateNodeCommand command,
			Errors errors, Model model) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "verificationCode", "field.required");
		if ( errors.hasErrors() ) {
			return PAGE_ENTER_CODE;
		}

		try {
			NetworkAssociationDetails details = getSetupBiz().decodeVerificationCode(
					command.getVerificationCode());
			model.addAttribute(KEY_DETAILS, details);
		} catch ( InvalidVerificationCodeException e ) {
			errors.rejectValue("verificationCode", "verificationCode.invalid", null, null);
			return PAGE_ENTER_CODE;
		}
		return "associate/preview-invitation";
	}

	/**
	 * Decodes the supplied verification code storing the details for the user
	 * to validation.
	 * 
	 * @param details
	 *        the association details
	 * @param result
	 *        a binding result
	 * @param model
	 *        the model
	 * @return the view name
	 */
	@RequestMapping(value = "/verify", method = RequestMethod.POST)
	public String verifyCode(@ModelAttribute(KEY_DETAILS) NetworkAssociationDetails details,
			Errors errors, Model model) {
		// Check expiration date
		if ( details.getExpiration().getTime() < System.currentTimeMillis() ) {
			errors.rejectValue("verificationCode", "verificationCode.expired", null, null);
			return setupForm(model);
		}

		try {
			// Retrieve the identity from the server
			NetworkAssociation na = getSetupBiz().retrieveNetworkAssociation(details);
			model.addAttribute("association", na);
		} catch ( RuntimeException e ) {
			// We are assuming any exception thrown here is caused by the server being down,
			// but there's no guarantee this is the case
			errors.reject("node.setup.identity.error", new Object[] { details.getHost() }, null);
			return setupForm(model);
		}

		return "associate/verify-identity";

	}

	/**
	 * Confirms the node association with the SolarNet server supplied in the
	 * verification code.
	 * 
	 * @param details
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/confirm", method = RequestMethod.POST)
	public String confirmIdentity(@ModelAttribute(KEY_DETAILS) NetworkAssociationDetails details,
			Errors errors, Model model) {
		try {

			// now that the association has been confirmed get send confirmation to the server
			getSetupBiz().acceptSolarNetHost(details);

			return "association/setup-success";
		} catch ( Exception e ) {
			errors.reject("node.setup.success.error", new Object[] { details.getHost() }, null);
			return setupForm(model);
		}
	}

}
