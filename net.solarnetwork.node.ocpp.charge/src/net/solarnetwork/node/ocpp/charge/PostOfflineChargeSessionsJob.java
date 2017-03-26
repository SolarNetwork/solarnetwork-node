/* ==================================================================
 * PostOfflineChargeSessionsJob.java - 16/06/2015 7:42:31 pm
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

package net.solarnetwork.node.ocpp.charge;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.ocpp.ChargeSessionManager;

/**
 * Job to periodically look for offline charge sessions that need to be posted
 * to the central system.
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class PostOfflineChargeSessionsJob extends AbstractJob {

	private ChargeSessionManager service;
	private TransactionTemplate transactionTemplate;
	private int maximum = 5;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( service == null ) {
			log.warn("No ChargeSessionManager available, cannot post offline charge sessions.");
			return;
		}
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					postCompletedOfflineSessions();
					return null;
				}
			});
		} else {
			postCompletedOfflineSessions();
		}
	}

	private void postCompletedOfflineSessions() {
		final int posted = service.postCompleteOfflineSessions(maximum);
		log.info("{} completed offline charge sessions posted to OCPP central system", posted);
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
	 * Set the maximum number of offline sessions to attempt to post during a
	 * single execution of the job.
	 * 
	 * @param maximum
	 *        The maximum number to attempt to post.
	 */
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}

}
