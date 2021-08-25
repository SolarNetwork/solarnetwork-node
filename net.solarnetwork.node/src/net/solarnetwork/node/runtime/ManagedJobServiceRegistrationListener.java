/* ==================================================================
 * ManagedJobServiceRegistrationListener.java - Jul 22, 2013 9:42:57 AM
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.job.ManagedTriggerAndJobDetail;
import net.solarnetwork.node.job.ServiceProvider;

/**
 * An OSGi service registration listener for {@link ManagedTriggerAndJobDetail},
 * so they can be automatically registered/unregistered with the job scheduler.
 * 
 * <p>
 * This class is designed to be registered as a listener for
 * {@link ManagedTriggerAndJobDetail} beans registered as services. It only
 * works with {@link CronTrigger} triggers. As
 * {@link ManagedTriggerAndJobDetail} services are discovered, they will be
 * scheduled to run in the configured {@link Scheduler}. As the services are
 * removed, they will be un-scheduled. In this way bundles can export jobs to be
 * run by the "core" {@code Scheduler} provided by this bundle.
 * </p>
 * 
 * <p>
 * For example, this might be configured via OSGi Blueprint like this:
 * </p>
 * 
 * <pre>
 * &lt;reference-list id="managedJobs" interface="net.solarnetwork.node.job.ManagedTriggerAndJobDetail"&gt;
 * 		&lt;reference-listener bind-method="onBind" unbind-method="onUnbind"&gt;
 * 			&lt;bean class="net.solarnetwork.node.runtime.ManagedJobServiceRegistrationListener"&gt;
 * 				&lt;property name="scheduler" ref="scheduler"/&gt;
 *              &lt;property name="bundleContext" ref="bundleContext"/&gt;
 * 			&lt;/bean&gt;
 * 		&lt;/reference-listener&gt;
 * &lt;/reference-list&gt;
 * </pre>
 * 
 * <p>
 * This class also implements {@link ConfigurationListener} and will
 * automatically register itself for {@link ConfigurationEvent} updates. If the
 * cron expression associated with a registered job changes, the job will be
 * automatically rescheduled with the new cron expression.
 * </p>
 * 
 * <p>
 * As {@link ManagedTriggerAndJobDetail} implements {@link ServiceProvider} as
 * well, any service configurations returned by
 * {@link ServiceProvider#getServiceConfigurations()} will be automatically
 * registered along with the job. When the job is unregistered the associated
 * services will be unregistered as well.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>scheduler</dt>
 * <dd>The Quartz {@link Scheduler} for scheduling and un-scheduling jobs with
 * as {@link ManagedTriggerAndJobDetail} services are registered and
 * un-registered.</dd>
 * 
 * <dt>bundleContext</dt>
 * <dd>The {@link BundleContext} to register for {@link ConfigurationEvent}
 * notifications with.</dd>
 * </dl>
 * 
 * @author matt
 * @version 2.4
 */
public class ManagedJobServiceRegistrationListener implements ConfigurationListener {

	private Scheduler scheduler;
	private BundleContext bundleContext;

	private ServiceRegistration<ConfigurationListener> configurationListenerRef;
	private final Map<String, List<ServiceRegistration<?>>> registeredServices = new HashMap<String, List<ServiceRegistration<?>>>();
	private final Map<String, ManagedTriggerAndJobDetail> pidMap = new HashMap<String, ManagedTriggerAndJobDetail>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Call to close down this instance.
	 */
	public void finish() {
		if ( configurationListenerRef != null ) {
			configurationListenerRef.unregister();
			configurationListenerRef = null;
		}
	}

	/**
	 * Callback when a trigger has been registered.
	 * 
	 * @param trigJob
	 *        the trigger and job
	 * @param properties
	 *        the service properties
	 */
	public void onBind(ManagedTriggerAndJobDetail trigJob, Map<String, ?> properties) {
		log.debug("Bind called on [{}] with props {}", trigJob, properties);

		if ( !(trigJob.getTrigger() instanceof CronTrigger) ) {
			log.warn("Trigger {} is not a CronTrigger! Not scheduling.",
					JobUtils.triggerKey(trigJob.getTrigger()));
			return;
		}

		final Trigger origTrigger = trigJob.getTrigger();
		final JobDetail origJobDetail = trigJob.getJobDetail();
		final String pid = (String) properties.get(Constants.SERVICE_PID);

		// rename job name and trigger name to account for instance
		final Trigger instanceTrigger = origTrigger.getTriggerBuilder().withIdentity(pid).forJob(pid)
				.build();
		final JobDetail instanceJobDetail = origJobDetail.getJobBuilder().withIdentity(pid).build();

		synchronized ( this ) {
			if ( configurationListenerRef == null ) {
				configurationListenerRef = bundleContext.registerService(ConfigurationListener.class,
						this, null);
			}

			pidMap.put(pid, trigJob);
		}

		Collection<ServiceProvider.ServiceConfiguration> services = trigJob.getServiceConfigurations();
		if ( services != null ) {
			for ( ServiceProvider.ServiceConfiguration conf : services ) {
				Object service = conf.getService();
				String[] interfaces = conf.getInterfaces();
				Dictionary<String, ?> props = dictionaryForMap(conf.getProperties());
				if ( service != null && interfaces != null && interfaces.length > 0 ) {
					log.debug("Registering managed service {} as {} with props {}", service,
							Arrays.toString(interfaces), props);
					ServiceRegistration<?> ref = bundleContext.registerService(interfaces, service,
							props);
					synchronized ( this ) {
						List<ServiceRegistration<?>> refs = registeredServices.get(pid);
						if ( refs == null ) {
							refs = new ArrayList<ServiceRegistration<?>>(services.size());
							registeredServices.put(pid, refs);
						}
						refs.add(ref);
					}
				}
			}
		}

		Trigger t = instanceTrigger;
		final String schedule = trigJob.getTriggerScheduleExpression();
		try {
			long ms = Long.parseLong(schedule);
			t = TriggerBuilder.newTrigger().withIdentity(t.getKey()).forJob(t.getJobKey())
					.startAt(new Date(System.currentTimeMillis() + ms)).usingJobData(t.getJobDataMap())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(ms)
							.repeatForever().withMisfireHandlingInstructionNextWithExistingCount())
					.build();
		} catch ( NumberFormatException e ) {
			// ignore and treat as-is
		}

		JobUtils.scheduleCronJob(scheduler, t, instanceJobDetail, triggerSchedule(t), t.getJobDataMap());
	}

	private Dictionary<String, ?> dictionaryForMap(Map<String, ?> map) {
		if ( map == null ) {
			return null;
		}
		return new Hashtable<String, Object>(map);
	}

	/**
	 * Callback when a trigger has been un-registered.
	 * 
	 * @param trigJob
	 *        the trigger and job
	 * @param properties
	 *        the service properties
	 */
	public void onUnbind(ManagedTriggerAndJobDetail trigJob, Map<String, ?> properties) {
		if ( trigJob == null ) {
			// gemini blueprint calls this when availability="optional" and there are no services
			return;
		}
		final String pid = (String) properties.get(Constants.SERVICE_PID);

		try {
			boolean deletedJob = scheduler.deleteJob(new JobKey(pid));
			if ( deletedJob ) {
				log.debug("Un-scheduled job {}", pid);
			} else {
				log.warn("Attempted to un-schedule job {} that wasn't found", pid);
			}
		} catch ( SchedulerException e ) {
			log.error("Unable to un-schedule job " + trigJob);
			throw new RuntimeException(e);
		}

		synchronized ( this ) {
			pidMap.remove(pid);
			List<ServiceRegistration<?>> refs = registeredServices.get(pid);
			if ( refs != null ) {
				for ( ServiceRegistration<?> reg : refs ) {
					log.debug("Unregistering managed service " + reg);
					reg.unregister();
				}
				registeredServices.remove(pid);
			}
		}
	}

	private String triggerSchedule(Trigger t) {
		return (t instanceof CronTrigger ? ((CronTrigger) t).getCronExpression()
				: t instanceof SimpleTrigger ? String.valueOf(((SimpleTrigger) t).getRepeatInterval())
						: null);
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		if ( event.getType() == ConfigurationEvent.CM_UPDATED ) {
			final String pid = event.getPid();
			ManagedTriggerAndJobDetail trigJob = null;
			synchronized ( pidMap ) {
				trigJob = pidMap.get(pid);
			}
			if ( trigJob == null ) {
				return;
			}
			final Trigger origTrigger = trigJob.getTrigger();
			final JobDetail origJobDetail = trigJob.getJobDetail();

			// rename job name and trigger name to account for instance
			final Trigger instanceTrigger = origTrigger.getTriggerBuilder().withIdentity(pid).forJob(pid)
					.build();
			final JobDetail instanceJobDetail = origJobDetail.getJobBuilder().withIdentity(pid).build();

			// even though the cron expression is also updated by ConfigurationAdmin, it can happen in a different thread
			// so it might not be updated yet so we must extract the current value from ConfigurationAdmin
			String newSchedule = triggerSchedule(origTrigger);
			JobDataMap newJobDataMap = (JobDataMap) origTrigger.getJobDataMap().clone();
			ServiceReference<ConfigurationAdmin> caRef = event.getReference();
			ConfigurationAdmin ca = bundleContext.getService(caRef);
			try {
				Configuration config = ca.getConfiguration(pid, null);
				Dictionary<String, ?> props = config.getProperties();

				// first look for expression on common attribute names
				String propCronExpression = (String) props.get("triggerCronExpression");
				if ( propCronExpression == null ) {
					// legacy property name
					propCronExpression = (String) props.get("trigger.cronExpression");
				}

				// get JobDataMap
				Enumeration<String> keyEnum = props.keys();
				Pattern pat = Pattern.compile("trigger\\.jobDataMap\\['([a-zA-Z0-9_]*)'\\].*");
				while ( keyEnum.hasMoreElements() ) {
					String key = keyEnum.nextElement();
					Matcher m = pat.matcher(key);
					if ( m.matches() ) {
						newJobDataMap.put(m.group(1), props.get(key));
					}

					// if cron expression not already found, search for any key containing "cronexpression"
					if ( propCronExpression == null ) {
						if ( key.toLowerCase().contains("cronexpression") ) {
							propCronExpression = (String) props.get(key);
						}
					}
				}

				if ( propCronExpression != null ) {
					newSchedule = propCronExpression;
				}
			} catch ( IOException e ) {
				log.warn("Exception processing configuration update event", e);
			}
			if ( newSchedule != null ) {
				JobUtils.scheduleCronJob(scheduler, instanceTrigger, instanceJobDetail, newSchedule,
						newJobDataMap);
			}
		}
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
