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
import net.solarnetwork.node.settings.SettingsImportOptions;
import net.solarnetwork.node.settings.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Look for a settings export to automatically load then the application starts.
 * 
 * The {@link SettingsService#importSettingsCSV(java.io.Reader)} method is used
 * to import a previously-exported settings CSV resource.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsImporter {

	private Resource settingsResource = new FileSystemResource("conf/auto-settings.csv");
	private SettingsService settingsService;

	private final Logger log = LoggerFactory.getLogger(getClass());

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

	public void setSettingsService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public void setSettingsResource(Resource settingsResource) {
		this.settingsResource = settingsResource;
	}

}
