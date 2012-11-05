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
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final String DATE_FORMAT = "yyyy-MM-dd-HHmmss";
	private static final String FILENAME_PREFIX = "settings_";
	private static final String FILENAME_EXT = "txt";
	private static final Pattern FILENAME_PATTERN = Pattern.compile('^' +FILENAME_PREFIX
			+"\\d{4}-\\d{2}-\\d{2}-\\d{6}\\." +FILENAME_EXT +"$");
	private static final int DEFAULT_MAX_BACKUP_COUNT = 5;
	
	private String destinationPath;
	private SettingDao settingDao;
	private SettingsService settingsService;
	private int maxBackupCount = DEFAULT_MAX_BACKUP_COUNT;
	
	@Override
	protected void executeInternal(JobExecutionContext jobContext)
			throws Exception {
		final Date mrd = settingDao.getMostRecentModificationDate();
		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		final String lastBackupDateStr = settingDao.getSetting(SETTING_LAST_BACKUP_DATE, 
				SettingDao.TYPE_IGNORE_MODIFICATION_DATE);
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
		final File f = new File(dir, FILENAME_PREFIX+backupDateKey+'.'+FILENAME_EXT);
		log.info("Backing up settings to {}", f.getPath());
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			settingsService.exportSettingsCSV(writer);
			settingDao.storeSetting(SETTING_LAST_BACKUP_DATE, 
					SettingDao.TYPE_IGNORE_MODIFICATION_DATE, backupDateKey);
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
		
		// clean out older backups
		File[] files = dir.listFiles(new RegexFileFilter(FILENAME_PATTERN));
		if ( files != null && files.length > maxBackupCount ) {
			// sort array 
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					// order in reverse, and then we can delete all but maxBackupCount
					return o2.getName().compareTo(o1.getName());
				}
			});
			for ( int i = maxBackupCount; i < files.length; i++ ) {
				if ( !files[i].delete() ) {
					log.warn("Unable to delete old settings backup file {}", files[i]);
				}
			}
		}
	}
	
	private static class RegexFileFilter implements FileFilter {
		final Pattern p;
		private RegexFileFilter(Pattern p) {
			super();
			this.p = p;
		}
		
		@Override
		public boolean accept(File pathname) {
			Matcher m = p.matcher(pathname.getName());
			return m.matches();
		}
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
