/* ==================================================================
 * CentralSystemServiceFactorySupport.java - 9/06/2015 11:05:18 am
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

package net.solarnetwork.node.ocpp.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.Identifiable;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.FilterableService;

/**
 * A base helper class for services that require use of
 * {@link CentralSystemServiceFactory}.
 * 
 * <p>
 * The {@link FilterableService} API can be used to allow dynamic runtime
 * resolution of which central service to use, if more than one are deployed.
 * </p>
 * 
 * <p>
 * This class also implements {@link Identifiable} and will delegate those
 * methods to the configured {@link CentralSystemServiceFactory} if not
 * explicitly defined on this class.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public abstract class CentralSystemServiceFactorySupport
		implements SettingSpecifierProvider, Identifiable {

	private CentralSystemServiceFactory centralSystem;
	private MessageSource messageSource;
	private String uid;
	private String groupUID;

	private final DatatypeFactory datatypeFactory;
	private final GregorianCalendar utcCalendar;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public CentralSystemServiceFactorySupport() {
		super();
		utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch ( DatatypeConfigurationException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a {@link DatatypeFactory} instance.
	 * 
	 * @return The factory.
	 */
	protected final DatatypeFactory getDatatypeFactory() {
		return datatypeFactory;
	}

	/**
	 * Get a {@link XMLGregorianCalendar} for the current time, set to the UTC
	 * time zone.
	 * 
	 * @return A new calendar instance.
	 */
	protected final XMLGregorianCalendar newXmlCalendar() {
		return newXmlCalendar(System.currentTimeMillis());
	}

	/**
	 * Get a {@link XMLGregorianCalendar} for a specific time, set to the UTC
	 * time zone.
	 * 
	 * @param date
	 *        The date, in milliseconds since the epoch.
	 * @return A new calendar instance.
	 */
	protected final XMLGregorianCalendar newXmlCalendar(long date) {
		GregorianCalendar now = (GregorianCalendar) utcCalendar.clone();
		now.setTimeInMillis(date);
		return datatypeFactory.newXMLGregorianCalendar(now);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicTextFieldSettingSpecifier("filterableCentralSystem.propertyFilters['UID']",
				"OCPP Central System"));
		return results;
	}

	/**
	 * Get a status message to display as a read-only setting.
	 * 
	 * @param locale
	 *        The desired locale of the message.
	 * @return The status message.
	 */
	protected abstract String getInfoMessage(Locale locale);

	/**
	 * Get the {@link CentralSystemServiceFactory} to use.
	 * 
	 * @return The configured central system.
	 */
	public final CentralSystemServiceFactory getCentralSystem() {
		return centralSystem;
	}

	/**
	 * Set the {@link CentralSystemServiceFactory} to use. If the provided
	 * object also implements {@link FilterableService} then it will be passed
	 * to {@link #setFilterableCentralSystem(FilterableService)} as well.
	 * 
	 * @param centralSystem
	 *        The central system to use.
	 */
	public final void setCentralSystem(CentralSystemServiceFactory centralSystem) {
		this.centralSystem = centralSystem;
	}

	/**
	 * Get the central system to use, as a {@link FilterableService}. If the
	 * configured central system does not also implement
	 * {@link FilterableService} this method returns <em>null</em>. This method
	 * is designed to assist with dynamic runtime configuration, where more than
	 * one {@link CentralSystemServiceFactory} may be available and a filter is
	 * needed to choose the appropriate one to use.
	 * 
	 * @return The central system, as a {@link FilterableService}.
	 */
	public final FilterableService getFilterableCentralSystem() {
		CentralSystemServiceFactory central = centralSystem;
		if ( central instanceof FilterableService ) {
			return (FilterableService) central;
		}
		return null;
	}

	@Override
	public final MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set the {@link MessageSource} to use for resolving settings messages.
	 * 
	 * @param messageSource
	 *        The message source to use.
	 */
	public final void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * Returns the {@code uid} value if configured, or else falls back to
	 * returning {@link CentralSystemServiceFactory#getUID()}.
	 */
	@Override
	public final String getUID() {
		String id = getUid();
		if ( id == null ) {
			CentralSystemServiceFactory system = centralSystem;
			if ( system != null ) {
				try {
					id = system.getUID();
				} catch ( RuntimeException e ) {
					log.debug("Error getting central system UID: {}", e.getMessage());
				}
			}
		}
		return id;
	}

	/**
	 * Returns the {@code groupUID} value if configured, or else falls back to
	 * returning {@link CentralSystemServiceFactory#getGroupUID()}.
	 */
	@Override
	public final String getGroupUID() {
		String id = groupUID;
		if ( id == null ) {
			CentralSystemServiceFactory system = centralSystem;
			if ( system != null ) {
				try {
					id = system.getGroupUID();
				} catch ( RuntimeException e ) {
					log.debug("Error getting central system Group UID: {}", e.getMessage());
				}
			}
		}
		return id;
	}

	/**
	 * Manage a scheduled job based on a repeating interval.
	 * 
	 * This method will return the created trigger, which should be passed on
	 * subsequent calls if the interval is to be changed or unscheduled.
	 * 
	 * @param scheduler
	 *        The scheduler to use.
	 * @param interval
	 *        The interval, in seconds, for the job to be scheduled at. Pass
	 *        {@code 0} or less to unschedule the job.
	 * @param currTrigger
	 *        The current job trigger, or {@code null} if not scheduled.
	 * @param jobKey
	 *        The key to use for the scheduled job.
	 * @param jobClass
	 *        The class of the {@code Job} to schedule.
	 * @param jobData
	 *        A map of data to associate with the job.
	 * @param jobDescription
	 *        A description to use for the job. This value is included in log
	 *        messages.
	 * @return The scheduled job trigger, or {@code null} if an error occurs or
	 *         the job is unscheduled.
	 * @since 1.1
	 */
	protected SimpleTrigger scheduleIntervalJob(final Scheduler scheduler, final long interval,
			final SimpleTrigger currTrigger, final JobKey jobKey, final Class<? extends Job> jobClass,
			final JobDataMap jobData, final String jobDescription) {
		if ( scheduler == null ) {
			log.warn("No scheduler avaialable, cannot schedule {} job", jobDescription);
			return null;
		}
		SimpleTrigger trigger = currTrigger;
		if ( trigger != null ) {
			// check if interval actually changed
			if ( trigger.getRepeatInterval() == interval ) {
				log.debug("{} job interval unchanged at {}s", jobDescription, interval);
				return currTrigger;
			}
			// trigger has changed!
			if ( interval == 0 ) {
				try {
					scheduler.unscheduleJob(trigger.getKey());
				} catch ( SchedulerException e ) {
					log.error("Error unscheduling {} job", jobDescription, e);
				} finally {
					trigger = null;
				}
			} else {
				trigger = TriggerBuilder.newTrigger().withIdentity(trigger.getKey()).forJob(jobKey)
						.withSchedule(
								SimpleScheduleBuilder.repeatMinutelyForever((int) (interval / (60000L))))
						.build();
				try {
					scheduler.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling {} job", jobDescription, e);
				} finally {
					trigger = null;
				}
			}
			return trigger;
		}

		synchronized ( scheduler ) {
			try {
				JobDetail jobDetail = scheduler.getJobDetail(jobKey);
				if ( jobDetail == null ) {
					jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobKey).storeDurably().build();
					scheduler.addJob(jobDetail, true);
				}
				final TriggerKey triggerKey = new TriggerKey(jobKey.getName() + getUID(),
						jobKey.getGroup());
				trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
						.startAt(new Date(System.currentTimeMillis() + interval)).usingJobData(jobData)
						.withSchedule(
								SimpleScheduleBuilder.repeatMinutelyForever((int) (interval / (60000L)))
										.withMisfireHandlingInstructionNextWithExistingCount())
						.build();
				scheduler.scheduleJob(trigger);
				return trigger;
			} catch ( Exception e ) {
				log.error("Error scheduling {} job", jobDescription, e);
				return null;
			}
		}
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

}
