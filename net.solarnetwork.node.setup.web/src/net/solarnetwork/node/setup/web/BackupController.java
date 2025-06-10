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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupInfo;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.setup.web.support.SortByNodeAndDate;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller for backup support.
 * 
 * @author matt
 * @version 2.0
 */
@Controller
@RequestMapping("/a/backups")
public class BackupController extends BaseSetupController {

	private Future<Backup> importTask;

	@Resource(name = "backupManager")
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
	 * @return All avaialble backups.
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	@ResponseBody
	public Response<List<Backup>> availableBackups() {
		final BackupManager backupManager = backupManagerTracker.service();
		List<Backup> backups = new ArrayList<Backup>();
		if ( backupManager != null ) {
			BackupService service = backupManager.activeBackupService();
			if ( service != null ) {
				backups.addAll(service.getAvailableBackups());
				Collections.sort(backups, SortByNodeAndDate.DEFAULT);
			}
		}
		return Response.response(backups);
	}

	/**
	 * Create a new backup.
	 * 
	 * @return the backup
	 */
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public Response<Backup> initiateBackup() {
		final BackupManager manager = backupManagerTracker.service();
		Backup backup = null;
		if ( manager != null ) {
			backup = manager.createBackup();
		}
		return Response.response(backup);
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
	public Response<Boolean> importBackup(@RequestParam("file") MultipartFile file) throws IOException {
		Future<Backup> task = importTask;
		if ( task != null && !task.isDone() ) {
			return new Response<Boolean>(false, "422", "Import task already running", null);
		}
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return new Response<Boolean>(false, "500", "No backup manager available.", null);
		}
		Map<String, String> props = new HashMap<String, String>();
		props.put(BackupManager.BACKUP_KEY, file.getName());
		importTask = manager.importBackupArchive(file.getInputStream(), props);
		return Response.response(true);
	}

	/**
	 * Check on the last import task, returning the backup {@code key} if the
	 * import completed.
	 * 
	 * @return The response.
	 */
	@RequestMapping(value = "/import", method = RequestMethod.GET)
	@ResponseBody
	public Response<String> checkLastImport() {
		Future<Backup> task = importTask;
		try {
			return Response.response(task != null && task.isDone() ? task.get().getKey() : null);
		} catch ( ExecutionException e ) {
			return new Response<String>(false, "500", "Import exception: " + e.getMessage(), null);
		} catch ( InterruptedException e ) {
			return new Response<String>(false, "500", "Interrupted", null);
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
	public Response<BackupInfo> inspectBackup(@RequestParam("key") String key, Locale locale) {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return new Response<BackupInfo>(false, "500", "No backup manager available.", null);
		}
		final BackupInfo info = manager.infoForBackup(key, locale);
		if ( info == null ) {
			return new Response<BackupInfo>(false, "404", "Backup not available for provided key.",
					null);
		}
		return Response.response(info);
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
	public Response<?> restoreBackup(BackupOptions options, Locale locale) {
		final BackupManager manager = backupManagerTracker.service();
		if ( manager == null ) {
			return new Response<BackupInfo>(false, "500", "No backup manager available.", null);
		}
		final BackupService backupService = manager.activeBackupService();
		if ( backupService == null ) {
			return new Response<Backup>(false, "500", "No backup service available.", null);
		}
		final String backupKey = options.getKey();
		if ( backupKey == null ) {
			return new Response<Object>(false, "422", "No backup key provided.", null);
		}
		Backup backup = manager.activeBackupService().backupForKey(backupKey);
		if ( backup == null ) {
			return new Response<Object>(false, "404", "Backup not available.", null);
		}

		Map<String, String> props = options.asBackupManagerProperties();
		manager.restoreBackup(backup, props);
		shutdownSoon();
		return new Response<Object>(true, null,
				messageSource.getMessage("node.setup.restore.success", null, locale), null);
	}

}
