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
 */

package net.solarnetwork.node.settings.ca;

import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to backup the settings database table to a CSV text file.
 * 
 * @author matt
 * @version 2.0
 */
public class SettingsBackupJob extends BaseIdentifiable implements JobService {

	private final SettingsService settingsService;

	/**
	 * Constructor.
	 * 
	 * @param settingsService
	 *        the setting service to ues
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SettingsBackupJob(SettingsService settingsService) {
		super();
		this.settingsService = ObjectUtils.requireNonNullArgument(settingsService, "settingsService");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.settings.ca.backup";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		settingsService.backupSettings();
	}

}
