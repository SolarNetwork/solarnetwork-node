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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupInfo;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.PKIService;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.node.setup.UserProfile;
import net.solarnetwork.node.setup.UserService;
import net.solarnetwork.node.setup.web.support.AssociateNodeCommand;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Controller used to associate a node with a SolarNet account.
 * 
 * @author maxieduncan
 * @version 1.3
 */
@Controller
@SessionAttributes({ NodeAssociationController.KEY_DETAILS, NodeAssociationController.KEY_IDENTITY })
@RequestMapping("/associate")
public class NodeAssociationController extends BaseSetupController {

	private static final String BACKUP_KEY_SESSION_KEY = "restoreBackupKey";
	private static final String PAGE_ENTER_CODE = "associate/enter-code";
	private static final String PAGE_IMPORT_FROM_BACKUP = "associate/import-from-backup";
	private static final String PAGE_RESTORE_FROM_BACKUP = "associate/restore-from-backup";

	/** The model attribute for the network association details. */
	public static final String KEY_DETAILS = "details";

	/** The model attribute for the network identity details. */
	public static final String KEY_IDENTITY = "association";

	/** The model attribute for the network identity details. */
	public static final String KEY_NETWORK_URL_MAP = "networkLinks";

	/**
	 * The model attribute for a {@link UserProfile} instance.
	 * 
	 * @since 1.1
	 */
	public static final String KEY_USER = "user";

	private static final String KEY_SETTINGS_SERVICE = "settingsService";
	private static final String KEY_BACKUP_MANAGER = "backupManager";
	private static final String KEY_BACKUP_SERVICE = "backupService";
	private static final String KEY_BACKUPS = "backups";

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private PKIService pkiService;

	@Autowired
	private UserService userService;

	@Resource(name = "authenticationManager")
	private AuthenticationManager authenticationManager;

	@Resource(name = "settingsService")
	private OptionalService<SettingsService> settingsServiceTracker;

	@Resource(name = "backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

	@Resource(name = "networkLinks")
	private Map<String, String> networkURLs = new HashMap<String, String>(4);

	/**
	 * Node association entry point.
	 * 
	 * @param model
	 *        the model
	 * @return the view name
	 */
	@RequestMapping(value = "", method = { RequestMethod.GET, RequestMethod.HEAD })
	public String setupForm(Model model) {
		model.addAttribute("command", new AssociateNodeCommand());
		model.addAttribute(KEY_NETWORK_URL_MAP, networkURLs);
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
			NetworkAssociationDetails details = getSetupBiz()
					.decodeVerificationCode(command.getVerificationCode());
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
			model.addAttribute(KEY_IDENTITY, na);
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
	public String confirmIdentity(@ModelAttribute("command") AssociateNodeCommand command, Errors errors,
			@ModelAttribute(KEY_DETAILS) NetworkAssociationDetails details, Model model) {
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
			} else {
				// generate certificate request
				model.addAttribute("csr", pkiService.generateNodePKCS10CertificateRequestString());
			}
			if ( !userService.someUserExists() ) {
				// create a new user now, using the username from SolarNet and a random password
				UserProfile user = new UserProfile();
				user.setUsername(details.getUsername());
				user.setPassword(KeyGenerators.string().generateKey());
				user.setPasswordAgain(user.getPassword());
				log.debug("Creating initial user {} with password {}", user.getUsername(),
						user.getPassword());
				userService.storeUserProfile(user);

				model.addAttribute(KEY_USER, user);

				// and automatically log in as the new user
				UsernamePasswordAuthenticationToken loginReq = new UsernamePasswordAuthenticationToken(
						user.getUsername(), user.getPassword());
				Authentication auth = authenticationManager.authenticate(loginReq);
				SecurityContextHolder.getContext().setAuthentication(auth);
			}
			return "associate/setup-success";
		} catch ( Exception e ) {
			errors.reject("node.setup.success.error", new Object[] { details.getHost() }, null);
			return PAGE_ENTER_CODE;
		}
	}

	@RequestMapping(value = "/restore", method = RequestMethod.GET)
	public String restoreFromBackup(ModelMap model) {
		final SettingsService settingsService = settingsServiceTracker.service();
		if ( settingsService != null ) {
			model.put(KEY_SETTINGS_SERVICE, settingsService);
		}
		final BackupManager backupManager = backupManagerTracker.service();
		if ( backupManager != null ) {
			model.put(KEY_BACKUP_MANAGER, backupManager);
			BackupService service = backupManager.activeBackupService();
			model.put(KEY_BACKUP_SERVICE, service);
			if ( service != null ) {
				List<Backup> backups = new ArrayList<Backup>(service.getAvailableBackups());
				Collections.sort(backups, new Comparator<Backup>() {

					@Override
					public int compare(Backup o1, Backup o2) {
						// sort in reverse chronological order (newest to oldest)
						return o2.getDate().compareTo(o1.getDate());
					}
				});
				model.put(KEY_BACKUPS, backups);
			}
		}
		return PAGE_IMPORT_FROM_BACKUP;
	}

	@RequestMapping(value = "/configure", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> configure(SettingsCommand cmd) {
		final SettingsService settingsService = settingsServiceTracker.service();
		if ( settingsService != null ) {
			settingsService.updateSettings(cmd);
		}
		return Response.response(null);
	}

	@RequestMapping(value = "/chooseBackup", method = RequestMethod.POST)
	public String chooseBackup(@RequestParam("backup") String key, HttpServletRequest request) {
		final BackupManager backupManager = backupManagerTracker.service();
		BackupService service = backupManager.activeBackupService();
		Backup backup = service.backupForKey(key);
		if ( backup != null ) {
			request.getSession(true).setAttribute(BACKUP_KEY_SESSION_KEY, backup.getKey());
			return PAGE_RESTORE_FROM_BACKUP;
		}
		request.getSession(true).setAttribute("errorMessageKey", "node.setup.restore.error.unknown");
		request.getSession(true).setAttribute("errorMessageParam0", "Backup not imported");
		return "redirect:/associate";
	}

	@RequestMapping(value = "/importBackup", method = RequestMethod.POST)
	public String importBackup(@RequestParam("file") MultipartFile file, HttpServletRequest request)
			throws IOException {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			request.getSession(true).setAttribute("errorMessageKey",
					"node.setup.restore.error.noBackupManager");
			return "redirect:/associate";
		}
		Map<String, String> props = new HashMap<String, String>();
		props.put(BackupManager.BACKUP_KEY, file.getOriginalFilename());
		try {
			Future<Backup> backupFuture = manager.importBackupArchive(file.getInputStream(), props);
			Backup backup = backupFuture.get();
			if ( backup != null ) {
				request.getSession(true).setAttribute(BACKUP_KEY_SESSION_KEY, backup.getKey());
				return PAGE_RESTORE_FROM_BACKUP;
			}
			request.getSession(true).setAttribute("errorMessageKey", "node.setup.restore.error.unknown");
			request.getSession(true).setAttribute("errorMessageParam0", "Backup not imported");
			return "redirect:/associate";
		} catch ( Exception e ) {
			log.error("Exception restoring backup archive", e);
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			request.getSession(true).setAttribute("errorMessageKey", "node.setup.restore.error.unknown");
			request.getSession(true).setAttribute("errorMessageParam0", root.getMessage());
			return "redirect:/associate";
		}

	}

	@RequestMapping(value = "/importedBackup", method = RequestMethod.GET)
	@ResponseBody
	public Response<BackupInfo> importedBackup(Locale locale, HttpServletRequest request)
			throws IOException {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return new Response<BackupInfo>(false, "500", "No backup manager available.", null);
		}
		final String backupKey = (String) request.getSession(true).getAttribute(BACKUP_KEY_SESSION_KEY);
		if ( backupKey == null ) {
			return new Response<BackupInfo>(false, "404", "No imported backup available.", null);
		}
		try {
			BackupInfo info = manager.infoForBackup(backupKey, locale);
			return Response.response(info);
		} catch ( Exception e ) {
			log.error("Exception importing backup archive", e);
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			return new Response<BackupInfo>(false, "500", e.getMessage(), null);
		}
	}

	@RequestMapping(value = "/restoreBackup", method = RequestMethod.POST)
	@ResponseBody
	public Response<?> restoreBackup(BackupOptions options, Locale locale, HttpServletRequest request) {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return new Response<Backup>(false, "500", "No backup manager available.", null);
		}
		final BackupService backupService = manager.activeBackupService();
		if ( backupService == null ) {
			return new Response<Backup>(false, "500", "No backup service available.", null);
		}
		final String backupKey = (String) request.getSession(true).getAttribute(BACKUP_KEY_SESSION_KEY);
		if ( backupKey == null ) {
			return new Response<Object>(false, "404", "No imported backup available.", null);
		}
		Backup backup = manager.activeBackupService().backupForKey(backupKey);
		if ( backup == null ) {
			return new Response<Object>(false, "404", "Imported backup not available.", null);
		}
		Map<String, String> props = options.asBackupManagerProperties();
		manager.restoreBackup(backup, props);
		request.getSession().removeAttribute(BACKUP_KEY_SESSION_KEY);
		shutdownSoon();
		return new Response<Object>(true, null,
				messageSource.getMessage("node.setup.restore.success", null, locale), null);
	}

	public void setPkiService(PKIService pkiService) {
		this.pkiService = pkiService;
	}

	public void setBackupManagerTracker(OptionalService<BackupManager> backupManagerTracker) {
		this.backupManagerTracker = backupManagerTracker;
	}

	public void setNetworkURLs(Map<String, String> networkURLs) {
		this.networkURLs = networkURLs;
	}

}
