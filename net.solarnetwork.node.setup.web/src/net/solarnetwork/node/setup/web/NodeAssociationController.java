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

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.PKIService;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.node.setup.web.support.AssociateNodeCommand;
import net.solarnetwork.util.OptionalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller used to associate a node with a SolarNet account.
 * 
 * @author maxieduncan
 * @version 1.0
 */
@Controller
@SessionAttributes(NodeAssociationController.KEY_DETAILS)
@RequestMapping("/associate")
public class NodeAssociationController extends BaseSetupController {

	private static final String PAGE_ENTER_CODE = "associate/enter-code";
	private static final String PAGE_RESTORE_FROM_BACKUP = "associate/restore-from-backup";

	/** The model attribute for the network association details. */
	public static final String KEY_DETAILS = "details";

	@Autowired
	private PKIService pkiService;

	@Autowired
	@Qualifier("backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

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
	 * @param errors
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
	 * @param command
	 *        the associate comment, used only for reporting errors
	 * @param errors
	 *        the errors associated with the command
	 * @param details
	 *        the session details objects
	 * @param model
	 *        the view model
	 * @return the view name
	 */
	@RequestMapping(value = "/verify", method = RequestMethod.POST)
	public String verifyCode(@ModelAttribute("command") AssociateNodeCommand command, Errors errors,
			@ModelAttribute(KEY_DETAILS) NetworkAssociationDetails details, Model model) {
		// Check expiration date
		if ( details.getExpiration().getTime() < System.currentTimeMillis() ) {
			errors.rejectValue("verificationCode", "verificationCode.expired", null, null);
			return PAGE_ENTER_CODE;
		}

		try {
			// Retrieve the identity from the server
			NetworkAssociation na = getSetupBiz().retrieveNetworkAssociation(details);
			model.addAttribute("association", na);
		} catch ( SetupException e ) {
			errors.reject("node.setup.identity.error", new Object[] { details.getHost() }, null);
			return setupForm(model);
		} catch ( RuntimeException e ) {
			log.error("Unexpected exception processing /setup/verify", e);
			// We are assuming any exception thrown here is caused by the server being down,
			// but there's no guarantee this is the case
			errors.reject("node.setup.identity.error", new Object[] { details.getHost() }, null);
			return PAGE_ENTER_CODE;
		}

		return "associate/verify-identity";
	}

	/**
	 * Confirms the node association with the SolarNet server supplied in the
	 * verification code.
	 * 
	 * @param command
	 *        the associate comment, used only for reporting errors
	 * @param errors
	 *        the errors associated with the command
	 * @param details
	 *        the session details objects
	 * @param model
	 *        the view model
	 * @return the view name
	 */
	@RequestMapping(value = "/confirm", method = RequestMethod.POST)
	public String confirmIdentity(@ModelAttribute("command") AssociateNodeCommand command,
			Errors errors, @ModelAttribute(KEY_DETAILS) NetworkAssociationDetails details, Model model) {
		try {

			// now that the association has been confirmed get send confirmation to the server
			NetworkAssociationDetails req = new NetworkAssociationDetails(details);
			req.setUsername(details.getUsername());
			req.setKeystorePassword(command.getKeystorePassword());
			NetworkCertificate cert = getSetupBiz().acceptNetworkAssociation(req);
			details.setNetworkId(cert.getNetworkId());

			if ( cert.getNetworkCertificateStatus() != null ) {
				details.setNetworkCertificateStatus(cert.getNetworkCertificateStatus());
				details.setNetworkCertificateSubjectDN(cert.getNetworkCertificateSubjectDN());
				details.setNetworkCertificate(cert.getNetworkCertificate());
				pkiService.savePKCS12Keystore(cert.getNetworkCertificate(),
						command.getKeystorePassword());
			} else {
				// generate certificate request
				model.addAttribute("csr", pkiService.generateNodePKCS10CertificateRequestString());
			}

			return "associate/setup-success";
		} catch ( Exception e ) {
			errors.reject("node.setup.success.error", new Object[] { details.getHost() }, null);
			return PAGE_ENTER_CODE;
		}
	}

	@RequestMapping(value = "/restore", method = RequestMethod.GET)
	public String restoreFromBackup() {
		return PAGE_RESTORE_FROM_BACKUP;
	}

	@RequestMapping(value = "/importBackup", method = RequestMethod.POST)
	public String importBackup(@RequestParam("file") MultipartFile file, HttpServletRequest request)
			throws IOException {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			request.getSession(true).setAttribute("errorMessageKey",
					"node.setup.restore.error.noBackupManager");
		} else {
			try {
				manager.importBackupArchive(file.getInputStream());
				request.getSession(true).setAttribute("statusMessageKey", "node.setup.restore.success");
			} catch ( Exception e ) {
				log.error("Exception restoring backup archive", e);
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				request.getSession(true).setAttribute("errorMessageKey",
						"node.setup.restore.error.unknown");
				request.getSession(true).setAttribute("errorMessageParam0", root.getMessage());
			}
		}
		return "redirect:/associate";
	}

	public void setPkiService(PKIService pkiService) {
		this.pkiService = pkiService;
	}

}
