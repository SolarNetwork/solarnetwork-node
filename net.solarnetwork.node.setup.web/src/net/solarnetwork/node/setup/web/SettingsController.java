/* ==================================================================
 * SettingsSharedController.java - Mar 12, 2012 1:31:40 PM
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

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.sort;
import static net.solarnetwork.node.setup.web.WebConstants.setupSessionError;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.service.CsvConfigurableBackupService;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderFactoryMessageComparator;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderMessageComparator;
import net.solarnetwork.node.setup.web.support.IteratorStatus;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.node.setup.web.support.SettingResourceInfo;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.StringNaturalSortComparator;
import net.solarnetwork.web.jakarta.domain.Response;
import net.solarnetwork.web.jakarta.support.MultipartFileResource;

/**
 * Web controller for the settings UI.
 *
 * @author matt
 * @version 3.4
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
	private static final String KEY_SETTING_RESOURCES = "settingResources";
	private static final String KEY_SETTING_RESOURCE_LIST = "settingResourceList";
	private static final String KEY_BACKUP_MANAGER = "backupManager";
	private static final String KEY_BACKUP_SERVICE = "backupService";
	private static final String VIEW_REDIRECT_SETTINGS = "redirect:/a/settings";
	private static final String KEY_CSV_BACKUP_SERVICES = "csvBackupServiceList";

	private static final SearchFilter NOT_DATUM_FILTER = nonnull(
			SearchFilter.forLDAPSearchFilterString("(!(role=datum-filter))"), "NOT_DATUM_FILTER");
	private static final SearchFilter GLOBAL_DATUM_FILTER = nonnull(
			SearchFilter.forLDAPSearchFilterString("(&(role=datum-filter)(role=global))"),
			"GLOBAL_DATUM_FILTER");
	private static final SearchFilter USER_DATUM_FILTER = nonnull(
			SearchFilter.forLDAPSearchFilterString("(&(role=datum-filter)(role=user))"),
			"USER_DATUM_FILTER");

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("settingsService")
	private OptionalService<SettingsService> settingsServiceTracker;

	@Autowired
	@Qualifier("backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

	@Autowired
	@Qualifier("csvBackupServiceList")
	private Collection<CsvConfigurableBackupService> csvBackupServices;

	@Autowired
	private MessageSource messageSource;

	/**
	 * Default constructor.
	 */
	public SettingsController() {
		super();
	}

	/**
	 * Handle a {@link RuntimeException}.
	 *
	 * @param request
	 *        the current HTTP request
	 * @param e
	 *        the exception
	 * @return the view name (redirect to settings)
	 */
	@ExceptionHandler(RuntimeException.class)
	public String handleRuntimeException(HttpServletRequest request, RuntimeException e) {
		log.error("RuntimeException in {} controller", getClass().getSimpleName(), e);
		setupSessionError(request, "error.unexpected",
				e.getMessage() != null ? e.getMessage() : e.toString());
		return VIEW_REDIRECT_SETTINGS;
	}

	/**
	 * List factory settings.
	 *
	 * @param model
	 *        the model
	 * @param locale
	 *        the locale
	 * @return the settings list view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String settingsList(ModelMap model, Locale locale) {
		final SettingsService settingsService = service(settingsServiceTracker);
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
		}
		return "settings/settings-list";
	}

	/**
	 * List service settings.
	 *
	 * @param model
	 *        the model
	 * @param locale
	 *        the locale
	 * @return the settings list view name
	 */
	@RequestMapping(value = "/services", method = RequestMethod.GET)
	public String serviceSettingsList(ModelMap model, Locale locale) {
		final SettingsService settingsService = service(settingsServiceTracker);
		if ( locale == null ) {
			locale = Locale.US;
		}
		if ( settingsService != null ) {
			List<SettingSpecifierProvider> providers = settingsService.getProviders(NOT_DATUM_FILTER);
			if ( providers != null ) {
				sort(providers, new SettingSpecifierProviderMessageComparator(locale));
			}
			model.put(KEY_PROVIDERS, providers);
			model.put(KEY_SETTINGS_SERVICE, settingsService);
		}
		return "settings/services";
	}

	/**
	 * Manage backups.
	 *
	 * @param model
	 *        the model
	 * @param locale
	 *        the locale
	 * @return the settings list view name
	 */
	@RequestMapping(value = "/backups", method = RequestMethod.GET)
	public String backups(ModelMap model, Locale locale) {
		final SettingsService settingsService = service(settingsServiceTracker);
		if ( settingsService != null ) {
			List<SettingSpecifierProvider> providers = settingsService.getProviders(NOT_DATUM_FILTER);
			if ( providers != null ) {
				sort(providers, new SettingSpecifierProviderMessageComparator(locale));
			}
			model.put(KEY_PROVIDERS, providers);
			model.put(KEY_SETTINGS_SERVICE, settingsService);
			model.put(KEY_SETTINGS_BACKUPS, settingsService.getAvailableBackups());
			populateSettingResources(settingsService, providers, model);
		}
		final BackupManager backupManager = backupManagerTracker.service();
		if ( backupManager != null ) {
			model.put(KEY_BACKUP_MANAGER, backupManager);
			BackupService service = backupManager.activeBackupService();
			model.put(KEY_BACKUP_SERVICE, service);
		}
		model.put(KEY_CSV_BACKUP_SERVICES, csvBackupServices);
		return "settings/backups";
	}

	private static final Comparator<SettingResourceInfo> SETTING_RESOURCE_SORT_BY_NAME = (o1, o2) -> {
		return o1.getName().compareTo(o2.getName());
	};

	private void populateSettingResources(SettingsService settingsService,
			List<SettingSpecifierProvider> providers, ModelMap model) {
		Map<String, List<SettingResourceInfo>> info;
		List<SettingResourceInfo> list;
		List<SettingResourceHandler> handlers = settingsService.getSettingResourceHandlers();
		if ( handlers == null || handlers.isEmpty() ) {
			info = Collections.emptyMap();
			list = Collections.emptyList();
		} else {
			info = new TreeMap<>();
			list = new ArrayList<>(handlers.size());
			for ( SettingResourceHandler handler : handlers ) {
				final String handlerKey = handler.getSettingUid();
				Collection<String> supportedKeys = handler.supportedCurrentResourceSettingKeys();
				if ( supportedKeys == null || supportedKeys.isEmpty() ) {
					continue;
				}
				for ( String settingKey : supportedKeys ) {
					String name = displayNameForSettingResource(providers, handlerKey, settingKey);
					info.computeIfAbsent(handlerKey, k -> new ArrayList<SettingResourceInfo>())
							.add(new SettingResourceInfo(name, handlerKey, null, settingKey));
				}
			}
			info.values().stream().forEach(l -> {
				Collections.sort(l, SETTING_RESOURCE_SORT_BY_NAME);
				list.addAll(l);
			});
			Collections.sort(list, SETTING_RESOURCE_SORT_BY_NAME);
		}
		model.put(KEY_SETTING_RESOURCES, info);
		model.put(KEY_SETTING_RESOURCE_LIST, list);
	}

	private String displayNameForSettingResource(List<SettingSpecifierProvider> providers,
			String handlerKey, String settingKey) {
		final Locale locale = Locale.getDefault();
		final MessageSource messageSource = ObjectUtils.nonnull(this.messageSource, "MessageSource");
		String handlerName = null;
		String settingResourceName = null;
		SettingSpecifierProvider p = providers.stream().filter(e -> handlerKey.equals(e.getSettingUid()))
				.findAny().orElse(null);
		MessageSource ms = (p != null ? p.getMessageSource() : null);
		if ( ms != null ) {
			handlerName = ms.getMessage("title", null, (p != null ? p.getDisplayName() : "Unknown"),
					locale);
			settingResourceName = ms.getMessage(format("resource.%s.title", settingKey), null,
					messageSource.getMessage("settings.io.exportResource.unknownName", null, "Unnamed",
							locale),
					locale);
		} else {
			handlerName = (p != null ? p.getDisplayName() : handlerKey);
			settingResourceName = messageSource.getMessage("settings.io.exportResource.unknownName",
					null, "Unnamed", locale);
		}
		return format("%s - %s", handlerName, settingResourceName);
	}

	/**
	 * List filters.
	 *
	 * @param model
	 *        the model
	 * @param locale
	 *        the locale
	 * @return the filters list view name
	 */
	@RequestMapping(value = "/filters", method = RequestMethod.GET)
	public String filterSettingsList(ModelMap model, Locale locale) {
		final SettingsService settingsService = service(settingsServiceTracker);
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
		return "settings/filters-list";
	}

	/**
	 * Manage a specific settings factory.
	 *
	 * @param factoryUid
	 *        the UID of the factory to manage
	 * @param model
	 *        the model
	 * @param req
	 *        the request
	 * @return the manage factory view name
	 */
	@RequestMapping(value = { "/manage", "/filters/manage" }, method = RequestMethod.GET,
			params = "!key")
	public String settingsList(@RequestParam(value = "uid", required = true) String factoryUid,
			ModelMap model, HttpServletRequest req) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service != null ) {
			Map<String, FactorySettingSpecifierProvider> providers = service
					.getProvidersForFactory(factoryUid);
			if ( providers != null && !providers.isEmpty() ) {
				// sort map keys numerically
				String[] instanceIds = providers.keySet().toArray(new String[providers.size()]);
				Arrays.sort(instanceIds, StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
				Map<String, FactorySettingSpecifierProvider> orderedProviders = new LinkedHashMap<>();
				for ( String id : instanceIds ) {
					orderedProviders.put(id, providers.get(id));
				}
				model.put(KEY_PROVIDERS, orderedProviders);
			} else {
				model.put(KEY_PROVIDERS, Collections.emptyMap());
			}
			model.put(KEY_PROVIDER_FACTORY, service.getProviderFactory(factoryUid));
			model.put(KEY_SETTINGS_SERVICE, service);
		}
		return (req.getRequestURI().contains("/filters/") ? "settings/filters-factory-settings-list"
				: "settings/factory-settings-list");
	}

	/**
	 * Manage a specific settings factory.
	 *
	 * @param factoryUid
	 *        the UID of the factory to manage
	 * @param instanceKey
	 *        the factory instance key
	 * @param model
	 *        the model
	 * @param req
	 *        the request
	 * @return the manage factory view name
	 */
	@RequestMapping(value = { "/manage", "/filters/manage" }, method = RequestMethod.GET)
	public String settingsInstance(@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "key", required = true) String instanceKey, ModelMap model,
			HttpServletRequest req) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service != null ) {
			Map<String, FactorySettingSpecifierProvider> providers = service
					.getProvidersForFactory(factoryUid);
			if ( providers != null && providers.containsKey(instanceKey) ) {
				// sort map keys numerically
				String[] instanceIds = providers.keySet().toArray(new String[providers.size()]);
				Arrays.sort(instanceIds, StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
				FactorySettingSpecifierProvider provider = providers.get(instanceKey);
				for ( int i = 0, len = instanceIds.length; i < len; i++ ) {
					if ( instanceKey.equals(instanceIds[i]) ) {
						model.put("instance",
								new SimpleImmutableEntry<String, FactorySettingSpecifierProvider>(
										instanceKey, provider));
						model.put("provider", provider);
						model.put("instanceId", provider.getFactoryInstanceUID());
						model.put("instanceStatus",
								IteratorStatus.status(instanceIds.length, i, provider));
						break;
					}
				}
				model.put(KEY_PROVIDERS, Collections.singletonMap(instanceKey, provider));
			} else {
				model.put(KEY_PROVIDERS, Collections.emptyMap());
			}
			model.put(KEY_PROVIDER_FACTORY, service.getProviderFactory(factoryUid));
			model.put(KEY_SETTINGS_SERVICE, service);
		}
		return "settings/factory-settings-instance";
	}

	/**
	 * Import settings.
	 *
	 * @param file
	 *        the CSV settings resource to import
	 * @return the result view name
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public String importSettings(@RequestParam("file") MultipartFile file) throws IOException {
		final SettingsService service = service(settingsServiceTracker);
		if ( !file.isEmpty() && service != null ) {
			InputStreamReader reader = new InputStreamReader(file.getInputStream(), UTF_8);
			service.importSettingsCSV(reader);
		}
		return VIEW_REDIRECT_SETTINGS;
	}

	/**
	 * Initiate a backup.
	 *
	 * @param model
	 *        the model
	 * @return the result
	 */
	@RequestMapping(value = "/backupNow", method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> initiateBackup(ModelMap model) {
		final BackupManager manager = service(backupManagerTracker);
		boolean result = false;
		if ( manager != null ) {
			manager.createBackup();
			result = true;
		}
		return new Response<Object>(result, null, null, null);
	}

	/**
	 * Import a backup.
	 *
	 * @param file
	 *        the backup to import
	 * @return the settings view name
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/importBackup", method = RequestMethod.POST)
	public String importBackup(@RequestParam("file") MultipartFile file) throws IOException {
		final BackupManager manager = service(backupManagerTracker);
		if ( manager == null ) {
			return "redirect:/a/settings/backups";
		}
		Map<String, String> props = new HashMap<String, String>();
		props.put(BackupManager.BACKUP_KEY, file.getOriginalFilename());
		manager.importBackupArchive(file.getInputStream(), props);
		return "redirect:/a/settings/backups";
	}

	/**
	 * Import a setting resource.
	 *
	 * @param handlerKey
	 *        the {@link SettingResourceHandler} ID to import with
	 * @param instanceKey
	 *        the optional factory instance ID, or {@code null}
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
	public Result<Void> importResource(@RequestParam("handlerKey") String handlerKey,
			@RequestParam(name = "instanceKey", required = false) String instanceKey,
			@RequestParam("key") String key, @RequestPart("file") MultipartFile[] files)
			throws IOException {
		final SettingsService service = service(settingsServiceTracker);
		if ( service == null ) {
			return new Response<Void>(false, null, "SettingsService not available.", null);
		}
		List<Resource> resources = new ArrayList<>(files.length);
		for ( MultipartFile file : files ) {
			MultipartFileResource r = new MultipartFileResource(file);
			resources.add(r);
		}
		try {
			service.importSettingResources(handlerKey, instanceKey, key, resources);
			return Result.success();
		} catch ( RuntimeException e ) {
			String msg = String.format(
					"Error importing settings resource for handler [%s] instance [%s] key [%s]: %s",
					handlerKey, instanceKey, key, e.getMessage());
			log.error(msg, e);
			return Response.error("SET.0001", msg);
		}
	}

	/**
	 * Import a setting resource from direct text.
	 *
	 * @param handlerKey
	 *        the {@link SettingResourceHandler} ID to import with
	 * @param instanceKey
	 *        the optional factory instance ID, or {@code null}
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
		final SettingsService service = service(settingsServiceTracker);
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
			super(data.getBytes(UTF_8));
			this.filename = requireNonNullArgument(filename, "filename");
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}

	/**
	 * Internal settings command to support path clean prefix.
	 *
	 * @since 1.10
	 */
	public static final class CleanSupportSettingsCommand extends SettingsCommand {

		private @Nullable String settingKeyPrefixToClean;

		/**
		 * Constructor.
		 */
		public CleanSupportSettingsCommand() {
			super();
		}

		/**
		 * Get a prefix path to clean during the settings update.
		 *
		 * @return the prefix path, or {@code null}
		 */
		public @Nullable String getSettingKeyPrefixToClean() {
			return settingKeyPrefixToClean;
		}

		/**
		 * Set a prefix path to clean during the settings update.
		 *
		 * @param settingKeyPrefixToClean
		 *        the prefix to clean
		 */
		public void setSettingKeyPrefixToClean(@Nullable String settingKeyPrefixToClean) {
			this.settingKeyPrefixToClean = settingKeyPrefixToClean;
		}

	}

}
