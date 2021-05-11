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

import static java.util.Collections.sort;
import static net.solarnetwork.web.domain.Response.response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceSupport;
import net.solarnetwork.node.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.SettingsBackup;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.FactoryInstanceIdComparator;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderFactoryMessageComparator;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderMessageComparator;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.node.setup.web.support.SortByNodeAndDate;
import net.solarnetwork.support.SearchFilter;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;
import net.solarnetwork.web.support.MultipartFileResource;

/**
 * Web controller for the settings UI.
 * 
 * @author matt
 * @version 1.9
 */
@ServiceAwareController
@RequestMapping("/a/settings")
public class SettingsController {

	private static final String KEY_PROVIDERS = "providers";
	private static final String KEY_PROVIDER_FACTORY = "factory";
	private static final String KEY_PROVIDER_FACTORIES = "factories";
	private static final String KEY_GLOBAL_PROVIDER_FACTORIES = "globalFactories";
	private static final String KEY_USER_PROVIDER_FACTORIES = "userFactories";
	private static final String KEY_SETTINGS_SERVICE = "settingsService";
	private static final String KEY_SETTINGS_BACKUPS = "settingsBackups";
	private static final String KEY_BACKUP_MANAGER = "backupManager";
	private static final String KEY_BACKUP_SERVICE = "backupService";
	private static final String KEY_BACKUPS = "backups";

	private static final SearchFilter NOT_DATUM_FILTER = SearchFilter
			.forLDAPSearchFilterString("(!(role=datum-filter))");
	private static final SearchFilter GLOBAL_DATUM_FILTER = SearchFilter
			.forLDAPSearchFilterString("(&(role=datum-filter)(role=global))");
	private static final SearchFilter USER_DATUM_FILTER = SearchFilter
			.forLDAPSearchFilterString("(&(role=datum-filter)(role=user))");

	@Autowired
	@Qualifier("settingsService")
	private OptionalService<SettingsService> settingsServiceTracker;

	@Autowired
	@Qualifier("backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

	@Autowired
	private IdentityService identityService;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String settingsList(ModelMap model, Locale locale) {
		final SettingsService settingsService = settingsServiceTracker.service();
		if ( locale == null ) {
			locale = Locale.US;
		}
		if ( settingsService != null ) {
			List<SettingSpecifierProviderFactory> factories = settingsService
					.getProviderFactories(NOT_DATUM_FILTER);
			if ( factories != null ) {
				sort(factories, new SettingSpecifierProviderFactoryMessageComparator(locale));
			}
			List<SettingSpecifierProvider> providers = settingsService.getProviders(NOT_DATUM_FILTER);
			if ( providers != null ) {
				sort(providers, new SettingSpecifierProviderMessageComparator(locale));
			}
			model.put(KEY_PROVIDERS, providers);
			model.put(KEY_PROVIDER_FACTORIES, factories);
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
				Collections.sort(backups, SortByNodeAndDate.DEFAULT);
				model.put(KEY_BACKUPS, backups);
			}
		}
		return "settings-list";
	}

	@RequestMapping(value = "/filters", method = RequestMethod.GET)
	public String filterSettingsList(ModelMap model, Locale locale) {
		final SettingsService settingsService = settingsServiceTracker.service();
		if ( locale == null ) {
			locale = Locale.US;
		}
		if ( settingsService != null ) {
			List<SettingSpecifierProviderFactory> globalFactories = settingsService
					.getProviderFactories(GLOBAL_DATUM_FILTER);
			if ( globalFactories != null ) {
				sort(globalFactories, new SettingSpecifierProviderFactoryMessageComparator(locale));
			}
			List<SettingSpecifierProviderFactory> userFactories = settingsService
					.getProviderFactories(USER_DATUM_FILTER);
			if ( userFactories != null ) {
				sort(userFactories, new SettingSpecifierProviderFactoryMessageComparator(locale));
			}
			List<SettingSpecifierProvider> providers = settingsService.getProviders(GLOBAL_DATUM_FILTER);
			if ( providers != null ) {
				sort(providers, new SettingSpecifierProviderMessageComparator(locale));
			}
			model.put(KEY_PROVIDERS, providers);
			model.put(KEY_GLOBAL_PROVIDER_FACTORIES, globalFactories);
			model.put(KEY_USER_PROVIDER_FACTORIES, userFactories);
			model.put(KEY_SETTINGS_SERVICE, settingsService);
		}
		return "filters-list";
	}

	@RequestMapping(value = { "/manage", "/filters/manage" }, method = RequestMethod.GET)
	public String settingsList(@RequestParam(value = "uid", required = true) String factoryUID,
			ModelMap model, HttpServletRequest req) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			Map<String, FactorySettingSpecifierProvider> providers = service
					.getProvidersForFactory(factoryUID);
			if ( providers != null && !providers.isEmpty() ) {
				// sort map keys numerically
				String[] instanceIds = providers.keySet().toArray(new String[providers.size()]);
				Arrays.sort(instanceIds, FactoryInstanceIdComparator.INSTANCE);
				Map<String, FactorySettingSpecifierProvider> orderedProviders = new LinkedHashMap<>();
				for ( String id : instanceIds ) {
					orderedProviders.put(id, providers.get(id));
				}
				model.put(KEY_PROVIDERS, orderedProviders);
			} else {
				model.put(KEY_PROVIDERS, Collections.emptyMap());
			}
			model.put(KEY_PROVIDER_FACTORY, service.getProviderFactory(factoryUID));
			model.put(KEY_SETTINGS_SERVICE, service);
		}
		return (req.getRequestURI().contains("/filters/") ? "filters-factory-settings-list"
				: "factory-settings-list");
	}

	@RequestMapping(value = "/manage/add", method = RequestMethod.POST)
	@ResponseBody
	public Response<String> addConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUID) {
		final SettingsService service = settingsServiceTracker.service();
		String result = null;
		if ( service != null ) {
			result = service.addProviderFactoryInstance(factoryUID);
		}
		return response(result);
	}

	@RequestMapping(value = "/manage/delete", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> deleteConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUID,
			@RequestParam(value = "instance", required = true) String instanceUID) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			service.deleteProviderFactoryInstance(factoryUID, instanceUID);
		}
		return response(null);
	}

	@RequestMapping(value = "/manage/reset", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> resetConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUID,
			@RequestParam(value = "instance", required = true) String instanceUID) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			service.resetProviderFactoryInstance(factoryUID, instanceUID);
		}
		return response(null);
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> saveSettings(SettingsCommand command, ModelMap model) {
		final SettingsService service = settingsServiceTracker.service();
		if ( service != null ) {
			service.updateSettings(command);
		}
		return response(null);
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
		return "redirect:/a/settings";
	}

	@RequestMapping(value = "/backupNow", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> initiateBackup(ModelMap model) {
		final BackupManager manager = backupManagerTracker.service();
		boolean result = false;
		if ( manager != null ) {
			manager.createBackup();
			result = true;
		}
		return new Response<Object>(result, null, null, null);
	}

	private String backupExportFileNameForBackupKey(String backupKey) {
		// look if already has node ID + date in key
		Matcher m = BackupServiceSupport.NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(backupKey);
		String nodeId = null;
		String key = null;
		if ( m.find() ) {
			nodeId = m.group(1);
			key = m.group(2);
		} else {
			Long id = (identityService != null ? identityService.getNodeId() : null);
			nodeId = (id != null ? id.toString() : null);
			key = backupKey;
		}
		return "node-" + (nodeId != null ? nodeId : "UNKNOWN") + "-backup"
				+ (key == null ? "" : "-" + key) + ".zip";
	}

	@RequestMapping(value = "/exportBackup", method = RequestMethod.GET)
	@ResponseBody
	public void exportBackup(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response) throws IOException {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return;
		}

		final String exportFileName = backupExportFileNameForBackupKey(backupKey);

		// create the zip archive for the backup files
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=" + exportFileName);
		manager.exportBackupArchive(backupKey, response.getOutputStream());
	}

	@RequestMapping(value = "/importBackup", method = RequestMethod.POST)
	public String importBackup(@RequestParam("file") MultipartFile file) throws IOException {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return "redirect:/a/settings";
		}
		Map<String, String> props = new HashMap<String, String>();
		props.put(BackupManager.BACKUP_KEY, file.getName());
		manager.importBackupArchive(file.getInputStream(), props);
		return "redirect:/a/settings";
	}

	/**
	 * Import a setting resource.
	 * 
	 * @param handlerKey
	 *        the {@link SettingResourceHandler} ID to import with
	 * @param instanceKey
	 *        the optional factory instance ID, or {@literal null}
	 * @param key
	 *        the resource setting key
	 * @param files
	 *        the resources
	 * @return status response
	 * @throws IOException
	 *         if any IO error occurs
	 */
	@RequestMapping(value = "/importResource", method = RequestMethod.POST, params = "!data")
	@ResponseBody
	public Response<Void> importResource(@RequestParam("handlerKey") String handlerKey,
			@RequestParam(name = "instanceKey", required = false) String instanceKey,
			@RequestParam("key") String key, @RequestPart("file") MultipartFile[] files)
			throws IOException {
		final SettingsService service = settingsServiceTracker.service();
		if ( service == null ) {
			return new Response<Void>(false, null, "SettingsService not available.", null);
		}
		List<Resource> resources = new ArrayList<>(files.length);
		for ( MultipartFile file : files ) {
			MultipartFileResource r = new MultipartFileResource(file);
			resources.add(r);
		}
		service.importSettingResources(handlerKey, instanceKey, key, resources);
		return Response.response(null);
	}

	/**
	 * Import a setting resource from direct text.
	 * 
	 * @param handlerKey
	 *        the {@link SettingResourceHandler} ID to import with
	 * @param instanceKey
	 *        the optional factory instance ID, or {@literal null}
	 * @param key
	 *        the resource setting key
	 * @param data
	 *        the resource data
	 * @return status response
	 * @throws IOException
	 *         if any IO error occurs
	 */
	@RequestMapping(value = "/importResource", method = RequestMethod.POST, params = "data")
	@ResponseBody
	public Response<Void> importResourceData(@RequestParam("handlerKey") String handlerKey,
			@RequestParam(name = "instanceKey", required = false) String instanceKey,
			@RequestParam("key") String key, @RequestParam("data") String data) throws IOException {
		final SettingsService service = settingsServiceTracker.service();
		if ( service == null ) {
			return new Response<Void>(false, null, "SettingsService not available.", null);
		}
		NamedDataResource r = new NamedDataResource(key + "-01.txt", data);
		service.importSettingResources(handlerKey, instanceKey, key, Collections.singleton(r));
		return Response.response(null);
	}

	private static final class NamedDataResource extends ByteArrayResource {

		private final String filename;

		private NamedDataResource(String filename, String data) {
			super(data.getBytes(Charset.forName("UTF-8")));
			this.filename = filename;
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}

}
