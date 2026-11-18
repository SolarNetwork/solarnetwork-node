/* ==================================================================
 * SettingsSharedController.java - 17 Nov 2026 6:33:09 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.api;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.ZIP_ARCHIVE_CONTENT_TYPE;
import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.responseOutputStream;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.naturalSortCompare;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupServiceSupport;
import net.solarnetwork.node.domain.SettingNote;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingsBackup;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.setup.web.SettingsController.CleanSupportSettingsCommand;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceRegistry;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.settings.SettingSpecifierProviderInfo;
import net.solarnetwork.settings.support.BasicSettingSpecifierProviderInfo;

/**
 * Settings API shared between REST and internal API.
 *
 * @author matt
 * @version 1.0
 * @since 5.6
 */
@GlobalExceptionRestController
@RequestMapping({ "/api/v1/sec/settings", "/a/settings" })
public class SettingsSharedController {

	private final IdentityService identityService;
	private final ServiceRegistry serviceRegistry;
	private final OptionalService<SettingsService> settingsService;
	private final OptionalService<BackupManager> backupManager;

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        the identity service to use
	 * @param serviceRegistry
	 *        the service registry to use
	 * @param settingsService
	 *        the settings service to use
	 * @param backupManager
	 *        the backup manager to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SettingsSharedController(IdentityService identityService, ServiceRegistry serviceRegistry,
			@Qualifier("settingsService") OptionalService<SettingsService> settingsService,
			@Qualifier("backupManager") OptionalService<BackupManager> backupManager) {
		super();
		this.identityService = requireNonNullArgument(identityService, "identityService");
		this.serviceRegistry = requireNonNullArgument(serviceRegistry, "serviceRegistry");
		this.settingsService = requireNonNullArgument(settingsService, "settingsService");
		this.backupManager = requireNonNullArgument(backupManager, "backupManager");
	}

	/**
	 * Get a list of setting specifier provider infos for a given service
	 * filter.
	 *
	 * @param serviceFilter
	 *        the LDAP search filter of the providers to get info for, for
	 *        example
	 *        {@code (objectClass=net.solarnetwork.node.service.MetadataService)}
	 * @param allowAll
	 *        {@code true} to allow providers without any UID value, or
	 *        {@code false} to require a non-empty UID value
	 * @param locale
	 *        the desired locale
	 * @return the result list
	 */
	@GetMapping("/providerInfo")
	@ResponseBody
	public Result<List<SettingSpecifierProviderInfo>> providerInfos(
			@RequestParam("filter") String serviceFilter,
			@RequestParam(name = "all", required = false) boolean allowAll, Locale locale) {
		if ( serviceRegistry == null ) {
			return success();
		}
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		final List<SettingSpecifierProviderInfo> results = serviceRegistry.services(serviceFilter)
				.stream().filter((p) -> {
					if ( p == null || !(p instanceof Identifiable ip) ) {
						return false;
					}
					if ( allowAll ) {
						return true;
					}
					String uid = ip.getUid();
					if ( uid == null || uid.isEmpty() ) {
						return false;
					}
					return true;
				}).map((p) -> {
					Identifiable ip = (Identifiable) p;
					String uid = requireNonNullElse(ip.getUid(), "");
					String groupUid = ip.getGroupUid();
					if ( p instanceof SettingSpecifierProvider ssp ) {
						return ssp.localizedInfo(locale, uid, groupUid);
					}
					return new BasicSettingSpecifierProviderInfo("", ip.getDisplayName(), uid, groupUid);
				}).sorted((l, r) -> {
					int result = naturalSortCompare(l.getDisplayName(), r.getDisplayName(), true);
					if ( result == 0 ) {
						result = naturalSortCompare(l.getUid(), r.getUid(), true);
					}
					return result;
				}).toList();
		return success(results);
	}

	/**
	 * Find available notes for a given key.
	 *
	 * @param key
	 *        the setting key to get notes for
	 * @return the notes
	 */
	@GetMapping("/notes")
	@ResponseBody
	public Result<List<SettingNote>> notesForKey(@RequestParam("key") String key) {
		final SettingsService service = service(settingsService);
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
	 * Save notes for a given key.
	 *
	 * @param command
	 *        the setting notes to save
	 * @return success result
	 */
	@PostMapping("/notes")
	@ResponseBody
	public Result<Void> saveNotes(SettingsCommand command) {
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		service.saveNotes(command);
		return success();
	}

	/**
	 * Add a new factory instance.
	 *
	 * @param factoryUid
	 *        the factory to create a new instance for
	 * @param instanceUid
	 *        the instance ID, or {@code null} to auto-assign one
	 * @return the new instance ID
	 */
	@PostMapping("/manage/add")
	@ResponseBody
	public Result<String> addConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "name", required = false) String instanceUid) {
		final SettingsService service = service(settingsService);
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
	@PostMapping("/manage/delete")
	@ResponseBody
	public Result<Object> deleteConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "instance", required = true) String instanceUid) {
		final SettingsService service = service(settingsService);
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
	@PostMapping("/manage/reset")
	@ResponseBody
	public Result<Object> resetConfiguration(
			@RequestParam(value = "uid", required = true) String factoryUid,
			@RequestParam(value = "instance", required = true) String instanceUid) {
		final SettingsService service = service(settingsService);
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
	@PostMapping("/manage/removeall")
	@ResponseBody
	public Result<Object> removeAllConfigurations(
			@RequestParam(value = "uid", required = true) String factoryUid) {
		final SettingsService service = service(settingsService);
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
	@PostMapping("/save")
	@ResponseBody
	public Result<Void> saveSettings(CleanSupportSettingsCommand command) {
		final SettingsService service = service(settingsService);
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
	@GetMapping(value = "/export")
	@ResponseBody
	public void exportSettings(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response, @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING,
					required = false) final String acceptEncoding)
			throws IOException {
		final SettingsService service = service(settingsService);
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
				@SuppressWarnings("deprecation")
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
	@GetMapping("/exportFactory")
	@ResponseBody
	public void exportFactorySettings(@RequestParam("uid") String factoryUid,
			HttpServletResponse response, Locale locale) throws IOException {
		final SettingsService service = service(settingsService);
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
	 *        the optional factory instance ID, or {@code null}
	 * @param key
	 *        the resource setting key
	 * @param response
	 *        the HTTP response
	 * @param acceptEncoding
	 *        the Accept-Encoding header value
	 * @throws IOException
	 *         if any IO error occurs
	 */
	@GetMapping("/exportResources")
	@ResponseBody
	public void exportSettingsResources(@RequestParam("handlerKey") String handlerKey,
			@RequestParam(name = "instanceKey", required = false) String instanceKey,
			@RequestParam("key") String key, HttpServletResponse response,
			@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING,
					required = false) final String acceptEncoding)
			throws IOException {
		final SettingsService service = service(settingsService);
		if ( service != null ) {
			SettingResourceHandler handler = service.getSettingResourceHandler(handlerKey, instanceKey);
			if ( handler != null ) {
				Iterable<Resource> rItr = handler.currentSettingResources(key);
				if ( rItr != null ) {
					List<Resource> rList = StreamSupport.stream(rItr.spliterator(), false).toList();
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
	@GetMapping("/exportBackup")
	@ResponseBody
	public void exportBackup(@RequestParam(required = false, value = "backup") String backupKey,
			HttpServletResponse response) throws IOException {
		final BackupManager manager = service(backupManager);
		if ( manager == null ) {
			return;
		}

		final String exportFileName = backupExportFileNameForBackupKey(backupKey);

		// create the zip archive for the backup files
		response.setContentType(ZIP_ARCHIVE_CONTENT_TYPE);
		response.setHeader("Content-Disposition", "attachment; filename=" + exportFileName);
		manager.exportBackupArchive(backupKey, response.getOutputStream());
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

}
