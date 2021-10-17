/* ==================================================================
 * TablesMaintenanceJob.java - Jul 29, 2017 2:09:24 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Job to execute the {@link TablesMaintenanceService#processTables(String)}
 * method.
 * 
 * @author matt
 * @version 2.0
 * @since 1.8
 */
public class TablesMaintenanceJob extends BaseIdentifiable implements JobService {

	private SettingDao settingDao;
	private TablesMaintenanceService maintenanceService;

	public static final String JOB_KEY_LAST_TABLE_KEY = "TablesMaintenanceLastKey";

	/**
	 * Constructor.
	 * 
	 * @param settingDao
	 *        the setting DAO to use
	 * @param maintenanceService
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TablesMaintenanceJob(SettingDao settingDao, TablesMaintenanceService maintenanceService) {
		super();
		this.settingDao = requireNonNullArgument(settingDao, "settingDao");
		this.maintenanceService = requireNonNullArgument(maintenanceService, "maintenanceService");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.dao.jdbc.derby";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		if ( maintenanceService == null ) {
			return;
		}
		String startAfterKey = settingDao.getSetting(JOB_KEY_LAST_TABLE_KEY, getUid());
		startAfterKey = maintenanceService.processTables(startAfterKey);
		if ( startAfterKey == null ) {
			settingDao.deleteSetting(JOB_KEY_LAST_TABLE_KEY, getUid());
		} else {
			settingDao.storeSetting(JOB_KEY_LAST_TABLE_KEY, getUid(), startAfterKey);
		}
	}

	/**
	 * Set the service to use.
	 * 
	 * @param maintenanceService
	 *        The maintenance service.
	 */
	public void setMaintenanceService(TablesMaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

}
