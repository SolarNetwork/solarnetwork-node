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

import static java.lang.String.format;
import static java.util.Collections.sort;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.node.setup.web.WebConstants.setupSessionError;
import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.responseOutputStream;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.StringUtils.naturalSortCompare;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceSupport;
import net.solarnetwork.node.domain.SettingNote;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingsBackup;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderFactoryMessageComparator;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderMessageComparator;
import net.solarnetwork.node.setup.web.support.IteratorStatus;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.node.setup.web.support.SettingResourceInfo;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceRegistry;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.settings.SettingSpecifierProviderInfo;
import net.solarnetwork.settings.support.BasicSettingSpecifierProviderInfo;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.StringNaturalSortComparator;
import net.solarnetwork.web.jakarta.domain.Response;
import net.solarnetwork.web.jakarta.support.MultipartFileResource;

/**
 * Web controller for the settings UI.
 *
 * @author matt
 * @version 3.2
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

	private static final SearchFilter NOT_DATUM_FILTER = SearchFilter
			.forLDAPSearchFilterString("(!(role=datum-filter))");
	private static final SearchFilter GLOBAL_DATUM_FILTER = SearchFilter
			.forLDAPSearchFilterString("(&(role=datum-filter)(role=global))");
	private static final SearchFilter USER_DATUM_FILTER = SearchFilter
			.forLDAPSearchFilterString("(&(role=datum-filter)(role=user))");

	private static final String ZIP_ARCHIVE_CONTENT_TYPE = "application/zip";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("settingsService")
	private OptionalService<SettingsService> settingsServiceTracker;

	@Autowired
	@Qualifier("backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

	@Autowired
	private IdentityService identityService;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private ServiceRegistry serviceRegistry;

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
		setupSessionError(request, "error.unexpected", e.getMessage());
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
		String handlerName = null;
		String settingResourceName = null;
		SettingSpecifierProvider p = providers.stream().filter(e -> handlerKey.equals(e.getSettingUid()))
				.findAny().orElse(null);
		MessageSource ms = (p != null ? p.getMessageSource() : null);
		if ( ms != null ) {
			handlerName = ms.getMessage("title", null, p.getDisplayName(), Locale.getDefault());
			settingResourceName = ms.getMessage(format("resource.%s.title", settingKey), null,
					Locale.getDefault());
		} else {
			handlerName = p.getDisplayName();
			settingResourceName = messageSource.getMessage("settings.io.exportResource.unknownName",
					null, "Unnamed resource", Locale.getDefault());
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
	 * Get a list of setting specifier provider infos for a given service
	 * filter.
	 *
	 * @param serviceFilter
	 *        the LDAP search filter of the providers to get info for
	 * @param locale
	 *        the desired locale
	 * @return the result list
	 * @since 2.9
	 */
	@RequestMapping(value = "/providerInfo", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<SettingSpecifierProviderInfo>> providerInfos(
			@RequestParam("filter") String serviceFilter, Locale locale) {
		if ( serviceRegistry == null ) {
			return success();
		}
		final SettingsService service = service(settingsServiceTracker);
		if ( service == null ) {
			return success();
		}
		final List<SettingSpecifierProviderInfo> results = serviceRegistry.services(serviceFilter)
				.stream().filter((p) -> {
					if ( p == null || !(p instanceof Identifiable) ) {
						return false;
					}
					Identifiable ip = (Identifiable) p;
					String uid = ip.getUid();
					if ( uid == null || uid.isEmpty() ) {
						return false;
					}
					return true;
				}).map((p) -> {
					Identifiable ip = (Identifiable) p;
					String uid = ip.getUid();
					String groupUid = ip.getGroupUid();
					if ( p instanceof SettingSpecifierProvider ) {
						return ((SettingSpecifierProvider) p).localizedInfo(locale, uid, groupUid);
					}
					return new BasicSettingSpecifierProviderInfo(null, ip.getDisplayName(), uid,
							groupUid);
				}).sorted((l, r) -> {
					int result = naturalSortCompare(l.getDisplayName(), r.getDisplayName(), true);
					if ( result == 0 ) {
						result = naturalSortCompare(l.getUid(), r.getUid(), true);
					}
					return result;
				}).collect(Collectors.toList());
		return success(results);
	}

	/**
	 * Find available notes for a given key.
	 *
	 * @param key
	 *        the setting key to get notes for
	 * @return the notes
	 * @since 2.12
	 */
	@RequestMapping(value = "/notes", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<SettingNote>> notesForKey(@RequestParam("key") String key) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service == null ) {
			return success();
		}
		List<SettingNote> results = service.notesForKey(key);
		Collections.sort(results, (l, r) -> {
			return naturalSortCompare(l.getType(), r.getType(), false);
		});
		return success(results);
	}

	/**
	 * Find available notes for a given key.
	 *
	 * @param command
	 *        the setting notes to save
	 * @return success result
	 * @since 2.12
	 */
	@RequestMapping(value = "/notes", method = RequestMethod.POST)
	@ResponseBody
	public Result<Void> saveNotes(SettingsCommand command) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service == null ) {
			return success();
		}
		service.saveNotes(command);
		return success();
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
	 * Add a new factory instance.
	 *
	 * @param factoryUid
	 *        the factory to create a new instance for
	 * @param instanceUid
	 *        the instance ID, or {@literal null} to auto-assign one
	 * @return the new instance ID
	 */
	@RequestMapping(value = "/manage/add", method = RequestMethod.POST)
	@ResponseBody
	public Result<String> addConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "name", required = false) String instanceUid) {
		final SettingsService service = service(settingsServiceTracker);
		String result = null;
		if ( service != null ) {
			result = service.addProviderFactoryInstance(factoryUid, instanceUid);
		}
		return success(result);
	}

	/**
	 * Delete a setting.
	 *
	 * @param factoryUid
	 *        the factory UID
	 * @param instanceUid
	 *        the instance UID
	 * @return the result
	 */
	@RequestMapping(value = "/manage/delete", method = RequestMethod.POST)
	@ResponseBody
	public Result<Object> deleteConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "instance", required = true) String instanceUid) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service != null ) {
			service.deleteProviderFactoryInstance(factoryUid, instanceUid);
		}
		return success(null);
	}

	/**
	 * Reset settings to default values.
	 *
	 * @param factoryUid
	 *        the factory UID
	 * @param instanceUid
	 *        the instance UID
	 * @return the result
	 */
	@RequestMapping(value = "/manage/reset", method = RequestMethod.POST)
	@ResponseBody
	public Result<Object> resetConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "instance", required = true) String instanceUid) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service != null ) {
			service.resetProviderFactoryInstance(factoryUid, instanceUid);
		}
		return success(null);
	}

	/**
	 * Remove all settings for a factory.
	 *
	 * @param factoryUid
	 *        the factory UID
	 * @return the result
	 */
	@RequestMapping(value = "/manage/removeall", method = RequestMethod.POST)
	@ResponseBody
	public Result<Object> removeAllConfigurations(
			@RequestParam(value = "uid", required = true) String factoryUid) {
		final SettingsService service = service(settingsServiceTracker);
		if ( service != null ) {
			Map<String, FactorySettingSpecifierProvider> instances = service
					.getProvidersForFactory(factoryUid);
			service.removeProviderFactoryInstances(factoryUid, instances.keySet());
		}
		return success(null);
	}

	/**
	 * Save settings.
	 *
	 * @param command
	 *        the settings to save
	 * @return the result
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Result<Void> saveSettings(CleanSupportSettingsCommand command) {
		final SettingsService service = service(settingsServiceTracker);
		SettingsCommand cmd = command;
		if ( service != null ) {
			if ( command.getSettingKeyPrefixToClean() != null ) {
				String prefix = command.getSettingKeyPrefixToClean();
				try {
					Pattern pat = Pattern.compile(Pattern.quote(prefix) + ".*");
					cmd = new SettingsCommand(command.getValues(), Collections.singleton(pat));
				} catch ( PatternSyntaxException e ) {
					throw new IllegalArgumentException(
							"The settingKeyPrefixToClean expression is invlalid.");
				}
			}
			service.updateSettings(cmd);
		}
		return success(null);
	}

	/**
	 * Export settings to CSV.
	 *
	 * @param backupKey
	 *        the backup key
	 * @param response
	 *        the response
	 * @param acceptEncoding
	 *        the Accept-Encoding header value
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/export", method = RequestMethod.GET)
	@ResponseBody
	public void exportSettings(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response, @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING,
					required = false) final String acceptEncoding)
			throws IOException {
		final SettingsService service = service(settingsServiceTracker);
		final Long nodeId = identityService.getNodeId();
		if ( service != null ) {
			response.setContentType("text/csv;charset=UTF-8");
			response.setHeader("Content-Disposition",
					"attachment; filename=solarnode-settings" + (nodeId == null ? "" : "-" + nodeId)
							+ "_"
							+ (backupKey == null
									? (DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
											.format(ZonedDateTime.now()))
									: backupKey)
							+ ".csv");
			if ( backupKey != null ) {
				Reader r = service.getReaderForBackup(new SettingsBackup(backupKey, null));
				if ( r != null ) {
					try (Writer out = new OutputStreamWriter(
							responseOutputStream(response, acceptEncoding), StandardCharsets.UTF_8)) {
						FileCopyUtils.copy(r, out);
					}
				}
			} else {
				try (Writer out = new OutputStreamWriter(responseOutputStream(response, acceptEncoding),
						StandardCharsets.UTF_8)) {
					service.exportSettingsCSV(out);
				}
			}
		}
	}

	/**
	 * Export settings to CSV.
	 *
	 * @param factoryUid
	 *        the UID of the factory to export the settings of
	 * @param response
	 *        the response
	 * @param locale
	 *        the desired locale
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/exportFactory", method = RequestMethod.GET)
	@ResponseBody
	public void exportFactorySettings(@RequestParam("uid") String factoryUid,
			HttpServletResponse response, Locale locale) throws IOException {
		final SettingsService service = service(settingsServiceTracker);
		final Long nodeId = identityService.getNodeId();
		if ( service != null ) {
			SettingSpecifierProviderFactory factory = service.getProviderFactory(factoryUid);
			String name = factoryUid;
			if ( factory != null ) {
				String desc = null;
				if ( factory.getMessageSource() != null ) {
					desc = factory.getMessageSource().getMessage("title", null, null, locale);
				}
				if ( desc == null ) {
					desc = factory.getDisplayName();
				}
				if ( desc != null ) {
					name = desc;
				}
			}
			response.setContentType("text/csv;charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=solarnode-settings"
					+ (nodeId == null ? "" : "-" + nodeId) + "_" + name + ".csv");

			SettingsCommand filter = new SettingsCommand();
			filter.setProviderKey(factoryUid);
			service.exportSettingsCSV(filter, response.getWriter());
		}
	}

	/**
	 * Export settings resources.
	 *
	 * @param handlerKey
	 *        the {@link SettingResourceHandler} ID to import with
	 * @param instanceKey
	 *        the optional factory instance ID, or {@literal null}
	 * @param key
	 *        the resource setting key
	 * @param response
	 *        the HTTP response
	 * @param acceptEncoding
	 *        the Accept-Encoding header value
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.1
	 */
	@RequestMapping(value = "/exportResources", method = RequestMethod.GET)
	@ResponseBody
	public void exportSettingsResources(@RequestParam("handlerKey") String handlerKey,
			@RequestParam(name = "instanceKey", required = false) String instanceKey,
			@RequestParam("key") String key, HttpServletResponse response,
			@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING,
					required = false) final String acceptEncoding)
			throws IOException {
		final SettingsService service = service(settingsServiceTracker);
		if ( service != null ) {
			SettingResourceHandler handler = service.getSettingResourceHandler(handlerKey, instanceKey);
			if ( handler != null ) {
				Iterable<Resource> rItr = handler.currentSettingResources(key);
				if ( rItr != null ) {
					List<Resource> rList = StreamSupport.stream(rItr.spliterator(), false)
							.collect(Collectors.toList());
					if ( !rList.isEmpty() ) {
						int idx = 1;
						if ( rList.size() == 1 ) {
							// return resource directly
							Resource r = rList.get(0);
							String rName = filenameForSettingsResource(r, key, idx);
							String contentType = contentTypeForSettingsResource(r, rName);
							response.setContentType(contentType);
							response.setHeader("Content-Disposition",
									format("attachment; filename=%s", rName));
							try (OutputStream out = (contentType.startsWith("text")
									? responseOutputStream(response, acceptEncoding)
									: response.getOutputStream())) {
								FileCopyUtils.copy(r.getInputStream(), out);
							}
						} else {
							// zip up into a single archive
							response.setContentType(ZIP_ARCHIVE_CONTENT_TYPE);
							try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
								for ( Resource r : rList ) {
									String rName = filenameForSettingsResource(r, key, idx);
									zos.putNextEntry(new ZipEntry(rName));
									StreamUtils.copy(r.getInputStream(), zos);
									idx++;
								}
							}
						}
						return;
					}
				}
			}
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	private String contentTypeForSettingsResource(Resource r, String rName) {
		int idx = rName.lastIndexOf('.');
		String ext = null;
		if ( idx > 0 ) {
			ext = rName.substring(idx + 1).toLowerCase();
		}
		if ( ext != null ) {
			switch (ext) {
				case "csv":
					return "text/csv; charset=utf-8";

				case "txt":
					return MediaType.TEXT_PLAIN + "; charset=utf-8";

				case "xml":
					return MediaType.TEXT_XML_VALUE;

				default:
					// don't know
			}
		}
		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	private static String filenameForSettingsResource(Resource r, String key, int idx) {
		String name = r.getFilename();
		if ( name == null ) {
			name = r.getDescription();
			if ( name == null ) {
				name = key + "-" + idx;
			}
		}
		return name;
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
			InputStreamReader reader = new InputStreamReader(file.getInputStream(), "UTF-8");
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

	/**
	 * Export a backup.
	 *
	 * @param backupKey
	 *        the backup to export
	 * @param response
	 *        the response
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/exportBackup", method = RequestMethod.GET)
	@ResponseBody
	public void exportBackup(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response) throws IOException {
		final BackupManager manager = service(backupManagerTracker);
		if ( manager == null ) {
			return;
		}

		final String exportFileName = backupExportFileNameForBackupKey(backupKey);

		// create the zip archive for the backup files
		response.setContentType(ZIP_ARCHIVE_CONTENT_TYPE);
		response.setHeader("Content-Disposition", "attachment; filename=" + exportFileName);
		manager.exportBackupArchive(backupKey, response.getOutputStream());
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
			super(data.getBytes(Charset.forName("UTF-8")));
			this.filename = filename;
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

		private String settingKeyPrefixToClean;

		/**
		 * Constructor.
		 */
		public CleanSupportSettingsCommand() {
			super();
		}

		/**
		 * Get a prefix path to clean during the settings update.
		 *
		 * @return the prefix path, or {@literal null}
		 */
		public String getSettingKeyPrefixToClean() {
			return settingKeyPrefixToClean;
		}

		/**
		 * Set a prefix path to clean during the settings update.
		 *
		 * @param settingKeyPrefixToClean
		 *        the prefix to clean
		 */
		public void setSettingKeyPrefixToClean(String settingKeyPrefixToClean) {
			this.settingKeyPrefixToClean = settingKeyPrefixToClean;
		}

	}

}
