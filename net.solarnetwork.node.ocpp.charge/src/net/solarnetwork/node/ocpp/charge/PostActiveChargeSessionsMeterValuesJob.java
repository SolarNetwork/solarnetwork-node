/* ==================================================================
 * PostActiveChargeSessionsMeterValuesJob.java - 26/03/2017 9:02:48 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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
 * Job to periodically post charge session meter values.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class PostActiveChargeSessionsMeterValuesJob extends AbstractJob {

	private ChargeSessionManager service;
	private TransactionTemplate transactionTemplate;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( service == null ) {
			log.warn(
					"No ChargeSessionManager available, cannot post active charge sessions meter values.");
			return;
		}
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus arg0) {
					service.postActiveChargeSessionsMeterValues();
					return null;
				}
			});
		} else {
			service.postActiveChargeSessionsMeterValues();
		}
		log.info("Completed posting active charge sessions meter values to OCPP central system");
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

}
