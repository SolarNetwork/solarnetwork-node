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
 */

package net.solarnetwork.node.setup.web;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceIterable;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.settings.SettingsBackup;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.util.OptionalService;
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
 * @version 1.1
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

	private static final String KEY_PROVIDERS = "providers";
	private static final String KEY_PROVIDER_FACTORY = "factory";
	private static final String KEY_PROVIDER_FACTORIES = "factories";
	private static final String KEY_SETTINGS_SERVICE = "settingsService";
	private static final String KEY_SETTINGS_BACKUPS = "settingsBackups";
	private static final String KEY_BACKUP_MANAGER = "backupManager";
	private static final String KEY_BACKUP_SERVICE = "backupService";
	private static final String KEY_BACKUPS = "backups";

	@Autowired
	@Qualifier("settingsService")
	private OptionalService<SettingsService> settingsServiceTracker;

	@Autowired
	@Qualifier("backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String settingsList(ModelMap model) {
		final SettingsService settingsService = settingsServiceTracker.service();
		if ( settingsService != null ) {
			model.put(KEY_PROVIDERS, settingsService.getProviders());
			model.put(KEY_PROVIDER_FACTORIES, settingsService.getProviderFactories());
			model.put(KEY_SETTINGS_SERVICE, settingsService);
			model.put(KEY_SETTINGS_BACKUPS, settingsService.getAvailableBackups());
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
		return "settings-list";
	}

	@RequestMapping(value = "/manage", method = RequestMethod.GET)
	public String settingsList(@RequestParam(value = "uid", required = true) String factoryUID,
			ModelMap model) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			model.put(KEY_PROVIDERS, service.getProvidersForFactory(factoryUID));
			model.put(KEY_PROVIDER_FACTORY, service.getProviderFactory(factoryUID));
			model.put(KEY_SETTINGS_SERVICE, service);
		}
		return "factory-settings-list";
	}

	@RequestMapping(value = "/manage/add", method = RequestMethod.POST)
	public String addConfiguration(@RequestParam(value = "uid", required = true) String factoryUID,
			ModelMap model) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			String result = service.addProviderFactoryInstance(factoryUID);
			model.put("result", result);
		}
		model.put("success", Boolean.TRUE);
		return "json";
	}

	@RequestMapping(value = "/manage/delete", method = RequestMethod.POST)
	public String deleteConfiguration(@RequestParam(value = "uid", required = true) String factoryUID,
			@RequestParam(value = "instance", required = true) String instanceUID, ModelMap model) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			service.deleteProviderFactoryInstance(factoryUID, instanceUID);
		}
		model.put("success", Boolean.TRUE);
		return "json";
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveSettings(SettingsCommand command, ModelMap model) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			service.updateSettings(command);
		}
		model.put("success", Boolean.TRUE);
		return "json";
	}

	@RequestMapping(value = "/export", method = RequestMethod.GET)
	@ResponseBody
	public void exportSettings(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response) throws IOException {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
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
	public String importSettings(@RequestParam("file") MultipartFile file) throws IOException {
		final SettingsService service = settingsServiceTracker.service();
		if ( !file.isEmpty() && service != null ) {
			InputStreamReader reader = new InputStreamReader(file.getInputStream(), "UTF-8");
			service.importSettingsCSV(reader);
		}
		return "redirect:/settings.do";
	}

	@RequestMapping(value = "/backupNow", method = RequestMethod.POST)
	public String initiateBackup(ModelMap model) {
		final BackupManager manager = backupManagerTracker.service();
		boolean result = false;
		if ( manager != null ) {
			manager.createBackup();
			result = true;
		}
		model.put("success", result);
		return "json";

	}

	@RequestMapping(value = "/exportBackup", method = RequestMethod.GET)
	@ResponseBody
	public void exportBackup(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response) throws IOException {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return;
		}
		final BackupService service = manager.activeBackupService();
		if ( service == null ) {
			return;
		}

		final Backup backup = service.backupForKey(backupKey);
		if ( backup == null ) {
			return;
		}

		// create the zip archive for the backup files
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=node-backup"
				+ (backupKey == null ? "" : "_" + backupKey) + ".zip");
		ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
		try {
			BackupResourceIterable resources = service.getBackupResources(backup);
			for ( BackupResource r : resources ) {
				zos.putNextEntry(new ZipEntry(r.getBackupPath()));
				FileCopyUtils.copy(r.getInputStream(), new FilterOutputStream(zos) {

					@Override
					public void close() throws IOException {
						// FileCopyUtils closed the stream, which we don't want here
					}

				});
			}
			resources.close();
		} finally {
			zos.flush();
			zos.finish();
			zos.close();
		}
	}

}
