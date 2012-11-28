/* ==================================================================
 * SettingsController.java - Mar 12, 2012 1:31:40 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
import java.io.InputStreamReader;
import java.io.Reader;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.node.settings.SettingsBackup;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.util.OptionalServiceTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * Web controller for the settings UI.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

	private static final String KEY_PROVIDERS = "providers";
	private static final String KEY_PROVIDER_FACTORY = "factory";
	private static final String KEY_PROVIDER_FACTORIES = "factories";
	private static final String KEY_SETTINGS_SERVICE = "settingsService";
	private static final String KEY_SETTINGS_BACKUPS = "settingsBackups";

	@Autowired
	@Qualifier("settingsService")
	private OptionalServiceTracker<SettingsService> settingsService;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String settingsList(ModelMap model) {
		if ( settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			model.put(KEY_PROVIDERS, service.getProviders());
			model.put(KEY_PROVIDER_FACTORIES, service.getProviderFactories());
			model.put(KEY_SETTINGS_SERVICE, service);
			model.put(KEY_SETTINGS_BACKUPS, service.getAvailableBackups());
		}
		return "settings-list";
	}

	@RequestMapping(value = "/manage", method = RequestMethod.GET)
	public String settingsList(@RequestParam(value = "uid", required = true) String factoryUID,
			ModelMap model) {
		if ( settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			model.put(KEY_PROVIDERS, service.getProvidersForFactory(factoryUID));
			model.put(KEY_PROVIDER_FACTORY, service.getProviderFactory(factoryUID));
			model.put(KEY_SETTINGS_SERVICE, service);
		}
		return "factory-settings-list";
	}

	@RequestMapping(value = "/manage/add", method = RequestMethod.POST)
	public String addConfiguration(@RequestParam(value = "uid", required = true) String factoryUID,
			ModelMap model) {
		if ( settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			String result = service.addProviderFactoryInstance(factoryUID);
			model.put("result", result);
		}
		model.put("success", Boolean.TRUE);
		return "json";
	}

	@RequestMapping(value = "/manage/delete", method = RequestMethod.POST)
	public String deleteConfiguration(@RequestParam(value = "uid", required = true) String factoryUID,
			@RequestParam(value = "instance", required = true) String instanceUID, ModelMap model) {
		if ( settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			service.deleteProviderFactoryInstance(factoryUID, instanceUID);
		}
		model.put("success", Boolean.TRUE);
		return "json";
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveSettings(SettingsCommand command, ModelMap model) {
		if ( settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			service.updateSettings(command);
		}
		model.put("success", Boolean.TRUE);
		return "json";
	}

	@RequestMapping(value = "/export", method = RequestMethod.GET)
	@ResponseBody
	public void exportSettings(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response) throws IOException {
		if ( settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			response.setContentType(MediaType.TEXT_PLAIN.toString());
			response.setHeader("Content-Disposition", "attachment; filename=settings"
					+ (backupKey == null ? "" : "_" + backupKey) + ".txt");
			if ( backupKey != null ) {
				Reader r = service.getReaderForBackup(new SettingsBackup(backupKey, null));
				if ( r != null ) {
					FileCopyUtils.copy(r, response.getWriter());
				}
			} else {
				service.exportSettingsCSV(response.getWriter());
			}
		}
	}

	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public String importSettigns(@RequestParam("file") MultipartFile file) throws IOException {
		if ( !file.isEmpty() && settingsService.isAvailable() ) {
			SettingsService service = settingsService.getService();
			InputStreamReader reader = new InputStreamReader(file.getInputStream(), "UTF-8");
			service.importSettingsCSV(reader);
		}
		return "redirect:/settings.do";
	}
}
