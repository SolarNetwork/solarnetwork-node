/* ==================================================================
 * SettingsBackupJob.java - Nov 5, 2012 7:54:10 PM
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

package net.solarnetwork.node.settings.ca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.settings.SettingsService;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job to backup the settings database table to a CSV text file.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsBackupJob extends AbstractJob implements StatefulJob {
	
	private static final String SETTING_LAST_BACKUP_DATE = SettingsBackupJob.class.getName()+".lastBackupDate";
	
	private String destinationPath;
	private SettingDao settingDao;
	private SettingsService settingsService;
	private Integer maxBackupCount;
	
	@Override
	protected void executeInternal(JobExecutionContext jobContext)
			throws Exception {
		final Date mrd = settingDao.getMostRecentModificationDate();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		final String lastBackupDateStr = settingDao.getSetting(SETTING_LAST_BACKUP_DATE);
		final Date lastBackupDate = (lastBackupDateStr == null ? null : sdf.parse(lastBackupDateStr));
		if ( mrd == null || (lastBackupDate != null && lastBackupDate.after(mrd)) ) {
			log.debug("Settings unchanged since last backup on {}", lastBackupDateStr);
			return;
		}
		final String backupDateKey = sdf.format(new Date());
		final File dir = new File(destinationPath);
		if ( !dir.exists() ) {
			dir.mkdirs();
		}
		final File f = new File(dir, "settings_"+backupDateKey+".txt");
		log.info("Backing up settings to {}", f.getPath());
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			settingsService.exportSettingsCSV(writer);
			settingDao.storeSetting(SETTING_LAST_BACKUP_DATE, backupDateKey);
		} catch ( IOException e ) {
			log.error("Unable to create settings backup {}: {}", f.getPath(), e.getMessage());
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch ( IOException e ) {
				// ignore
			}
		}
		
		// TODO: clean out older backups
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}
	
	public void setSettingsService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	public void setMaxBackupCount(Integer maxBackupCount) {
		this.maxBackupCount = maxBackupCount;
	}

}
