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
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupInfo;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for backup support.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/a/backup")
public class BackupController {

	private Future<Backup> importTask;

	@Resource(name = "backupManager")
	private OptionalService<BackupManager> backupManagerTracker;

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
		importTask = manager.importBackupArchive(file.getInputStream());
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

}
