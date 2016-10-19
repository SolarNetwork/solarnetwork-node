/* ==================================================================
 * SimpleTriggerAndJobDetail.java - Jun 27, 2011 10:56:20 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Trigger;
import org.springframework.context.MessageSource;

/**
 * Simple implementation of {@link TriggerAndJobDetail}.
 * 
 * @author matt
 * @version 1.1
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SimpleTriggerAndJobDetail implements TriggerAndJobDetail {

	private Trigger trigger;
	private JobDetail jobDetail;
	private MessageSource messageSource;

	@Override
	public String toString() {
		return "TriggerAndJobDetail{trigger=" + trigger.getKey().getName() + ",job="
				+ jobDetail.getKey().getName() + '}';
	}

	@Override
	public Trigger getTrigger() {
		return trigger;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	@Override
	public JobDetail getJobDetail() {
		return jobDetail;
	}

	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
