/* ==================================================================
 * SettingsImporter.java - Feb 19, 2015 2:38:16 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.auto;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceInfo;
import net.solarnetwork.node.backup.BackupResourceProviderInfo;
import net.solarnetwork.node.backup.FileBackupResourceProvider;
import net.solarnetwork.node.backup.SimpleBackupResourceInfo;
import net.solarnetwork.node.backup.SimpleBackupResourceProviderInfo;
import net.solarnetwork.node.settings.SettingsImportOptions;
import net.solarnetwork.node.settings.SettingsService;

/**
 * Look for a settings export to automatically load then the application starts.
 * 
 * The {@link SettingsService#importSettingsCSV(java.io.Reader)} method is used
 * to import a previously-exported settings CSV resource.
 * 
 * @author matt
 * @version 1.1
 */
public class SettingsImporter extends FileBackupResourceProvider {

	private Resource settingsResource = new FileSystemResource("conf/auto-settings.csv");
	private SettingsService settingsService;

	public SettingsImporter() {
		super();
		setResourceDirectories(new String[] { "conf" });
		setFileNamePattern("^auto-settings\\..*");
	}

	/**
	 * Attempt to load the {@code settingsResource}. Does not throw any
	 * exceptions if the resource is not available or cannot be read.
	 */
	public void loadSettings() {
		if ( settingsResource == null ) {
			return;
		}
		Reader in = null;
		try {
			in = new InputStreamReader(settingsResource.getInputStream(), "UTF-8");
			log.info("Auto-importing settings from {}", settingsResource);
			SettingsImportOptions options = new SettingsImportOptions();
			options.setAddOnly(true);
			if ( settingsService != null ) {
				settingsService.importSettingsCSV(in, options);
			}
		} catch ( Exception e ) {
			log.warn("Error importing settings resource {}: {}", settingsResource, e.getMessage());
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this
				}
			}
		}
	}

	@Override
	public String getKey() {
		return SettingsImporter.class.getName();
	}

	@Override
	public BackupResourceProviderInfo providerInfo(Locale locale) {
		String name = "Auto Settings Provider";
		String desc = "Backs up the Auto Settings data.";
		MessageSource ms = getMessageSource();
		if ( ms != null ) {
			name = ms.getMessage("title", null, name, locale);
			desc = ms.getMessage("desc", null, desc, locale);
		}
		return new SimpleBackupResourceProviderInfo(getKey(), name, desc);
	}

	@Override
	public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
		String desc = "Auto Settings";
		MessageSource ms = getMessageSource();
		if ( ms != null ) {
			desc = ms.getMessage("auto-settings.desc", null, desc, locale);
		}
		return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(), desc);
	}

	public void setSettingsService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public void setSettingsResource(Resource settingsResource) {
		this.settingsResource = settingsResource;
	}

}
