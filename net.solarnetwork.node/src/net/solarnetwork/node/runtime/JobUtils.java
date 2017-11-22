/* ==================================================================
 * JobUtils.java - Jul 22, 2013 8:08:48 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import java.text.ParseException;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import net.solarnetwork.node.job.RandomizedCronTriggerFactoryBean;

/**
 * Utility methods for dealing with Quartz jobs.
 * 
 * @author matt
 * @version 2.0
 */
public class JobUtils {

	private static final Logger log = LoggerFactory.getLogger(JobUtils.class);

	/**
	 * Get a setting key for a Trigger.
	 * 
	 * <p>
	 * The key is named after these elements, joined with a period character:
	 * </p>
	 * 
	 * <ol>
	 * <li>job group (omitted if set to {@link Scheduler#DEFAULT_GROUP})</li>
	 * <li>job name</li>
	 * <li>trigger group (omitted if set to
	 * {@link Scheduler#DEFAULT_GROUP})</li>
	 * <li>trigger name</li>
	 * </ol>
	 * 
	 * @param t
	 *        the trigger to generate the key from
	 * @return the setting key
	 */
	public static String triggerKey(Trigger t) {
		StringBuilder buf = new StringBuilder();
		final TriggerKey triggerKey = t.getKey();
		final JobKey jobKey = t.getJobKey();
		if ( jobKey != null && jobKey.getGroup() != null
				&& !Scheduler.DEFAULT_GROUP.equals(jobKey.getGroup()) ) {
			buf.append(jobKey.getGroup());
		}
		if ( jobKey != null && jobKey.getName() != null ) {
			if ( buf.length() > 0 ) {
				buf.append('.');
			}
			buf.append(jobKey.getName());
		}
		if ( triggerKey.getGroup() != null && !Scheduler.DEFAULT_GROUP.equals(triggerKey.getGroup()) ) {
			if ( buf.length() > 0 ) {
				buf.append('.');
			}
			buf.append(triggerKey.getGroup());
		}
		if ( buf.length() > 0 ) {
			buf.append('.');
		}
		buf.append(triggerKey.getName());
		return buf.toString();
	}

	/**
	 * Schedule a new job or re-schedule an existing job.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param trigger
	 *        the Trigger to schedule
	 * @param jobDetail
	 *        the JobDetail to schedule
	 * @param newCronExpression
	 *        the cron expression to re-schedule the job with, if not currently
	 *        scheduled
	 */
	public static void scheduleCronJob(Scheduler scheduler, CronTrigger trigger, JobDetail jobDetail,
			String newCronExpression, JobDataMap newJobDataMap) {
		// has the trigger value actually changed?
		CronTrigger ct = trigger;
		boolean reschedule = false;
		try {
			Trigger runtimeTrigger = scheduler.getTrigger(trigger.getKey());
			if ( runtimeTrigger != null ) {
				reschedule = true;
				ct = (CronTrigger) runtimeTrigger;
			}
		} catch ( SchedulerException e ) {
			log.warn("Error getting trigger {}.{}",
					new Object[] { trigger.getKey().getGroup(), trigger.getKey().getName(), e });
		}

		String baseCronExpression = null;
		String currentCronExpression = ct.getCronExpression();
		if ( ct.getJobDataMap()
				.containsKey(RandomizedCronTriggerFactoryBean.BASE_CRON_EXPRESSION_KEY) ) {
			baseCronExpression = ct.getJobDataMap()
					.getString(RandomizedCronTriggerFactoryBean.BASE_CRON_EXPRESSION_KEY);
			currentCronExpression = baseCronExpression;
		}
		boolean triggerChanged = false;
		if ( !newCronExpression.equals(currentCronExpression) ) {
			log.info("Trigger {} cron changed from {} to {}",
					new Object[] { triggerKey(trigger), currentCronExpression, newCronExpression });
			triggerChanged = true;
		}
		if ( newJobDataMap != null && !newJobDataMap.equals(ct.getJobDataMap()) ) {
			log.info("Trigger {} job data changed", triggerKey(trigger));
			triggerChanged = true;
		}
		if ( reschedule && !triggerChanged ) {
			// unchanged
			return;
		}
		if ( reschedule ) {
			CronTriggerFactoryBean newTrigger;
			if ( baseCronExpression != null ) {
				RandomizedCronTriggerFactoryBean r = new RandomizedCronTriggerFactoryBean();
				r.setRandomSecond(true);
				newTrigger = r;
			} else {
				newTrigger = new CronTriggerFactoryBean();
			}
			newTrigger.setName(ct.getKey().getName());
			newTrigger.setGroup(ct.getKey().getGroup());
			newTrigger.setDescription(ct.getDescription());
			newTrigger.setMisfireInstruction(ct.getMisfireInstruction());
			newTrigger.setCronExpression(newCronExpression);
			newTrigger.setJobDetail(jobDetail);
			if ( newJobDataMap != null ) {
				newTrigger.setJobDataMap(newJobDataMap);
			}
			try {
				newTrigger.afterPropertiesSet();
				CronTrigger newCt = newTrigger.getObject();
				scheduler.rescheduleJob(ct.getKey(), newCt);
			} catch ( ParseException e ) {
				log.error("Error in cron expression [{}]", newCronExpression, e);
			} catch ( SchedulerException e ) {
				log.error("Error re-scheduling trigger {} for job {}",
						new Object[] { ct.getKey().getName(), jobDetail.getKey().getName(), e });
			}
		} else {
			log.info("Scheduling trigger {} as cron {}",
					new Object[] { triggerKey(trigger), newCronExpression });
			try {
				scheduler.scheduleJob(jobDetail, ct);
			} catch ( SchedulerException e ) {
				log.error("Error scheduling trigger {} for job {}",
						new Object[] { ct.getKey().getName(), jobDetail.getKey().getName(), e });
			}
		}
	}

}
