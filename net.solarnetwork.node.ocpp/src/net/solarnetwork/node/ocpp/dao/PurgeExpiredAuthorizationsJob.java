/* ==================================================================
 * PurgeExpiredAuthorizationsJob.java - 8/06/2015 7:34:58 pm
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

package net.solarnetwork.node.ocpp.dao;

import java.util.Calendar;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.ocpp.AuthorizationDao;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job to purge expired authorizations by calling
 * {@link AuthorizationDao#deleteExpiredAuthorizations(java.util.Date)}. The
 * maximum expired date to delete is derived from
 * {@link #getMinPurgeExpiredAuthorizationDays()}.
 * 
 * @author matt
 * @version 1.0
 */
public class PurgeExpiredAuthorizationsJob extends AbstractJob implements StatefulJob {

	private AuthorizationDao authorizationDao;
	private int minPurgeExpiredAuthorizationDays = 1;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( authorizationDao == null ) {
			log.debug("No AuthorizationDao avaiable, cannot purge exipred authorizations");
			return;
		}
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -minPurgeExpiredAuthorizationDays);
		int result = authorizationDao.deleteExpiredAuthorizations(c.getTime());
		log.info("Purged {} expired OCPP authorizations {} days old", result,
				minPurgeExpiredAuthorizationDays);
	}

	/**
	 * Set the {@link AuthorizationDao} to use.
	 * 
	 * @param authorizationDao
	 *        The DAO to use.
	 */
	public void setAuthorizationDao(AuthorizationDao authorizationDao) {
		this.authorizationDao = authorizationDao;
	}

	/**
	 * Set the minimum number of days past expiration an authorization must be
	 * before qualifying for purging.
	 * 
	 * @param minPurgeExpiredAuthorizationDays
	 *        The number of days.
	 */
	public void setMinPurgeExpiredAuthorizationDays(int minPurgeExpiredAuthorizationDays) {
		this.minPurgeExpiredAuthorizationDays = minPurgeExpiredAuthorizationDays;
	}

}
