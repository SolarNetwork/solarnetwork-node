/* ==================================================================
 * BackupController.java - 2/11/2016 8:39:46 AM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupInfo;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.SimpleBackupFilter;
import net.solarnetwork.node.setup.web.support.SortByNodeAndDate;
import net.solarnetwork.service.OptionalService;

/**
 * Controller for backup support.
 *
 * @author matt
 * @version 2.2
 */
@Controller
@RequestMapping("/a/backups")
public class BackupController extends BaseSetupController {

	private Future<Backup> importTask;

	@Autowired
	@Qualifier("backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

	@Autowired
	private MessageSource messageSource;

	/**
	 * Default constructor.
	 */
	public BackupController() {
		super();
	}

	/**
	 * Get a list of all available backups from the active backup service.
	 *
	 * @return All available backups.
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	@ResponseBody
	public Result<List<Backup>> availableBackups() {
		final BackupManager backupManager = backupManagerTracker.service();
		List<Backup> backups = new ArrayList<>();
		if ( backupManager != null ) {
			BackupService service = backupManager.activeBackupService();
			if ( service != null ) {
				backups.addAll(service.getAvailableBackups());
				Collections.sort(backups, SortByNodeAndDate.DEFAULT);
			}
		}
		return success(backups);
	}

	/**
	 * Find backups from the active backup service.
	 *
	 * @param filter
	 *        the search filter
	 * @return All available backups.
	 */
	@RequestMapping(value = "/find", method = RequestMethod.GET)
	@ResponseBody
	public Result<FilterResults<Backup, String>> findBackups(SimpleBackupFilter filter) {
		final BackupManager backupManager = backupManagerTracker.service();
		if ( backupManager != null ) {
			BackupService service = backupManager.activeBackupService();
			if ( service != null ) {
				var results = service.findBackups(filter);
				List<Backup> backups = new ArrayList<>();
				for ( Backup backup : results ) {
					backups.add(backup);
				}
				Collections.sort(backups, SortByNodeAndDate.DEFAULT);
				return success(new BasicFilterResults<>(backups, results.getTotalResults(),
						results.getStartingOffset(), results.getReturnedResultCount()));
			}
		}
		return success(new BasicFilterResults<>(List.of()));
	}

	/**
	 * Create a new backup.
	 *
	 * @return the backup
	 */
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public Result<Backup> initiateBackup() {
		final BackupManager manager = backupManagerTracker.service();
		Backup backup = null;
		if ( manager != null ) {
			backup = manager.createBackup();
		}
		return success(backup);
	}

	/**
	 * Import a backup into the system.
	 *
	 * @param file
	 *        The backup archive to import. The archive should be one returned
	 *        from a previous call to
	 *        {@link BackupManager#exportBackupArchive(String, java.io.OutputStream)}.
	 * @return The response.
	 * @throws IOException
	 *         If an IO error occurs.
	 */
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	@ResponseBody
	public Result<Boolean> importBackup(@RequestParam("file") MultipartFile file) throws IOException {
		Future<Backup> task = importTask;
		if ( task != null && !task.isDone() ) {
			return error("422", "Import task already running");
		}
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return error("500", "No backup manager available.");
		}
		Map<String, String> props = new HashMap<>();
		props.put(BackupManager.BACKUP_KEY, file.getName());
		importTask = manager.importBackupArchive(file.getInputStream(), props);
		return success();
	}

	/**
	 * Check on the last import task, returning the backup {@code key} if the
	 * import completed.
	 *
	 * @return The response.
	 */
	@RequestMapping(value = "/import", method = RequestMethod.GET)
	@ResponseBody
	public Result<String> checkLastImport() {
		Future<Backup> task = importTask;
		try {
			return success(task != null && task.isDone() ? task.get().getKey() : null);
		} catch ( ExecutionException e ) {
			return error("500", "Import exception: " + e.getMessage());
		} catch ( InterruptedException e ) {
			return error("500", "Interrupted");
		}
	}

	/**
	 * Get information about a backup.
	 *
	 * @param key
	 *        The key of the backup to get the information for.
	 * @param locale
	 *        The desired locale of the information.
	 * @return The backup info response.
	 */
	@RequestMapping(value = "/inspect", method = RequestMethod.GET)
	@ResponseBody
	public Result<BackupInfo> inspectBackup(@RequestParam("key") String key, Locale locale) {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return error("500", "No backup manager available.");
		}
		final BackupInfo info = manager.infoForBackup(key, locale);
		if ( info == null ) {
			return error("404", "Backup not available for provided key.");
		}
		return success(info);
	}

	/**
	 * Restore a backup.
	 *
	 * @param options
	 *        the options
	 * @param locale
	 *        the locale
	 * @return the restore status
	 */
	@RequestMapping(value = "/restore", method = RequestMethod.POST)
	@ResponseBody
	public Result<?> restoreBackup(BackupOptions options, Locale locale) {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return error("500", "No backup manager available.");
		}
		final BackupService backupService = manager.activeBackupService();
		if ( backupService == null ) {
			return error("500", "No backup service available.");
		}
		final String backupKey = options.getKey();
		if ( backupKey == null ) {
			return error("422", "No backup key provided.");
		}
		Backup backup = manager.activeBackupService().backupForKey(backupKey);
		if ( backup == null ) {
			return error("404", "Backup not available.");
		}

		Map<String, String> props = options.asBackupManagerProperties();
		manager.restoreBackup(backup, props);
		shutdownSoon();
		return new Result<>(true, null,
				messageSource.getMessage("node.setup.restore.success", null, locale), null);
	}

}
