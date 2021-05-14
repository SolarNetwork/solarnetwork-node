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
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import net.solarnetwork.node.job.RandomizedCronTriggerFactoryBean;

/**
 * Utility methods for dealing with Quartz jobs.
 * 
 * @author matt
 * @version 2.1
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
	 * Test if two triggers have the "same" schedule.
	 * 
	 * <p>
	 * The {@code schedule} can either be a number representing a millisecond
	 * frequency or a compatible cron schedule.
	 * </p>
	 * 
	 * @param t
	 *        the existing trigger, or {@literal null} if none
	 * @param schedule
	 *        the desired schedule for the trigger
	 * @return {@literal true} if {@code t} is not {@literal null} and has the
	 *         given schedule
	 * @since 2.1
	 */
	public static boolean triggerSchedulesEqual(Trigger t, String schedule) {
		if ( t == null || schedule == null ) {
			return false;
		}
		try {
			long ms = Long.parseLong(schedule);
			if ( t instanceof SimpleTrigger ) {
				return simpleTriggerSchedulesEqual((SimpleTrigger) t, ms);
			}
			return false;
		} catch ( NumberFormatException e ) {
			// assume a cron schedule
		}
		if ( t instanceof CronTrigger ) {
			return cronTriggerSchedulesEqual((CronTrigger) t, schedule);
		}
		return false;
	}

	/**
	 * Get the "base" cron expression from a randomized cron trigger schedule.
	 * 
	 * @param t
	 *        the trigger to inspect
	 * @return the base expression, or {@literal null}
	 * @since 2.1
	 */
	public static String baseCronExpression(Trigger t) {
		return (t != null
				? t.getJobDataMap().getString(RandomizedCronTriggerFactoryBean.BASE_CRON_EXPRESSION_KEY)
				: null);
	}

	/**
	 * Get a schedule string value for a trigger.
	 * 
	 * @param t
	 *        the trigger to extract a schedule from
	 * @return the schedule, never {@literal null}
	 * @since 2.1
	 */
	public static String triggerSchedule(Trigger t) {
		if ( t instanceof CronTrigger ) {
			return ((CronTrigger) t).getCronExpression();
		} else if ( t instanceof SimpleTrigger ) {
			return String.format("%dms", ((SimpleTrigger) t).getRepeatInterval());
		} else {
			return "";
		}
	}

	/**
	 * Test if a cron trigger has a given schedule.
	 * 
	 * @param t
	 *        the trigger to test
	 * @param newCronExpression
	 *        the cron expression to compare
	 * @return {@literal true} if {@code t} is not {@literal null} and has a
	 *         {@code newCronExpression} schedule
	 * @since 2.1
	 */
	public static boolean cronTriggerSchedulesEqual(CronTrigger t, String newCronExpression) {
		String baseCronExpression = baseCronExpression(t);
		String currentCronExpression;
		if ( baseCronExpression != null ) {
			currentCronExpression = baseCronExpression;
		} else {
			currentCronExpression = (t != null ? t.getCronExpression() : null);
		}
		if ( newCronExpression.equals(currentCronExpression) ) {
			return true;
		}
		return false;
	}

	/**
	 * Test if a simple trigger has a given schedule.
	 * 
	 * @param t
	 *        the trigger to test
	 * @param newInterval
	 *        the interval to compare
	 * @return {@literal true} if {@code t} is not {@literal null} and has a
	 *         {@code newInterval} schedule
	 * @since 2.1
	 */
	public static boolean simpleTriggerSchedulesEqual(SimpleTrigger t, long newInterval) {
		long currInterval = t.getRepeatInterval();
		if ( t != null && currInterval == newInterval ) {
			return true;
		}
		return false;
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
	 * @param newSchedule
	 *        the cron expression or millisecond frequency to re-schedule the
	 *        job with, if not currently scheduled; a simple number is allowed
	 *        to signal that a simple repeating millisecond interval should be
	 *        used
	 * @param newJobDataMap
	 *        new job data to use with the job
	 */
	public synchronized static void scheduleCronJob(Scheduler scheduler, Trigger trigger,
			JobDetail jobDetail, String newSchedule, JobDataMap newJobDataMap) {
		// has the trigger value actually changed?
		Trigger t = trigger;
		boolean reschedule = false;
		try {
			Trigger runtimeTrigger = scheduler.getTrigger(trigger.getKey());
			if ( runtimeTrigger != null ) {
				reschedule = true;
				t = runtimeTrigger;
			}
		} catch ( SchedulerException e ) {
			log.warn("Error getting trigger {}.{}",
					new Object[] { trigger.getKey().getGroup(), trigger.getKey().getName(), e });
		}

		boolean triggerChanged = !triggerSchedulesEqual(t, newSchedule);
		if ( newJobDataMap != null && !newJobDataMap.equals(t.getJobDataMap()) ) {
			log.info("Trigger {} job data changed", triggerKey(trigger));
			triggerChanged = true;
		}
		if ( reschedule && !triggerChanged ) {
			// unchanged
			return;
		}
		if ( reschedule ) {
			reschedule(scheduler, t, jobDetail, newSchedule, newJobDataMap);
		} else {
			if ( log.isInfoEnabled() ) {
				log.info("Scheduling job {} with [{}]", triggerKey(t), triggerSchedule(t));
			}
			try {
				scheduler.scheduleJob(jobDetail, t);
			} catch ( SchedulerException e ) {
				log.error("Error scheduling trigger {}: {}", triggerKey(t), e.toString());
			}
		}
	}

	private static void reschedule(Scheduler scheduler, Trigger t, JobDetail jobDetail,
			String newSchedule, JobDataMap newJobDataMap) {
		FactoryBean<? extends Trigger> newTriggerFactory = null;
		try {
			long ms = Long.parseLong(newSchedule);
			SimpleTriggerFactoryBean f = new SimpleTriggerFactoryBean();
			f.setName(t.getKey().getName());
			f.setGroup(t.getKey().getGroup());
			f.setDescription(t.getDescription());
			f.setMisfireInstruction(t.getMisfireInstruction());
			f.setRepeatInterval(ms);
			f.setJobDetail(jobDetail);
			if ( newJobDataMap != null ) {
				f.setJobDataMap(newJobDataMap);
			}
			newTriggerFactory = f;
		} catch ( NumberFormatException e ) {
			// treat as cron
			CronTriggerFactoryBean f;
			String baseCronExpression = baseCronExpression(t);
			if ( baseCronExpression != null ) {
				RandomizedCronTriggerFactoryBean r = new RandomizedCronTriggerFactoryBean();
				r.setRandomSecond(true);
				f = r;
			} else {
				f = new CronTriggerFactoryBean();
			}
			f.setName(t.getKey().getName());
			f.setGroup(t.getKey().getGroup());
			f.setDescription(t.getDescription());
			f.setMisfireInstruction(t.getMisfireInstruction());
			f.setCronExpression(newSchedule);
			f.setJobDetail(jobDetail);
			if ( newJobDataMap != null ) {
				f.setJobDataMap(newJobDataMap);
			}
			newTriggerFactory = f;
		}

		try {
			if ( newTriggerFactory instanceof InitializingBean ) {
				((InitializingBean) newTriggerFactory).afterPropertiesSet();
			}
			Trigger newTrigger = newTriggerFactory.getObject();
			if ( log.isInfoEnabled() ) {
				log.info("Re-scheduling job {} from [{}] to [{}]", triggerKey(t), triggerSchedule(t),
						triggerSchedule(newTrigger));
			}
			scheduler.rescheduleJob(t.getKey(), newTrigger);
		} catch ( ParseException e ) {
			log.error("Error in trigger schedule [{}]", newSchedule, e);
		} catch ( SchedulerException e ) {
			log.error("Error re-scheduling trigger {}: {}", triggerKey(t), e.toString());
		} catch ( Exception e ) {
			log.error("Error configuring trigger {}: {}", triggerKey(t), e.toString());
		}
	}

}
