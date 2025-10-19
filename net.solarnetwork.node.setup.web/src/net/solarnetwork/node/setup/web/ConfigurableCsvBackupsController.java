/* ==================================================================
 * ConfigurableCsvBackupsController.java - 20/10/2025 7:37:58 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.node.setup.web.support.StatusMessageHandlerInterceptor.MODEL_KEY_STATUS_MSG;
import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.responseOutputStream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.domain.StringDateKey;
import net.solarnetwork.node.service.CsvConfigurableBackupService;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.OptionalServiceNotAvailableException;

/**
 * Manage CVS backups
 *
 * @author matt
 * @version 1.0
 * @since 5.1
 */
@Controller
public class ConfigurableCsvBackupsController {

	private static final String VIEW_REDIRECT_SETTINGS_BACKUPS = "redirect:/a/settings/backups";

	private final IdentityService identityService;
	private final Collection<CsvConfigurableBackupService> csvBackupServices;

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        the identity service
	 * @param csvBackupServices
	 *        the list of available backup services
	 */
	public ConfigurableCsvBackupsController(IdentityService identityService,
			@Qualifier("csvBackupServiceList") Collection<CsvConfigurableBackupService> csvBackupServices) {
		super();
		this.identityService = requireNonNullArgument(identityService, "identityService");
		this.csvBackupServices = requireNonNullArgument(csvBackupServices, "csvBackupServices");
	}

	private CsvConfigurableBackupService serviceForId(String serviceId) {
		for ( CsvConfigurableBackupService service : csvBackupServices ) {
			if ( serviceId.equals(service.getSettingUid()) ) {
				return service;
			}
		}
		throw new OptionalServiceNotAvailableException(
				"Service [%s] not available.".formatted(serviceId));
	}

	/**
	 * List the available CSV backups for a given service.
	 *
	 * @param serviceId
	 *        the ID of the service to list the backups for
	 * @return the result
	 */
	@RequestMapping(value = "/a/settings/csv/list-backups", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<StringDateKey>> csvBackupsList(@RequestParam("serviceId") String serviceId) {
		List<StringDateKey> result = List.of();

		final CsvConfigurableBackupService service;
		try {
			service = serviceForId(serviceId);
			result = service.getAvailableCsvConfigurationBackups();
		} catch ( OptionalServiceNotAvailableException e ) {
			// ignore and return empty list
		}

		return success(result);
	}

	/**
	 * Export CSV backup.
	 *
	 * @param serviceId
	 *        the ID of the service to export CSV from
	 * @param key
	 *        the backup key, or {@code null} or empty to export the current
	 *        settings
	 * @param response
	 *        the response
	 * @param acceptEncoding
	 *        the Accept-Encoding header value
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/a/settings/csv/export", method = RequestMethod.GET)
	@ResponseBody
	public void exportSettings(
	// @formatter:off
			@RequestParam("serviceId")
			final String serviceId,

			@RequestParam(required = false, value = "key")
			final String backupKey,

			final HttpServletResponse response,

			@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false)
			final String acceptEncoding
			// @formatter:on
	) throws IOException {
		final CsvConfigurableBackupService service = serviceForId(serviceId);
		final Long nodeId = identityService.getNodeId();
		final String key = (backupKey != null && !backupKey.isBlank() ? backupKey : null);
		response.setContentType("text/csv;charset=UTF-8");
		// @formatter:off
		response.setHeader("Content-Disposition", "attachment; filename=solarnode-"
				+ (nodeId != null ? nodeId + "-" : "")
				+ service.getCsvConfigurationIdentifier()
				+ "_"
				+ (key == null
						? (DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").format(ZonedDateTime.now()))
						: key)
				+ ".csv");
		// @formatter:on
		if ( key != null ) {
			Reader r = service.getCsvConfigurationBackup(backupKey);
			if ( r != null ) {
				try (Writer out = new OutputStreamWriter(responseOutputStream(response, acceptEncoding),
						UTF_8)) {
					FileCopyUtils.copy(r, out);
				}
			}
		} else {
			try (Writer out = new OutputStreamWriter(responseOutputStream(response, acceptEncoding),
					UTF_8)) {
				service.exportCsvConfiguration(out);
			}
		}
	}

	/**
	 * Import settings.
	 *
	 * @param file
	 *        the CSV settings resource to import
	 * @param session
	 *        the session
	 * @return the result view name
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/a/settings/csv/import", method = RequestMethod.POST)
	public String importSettings(
	// @formatter:off
			@RequestParam("serviceId")
			String serviceId,

			@RequestParam(value = "replace", required = false)
			Boolean replace,

			@RequestParam("file")
			MultipartFile file,

			HttpSession session
			// @formatter:on
	) throws IOException {
		final CsvConfigurableBackupService service = serviceForId(serviceId);
		if ( !file.isEmpty() && service != null ) {
			try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), UTF_8)) {
				service.importCsvConfiguration(reader, replace != null ? replace : false);
			}
		}
		session.setAttribute(MODEL_KEY_STATUS_MSG, "settings.csv.io.import.complete");
		return VIEW_REDIRECT_SETTINGS_BACKUPS;
	}

}
