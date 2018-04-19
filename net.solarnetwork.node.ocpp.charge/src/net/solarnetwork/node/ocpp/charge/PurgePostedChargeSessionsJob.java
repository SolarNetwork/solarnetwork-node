/* ==================================================================
 * PurgePostedChargeSessionsJob.java - 19/04/2018 3:49:12 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.charge;

import java.util.Calendar;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.ocpp.ChargeSessionManager;

/**
 * Job to delete old charge sessions that have been uploaded.
 * 
 * @author matt
 * @version 1.0
 * @since 0.6
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class PurgePostedChargeSessionsJob extends AbstractJob {

	private ChargeSessionManager service;
	private TransactionTemplate transactionTemplate;
	private int minPurgeUploadedSessionDays = 1;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( service == null ) {
			log.warn("No ChargeSessionManager available, cannot purge posted charge sessions");
			return;
		}
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					purgePostedChargeSessions();
					return null;
				}
			});
		} else {
			purgePostedChargeSessions();
		}
	}

	private void purgePostedChargeSessions() {
		log.debug("Looking for OCPP posted charge sessions older than {} to purge");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -minPurgeUploadedSessionDays);
		int result = service.deletePostedChargeSessions(c.getTime());
		log.info("Purged {} uploaded OCPP charge session at least {} days old", result,
				minPurgeUploadedSessionDays);
	}

	/**
	 * Set the charge session manager to use.
	 * 
	 * @param service
	 *        The service to use.
	 */
	public void setService(ChargeSessionManager service) {
		this.service = service;
	}

	/**
	 * A transaction template to use.
	 * 
	 * @param transactionTemplate
	 *        The template to use.
	 */
	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Set the minimum number of days past upload a session must be before
	 * qualifying for purging.
	 * 
	 * @param minPurgeUploadedSessionDays
	 *        the number of days
	 */
	public void setMinPurgeUploadedSessionDays(int minPurgeUploadedSessionDays) {
		this.minPurgeUploadedSessionDays = minPurgeUploadedSessionDays;
	}

}
