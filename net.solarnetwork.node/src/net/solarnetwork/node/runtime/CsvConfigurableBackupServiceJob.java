/* ==================================================================
 * LocalStateBackupJob.java - 19/10/2025 12:10:15 pm
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.service.CsvConfigurableBackupService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Backup job for local state data.
 *
 * @author matt
 * @version 1.0
 * @since 4.1
 */
public class CsvConfigurableBackupServiceJob extends BaseIdentifiable implements JobService {

	private final CsvConfigurableBackupService backupService;
	private final String settingUid;

	/**
	 * Constructor.
	 *
	 * @param backupService
	 *        the backup service
	 * @param settingId
	 *        the setting UID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CsvConfigurableBackupServiceJob(CsvConfigurableBackupService backupService,
			String settingUid) {
		super();
		this.backupService = requireNonNullArgument(backupService, "backupService");
		this.settingUid = requireNonNullArgument(settingUid, "settingUid");
	}

	@Override
	public String getSettingUid() {
		return settingUid;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		backupService.backupCsvConfiguration();
	}

}
