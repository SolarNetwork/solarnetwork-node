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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.node.job.ManagedTriggerAndJobDetail;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * &lt;reference-list id="managedJobs" interface="net.solarnetwork.node.job.ManagedTriggerAndJobDetail">
 * 		&lt;reference-listener bind-method="onBind" unbind-method="onUnbind">
 * 			&lt;bean class="net.solarnetwork.node.runtime.ManagedJobServiceRegistrationListener">
 * 				&lt;property name="scheduler" ref="scheduler"/>
 *              &lt;property name="bundleContext" ref="bundleContext"/>
 * 			&lt;/bean>
 * 		&lt;/reference-listener>
 * &lt;/reference-list>
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
 * @version 1.0
 */
public class ManagedJobServiceRegistrationListener implements ConfigurationListener {

	private Scheduler scheduler;
	private BundleContext bundleContext;

	private ServiceRegistration<ConfigurationListener> configurationListenerRef;
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

		final CronTrigger trigger = (CronTrigger) trigJob.getTrigger();
		final String pid = (String) properties.get(Constants.SERVICE_PID);

		synchronized ( this ) {
			if ( configurationListenerRef == null ) {
				configurationListenerRef = bundleContext.registerService(ConfigurationListener.class,
						this, null);
			}

			pidMap.put(pid, trigJob);
		}

		JobUtils.scheduleCronJob(scheduler, trigger, trigJob.getJobDetail(), trigger.getCronExpression());
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
		try {
			scheduler.deleteJob(trigJob.getJobDetail().getName(), trigJob.getJobDetail().getGroup());
		} catch ( SchedulerException e ) {
			log.error("Unable to un-schedule job " + trigJob);
			throw new RuntimeException(e);
		}

		final String pid = (String) properties.get(Constants.SERVICE_PID);

		synchronized ( this ) {
			pidMap.remove(pid);
		}
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		if ( event.getType() == ConfigurationEvent.CM_UPDATED ) {
			final String pid = event.getPid();
			ManagedTriggerAndJobDetail trigJob = null;
			synchronized ( pidMap ) {
				trigJob = pidMap.get(pid);
			}
			if ( trigJob != null ) {
				final CronTrigger ct = (CronTrigger) trigJob.getTrigger();

				// even though the cron expression is also updated by ConfigurationAdmin, it can happen in a different thread
				// so it might not be updated yet so we must extract the current value from ConfigurationAdmin
				String newCronExpression = null;
				@SuppressWarnings("unchecked")
				ServiceReference<ConfigurationAdmin> caRef = event.getReference();
				ConfigurationAdmin ca = bundleContext.getService(caRef);
				try {
					Configuration config = ca.getConfiguration(pid, null);
					@SuppressWarnings("unchecked")
					Dictionary<String, ?> props = config.getProperties();
					newCronExpression = (String) props.get("trigger.cronExpression");
				} catch ( IOException e ) {
					log.warn("Exception processing configuration update event", e);
				}
				if ( newCronExpression != null ) {
					JobUtils.scheduleCronJob(scheduler, ct, trigJob.getJobDetail(), newCronExpression);
				}
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
