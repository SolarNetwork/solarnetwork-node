/* ==================================================================
 * JobUtils.java - 29/10/2019 11:00:03 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Utility methods for working with scheduled jobs.
 * 
 * @author matt
 * @version 2.0
 * @since 1.71
 */
public class JobUtils {

	private static final Logger log = LoggerFactory.getLogger(JobUtils.class);

	/**
	 * Get an existing job, or create one if it doesn't exist.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param jobClass
	 *        the job class
	 * @param jobKey
	 *        the job key
	 * @param jobDescription
	 *        a friendly description of the job
	 * @return the job, never {@literal null}
	 * @throws SchedulerException
	 *         if any scheduling error occurs
	 */
	public static JobDetail getOrCreateJobDetail(final Scheduler scheduler,
			final Class<? extends Job> jobClass, final JobKey jobKey, final String jobDescription)
			throws SchedulerException {
		if ( scheduler == null ) {
			log.warn("No scheduler avaialable, cannot get job {}", jobDescription);
			return null;
		}
		JobDetail jobDetail = scheduler.getJobDetail(jobKey);
		if ( jobDetail == null ) {
			jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobKey).storeDurably().build();
			scheduler.addJob(jobDetail, true);
		}
		return jobDetail;
	}

	/**
	 * Schedule an interval or cron-based trigger for a job, creating the job if
	 * it doesn't alraedy exist.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param jobClass
	 *        the job class
	 * @param jobKey
	 *        the job key
	 * @param jobDescription
	 *        a friendly description of the job
	 * @param schedule
	 *        if a whole number, then the frequency, in seconds, at which to
	 *        execute the job; otherwise a Quartz-compatible cron expression
	 *        representing the schedule at which to execute the job
	 * @param triggerKey
	 *        the trigger key
	 * @param jobData
	 *        the job data
	 * @return the scheduled trigger, or {@literal null} if {@code scheduler} is
	 *         {@literal null} or the job cannot be configured
	 */
	public static Trigger scheduleJob(final Scheduler scheduler, final Class<? extends Job> jobClass,
			final JobKey jobKey, final String jobDescription, final String schedule,
			final TriggerKey triggerKey, final JobDataMap jobData) {
		Trigger trigger = null;
		try {
			int freq = Integer.parseInt(schedule);
			trigger = JobUtils.scheduleIntervalJob(scheduler, jobClass, jobKey, jobDescription, freq,
					triggerKey, jobData);
		} catch ( NumberFormatException e ) {
			// assume cron schedule
			trigger = JobUtils.scheduleCronJob(scheduler, jobClass, jobKey, jobDescription, schedule,
					triggerKey, jobData);
		}
		return trigger;
	}

	/**
	 * Schedule a cron-based trigger for a job, creating the job if it doesn't
	 * already exist.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param jobClass
	 *        the job class
	 * @param jobKey
	 *        the job key
	 * @param jobDescription
	 *        a friendly description of the job
	 * @param cronExpression
	 *        the trigger cron expression
	 * @param triggerKey
	 *        the trigger key
	 * @param jobData
	 *        the job data
	 * @return the scheduled trigger, or {@literal null} if {@code scheduler} is
	 *         {@literal null} or the job cannot be configured
	 */
	public static CronTrigger scheduleCronJob(final Scheduler scheduler,
			final Class<? extends Job> jobClass, final JobKey jobKey, final String jobDescription,
			final String cronExpression, final TriggerKey triggerKey, final JobDataMap jobData) {
		if ( scheduler == null ) {
			log.warn("No scheduler avaialable, cannot schedule {} job", jobDescription);
			return null;
		}
		synchronized ( scheduler ) {
			Trigger currTrigger;
			try {
				currTrigger = scheduler.getTrigger(triggerKey);
			} catch ( SchedulerException e1 ) {
				currTrigger = null;
			}
			CronTrigger trigger = currTrigger instanceof CronTrigger ? (CronTrigger) currTrigger : null;
			if ( trigger != null ) {
				// check if interval actually changed
				if ( cronExpression.equals(trigger.getCronExpression()) ) {
					log.debug("{} job cron unchanged at {}", jobDescription, cronExpression);
					return trigger;
				}
				// trigger has changed!
				trigger = createCronTrigger(jobKey, cronExpression, triggerKey, jobData);
				try {
					scheduler.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling {} job", jobDescription, e);
				}
				return trigger;
			}

			// trigger not found; get/create job and trigger now
			try {
				JobDetail jobDetail = getOrCreateJobDetail(scheduler, jobClass, jobKey, jobDescription);
				trigger = createCronTrigger(jobDetail.getKey(), cronExpression, triggerKey, jobData);
				scheduler.scheduleJob(trigger);
				log.info("Job {} ({}) scheduled at cron {}", jobDescription, triggerKey, cronExpression);
				return trigger;
			} catch ( Exception e ) {
				log.error("Error scheduling {} job", jobDescription, e);
				return null;
			}
		}
	}

	/**
	 * Create a cron-based trigger.
	 * 
	 * @param jobKey
	 *        the job key
	 * @param cronExpression
	 *        the cron expression
	 * @param triggerKey
	 *        the trigger key
	 * @param jobData
	 *        the job data
	 * @return the trigger, never {@literal null}
	 */
	public static CronTrigger createCronTrigger(final JobKey jobKey, final String cronExpression,
			final TriggerKey triggerKey, final JobDataMap jobData) {
		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
				.startAt(new Date(System.currentTimeMillis())).usingJobData(jobData)
				.withSchedule(org.quartz.CronScheduleBuilder.cronSchedule(cronExpression)
						.withMisfireHandlingInstructionDoNothing())
				.build();
		return trigger;
	}

	/**
	 * Schedule an interval-based trigger for a job, creating the job if it
	 * doesn't already exist.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param jobClass
	 *        the job class
	 * @param jobKey
	 *        the job key
	 * @param jobDescription
	 *        a friendly description of the job
	 * @param interval
	 *        the job interval, in seconds
	 * @param triggerKey
	 *        the trigger key
	 * @param jobData
	 *        the job data
	 * @return the scheduled trigger, or {@literal null} if {@code scheduler} is
	 *         {@literal null} or the job cannot be configured
	 */
	public static SimpleTrigger scheduleIntervalJob(final Scheduler scheduler,
			final Class<? extends Job> jobClass, final JobKey jobKey, final String jobDescription,
			final int interval, final TriggerKey triggerKey, final JobDataMap jobData) {
		if ( scheduler == null ) {
			log.warn("No scheduler avaialable, cannot schedule {} job", jobDescription);
			return null;
		}
		synchronized ( scheduler ) {
			Trigger currTrigger;
			try {
				currTrigger = scheduler.getTrigger(triggerKey);
			} catch ( SchedulerException e1 ) {
				currTrigger = null;
			}
			SimpleTrigger trigger = currTrigger instanceof SimpleTrigger ? (SimpleTrigger) currTrigger
					: null;
			if ( trigger != null ) {
				// check if interval actually changed
				if ( trigger.getRepeatInterval() == interval ) {
					log.debug("{} job interval unchanged at {}s", jobDescription, interval);
					return trigger;
				}
				// trigger has changed!
				trigger = createIntervalTrigger(jobKey, interval, triggerKey, jobData);
				try {
					scheduler.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling {} job", jobDescription, e);
				}
				return trigger;
			}

			try {
				JobDetail jobDetail = getOrCreateJobDetail(scheduler, jobClass, jobKey, jobDescription);
				trigger = createIntervalTrigger(jobDetail.getKey(), interval, triggerKey, jobData);
				scheduler.scheduleJob(trigger);
				log.info("Job {} ({}) scheduled at interval {}s", jobDescription, triggerKey, interval);
				return trigger;
			} catch ( Exception e ) {
				log.error("Error scheduling {} job", jobDescription, e);
				return null;
			}
		}
	}

	/**
	 * Create a simple interval-based trigger.
	 * 
	 * @param jobKey
	 *        the job key
	 * @param interval
	 *        the job interval, in seconds
	 * @param triggerKey
	 *        the trigger key
	 * @param jobData
	 *        the job data
	 * @return the trigger, never {@literal null}
	 */
	public static SimpleTrigger createIntervalTrigger(final JobKey jobKey, final int interval,
			final TriggerKey triggerKey, final JobDataMap jobData) {
		SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
				.startAt(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(interval)))
				.usingJobData(jobData).withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(interval)
						.withMisfireHandlingInstructionNextWithExistingCount())
				.build();
		return trigger;
	}

	/**
	 * Unschedule a job.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param jobDescription
	 *        the job description
	 * @param triggerKey
	 *        the trigger key
	 * @return {@literal true} if the job was previously scheduled and
	 *         successfully unscheduled
	 */
	public static boolean unscheduleJob(final Scheduler scheduler, final String jobDescription,
			final TriggerKey triggerKey) {
		try {
			boolean unscheduled = scheduler.unscheduleJob(triggerKey);
			if ( unscheduled ) {
				log.info("Job {} ({}) unscheduled.", jobDescription, triggerKey);
			}
			return unscheduled;
		} catch ( SchedulerException e ) {
			log.error("Error unscheduling job", triggerKey, e);
		}
		return false;
	}

	/**
	 * Create a trigger from a schedule expression.
	 * 
	 * <p>
	 * The {@code expression} can be either an integer number representing a
	 * millisecond frequency or else a cron expression.
	 * </p>
	 * 
	 * @param expression
	 *        the schedule expression
	 * @return the trigger, or {@literal null} if the expression cannot be
	 *         parsed into one
	 * @since 2.0
	 */
	public static org.springframework.scheduling.Trigger triggerForExpression(final String expression) {
		if ( expression != null ) {
			try {
				try {
					long ms = Long.parseLong(expression);
					return new PeriodicTrigger(ms);
				} catch ( NumberFormatException e ) {
					// ignore
				}
				return new org.springframework.scheduling.support.CronTrigger(expression);
			} catch ( IllegalArgumentException e ) {
				log.warn("Error parsing cron expression [{}]: {}", expression, e.getMessage());
			}
		}
		return null;
	}

}
