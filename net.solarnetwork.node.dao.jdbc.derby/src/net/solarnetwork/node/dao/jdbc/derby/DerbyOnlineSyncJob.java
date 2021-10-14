/* ==================================================================
 * DerbyOnlineSyncJob.java - Jan 16, 2012 11:49:17 AM
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

package net.solarnetwork.node.dao.jdbc.derby;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Job to backup the Derby database using the SYSCS_UTIL.SYSCS_FREEZE_DATABASE
 * procedure.
 * 
 * <p>
 * This job is designed with using an OS tool like rsync to make a copy of the
 * Derby database to a backup location.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class DerbyOnlineSyncJob extends BaseIdentifiable implements JobService {

	private final DerbyOnlineSyncService syncService;

	/**
	 * Constructor.
	 * 
	 * @param syncService
	 *        the sync service
	 * @throws IllegalArgumentException
	 *         if any argument is {@litearl null}
	 */
	public DerbyOnlineSyncJob(DerbyOnlineSyncService syncService) {
		super();
		this.syncService = requireNonNullArgument(syncService, "syncService");
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
		syncService.sync();
	}

}
