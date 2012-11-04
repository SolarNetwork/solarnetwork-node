/* ===================================================================
 * JobServiceRegistrationListener.java
 * 
 * Created Dec 2, 2009 10:29:15 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.runtime;

import java.io.IOException;
import java.text.ParseException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import net.solarnetwork.node.job.RandomizedCronTriggerBean;
import net.solarnetwork.node.job.TriggerAndJobDetail;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.util.BaseServiceListener;
import net.solarnetwork.node.util.RegisteredService;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.CronTriggerBean;

/**
 * An OSGi service registration listener for jobs, so they can be automatically
 * registered/unregistered with the job scheduler.
 * 
 * <p>
 * This class is designed to be registered as a listener for
 * {@link TriggerAndJobDetail} beans registered as services. As
 * {@link TriggerAndJobDetail} services are discovered, they will be scheduled
 * to run in the configured {@link Scheduler}. As those services are removed,
 * they will be un-scheduled. In this way bundles can export jobs to be run by
 * the "core" {@code Scheduler} provided by this bundle.
 * </p>
 * 
 * <p>
 * This class will also register {@link JobSettingSpecifierProvider} for every
 * unique bundle symbolic name
 * 
 * <p>
 * For example, this might be configured via Spring DM like this:
 * </p>
 * 
 * <pre>
 * &lt;osgi:list id="triggers" interface="net.solarnetwork.node.job.TriggerAndJobDetail">
 * 		&lt;osgi:listener bind-method="onBind" unbind-method="onUnbind">
 * 			&lt;bean class="net.solarnetwork.node.runtime.JobServiceRegistrationListener">
 * 				&lt;property name="scheduler" ref="scheduler"/>
 *              &lt;property name="bundleContext" ref="bundleContext"/>
 * 			&lt;/bean>
 * 		&lt;/osgi:listener>
 * &lt;/osgi:list>
 * </pre>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>scheduler</dt>
 * <dd>The Quartz {@link Scheduler} for scheduling and un-scheduling jobs with
 * as {@link TriggerAndJobDetail} services are registered and un-registered.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$ $Date: 2012-03-22 09:35:02 +1300 (Thu, 22 Mar
 *          2012) $
 */
public class JobServiceRegistrationListener extends
		BaseServiceListener<TriggerAndJobDetail, RegisteredService<TriggerAndJobDetail>> implements
		ConfigurationListener {

	private Scheduler scheduler;
	
	private ServiceRegistration configurationListenerRef;

	private Map<String, JobSettingSpecifierProvider> providerMap = new TreeMap<String, JobSettingSpecifierProvider>();

	private String pidForSymbolicName(String name) {
		return name + ".JOBS";
	}

	/**
	 * Callback when a trigger has been registered.
	 * 
	 * @param trigJob the trigger and job
	 * @param properties the service properties
	 */
	public void onBind(TriggerAndJobDetail trigJob, Map<String, ?> properties) {
		if ( log.isDebugEnabled() ) {
			log.debug("Bind called on [" +trigJob +"] with props " +properties);
		}

		JobDetail job = trigJob.getJobDetail();
		Trigger trigger = trigJob.getTrigger();

		final String pid = pidForSymbolicName((String) properties.get("Bundle-SymbolicName"));
		String cronExpression = null;
		String settingKey = null;
		JobSettingSpecifierProvider provider = null;
		
		synchronized (providerMap) {
			if ( pid != null ) {
				provider = providerMap.get(pid);
				if ( provider == null ) {
					provider = new JobSettingSpecifierProvider(pid, trigJob.getMessageSource());
					providerMap.put(pid, provider);
				}

				if ( configurationListenerRef == null ) {
					configurationListenerRef = getBundleContext().registerService(
							ConfigurationListener.class.getName(), this, null);
				}

				// check for ConfigurationAdmin cron setting for this trigger,
				// and
				// use that if available
				settingKey = JobSettingSpecifierProvider.triggerKey(trigger);
				if ( trigger instanceof CronTrigger ) {
					cronExpression = ((CronTrigger) trigger).getCronExpression();
					ConfigurationAdmin ca = (ConfigurationAdmin) getBundleContext().getService(
							getBundleContext().getServiceReference(
									ConfigurationAdmin.class.getName()));
					if ( ca != null ) {
						try {
							Configuration conf = ca.getConfiguration(pid, null);
							if ( conf != null ) {
								@SuppressWarnings("unchecked")
								Dictionary<String, ?> props = conf.getProperties();
								if ( props != null ) {
									String newCronExpression = (String) props.get(settingKey);
									if ( newCronExpression != null ) {
										cronExpression = newCronExpression;
									}
								}
							}
						} catch (IOException e) {
							log.warn("Unable to get configuration for {}", pid, e);
						}
					}
				}
			}
		}

		try {
			if ( cronExpression != null && settingKey != null ) {
				scheduleJobForSetting(settingKey, cronExpression, trigJob);
				if ( provider != null ) {
					provider.addSpecifier(trigJob);
					RegisteredService<TriggerAndJobDetail> rs = new RegisteredService<TriggerAndJobDetail>(
							trigJob, properties);
					Properties serviceProps = new Properties();
					serviceProps.put("settingPid", provider.getSettingUID());
					addRegisteredService(rs, provider,
							new String[] { SettingSpecifierProvider.class.getName() }, serviceProps);
				}
			} else {
				scheduler.scheduleJob(job, trigger);
			}
		} catch ( SchedulerException e ) {
			log.error("Error scheduling trigger {} for job {}", new Object[] { trigger.getName(),
					trigger.getJobName(), e });
		}
	}
	
	/**
	 * Callback when a trigger has been un-registered.
	 * 
	 * @param trigJob the trigger and job
	 * @param properties the service properties
	 */
	public void onUnbind(TriggerAndJobDetail trigJob, Map<String, ?> properties) {
		try {
			scheduler.deleteJob(trigJob.getJobDetail().getName(), trigJob.getJobDetail().getGroup());
		} catch ( SchedulerException e ) {
			log.error("Unable to un-schedule job " +trigJob);
			throw new RuntimeException(e);
		}

		removeRegisteredService(trigJob, properties);

		final String pid = pidForSymbolicName((String) properties.get("Bundle-SymbolicName"));

		JobSettingSpecifierProvider provider = null;
		synchronized (providerMap) {
			provider = providerMap.get(pid);
			if ( provider != null ) {
				provider.removeSpecifier(trigJob);
			}
		}
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		if ( event.getType() == ConfigurationEvent.CM_UPDATED ) {
			JobSettingSpecifierProvider provider = null;
			synchronized (providerMap) {
				provider = providerMap.get(event.getPid());
			}
			if ( provider != null ) {
				ConfigurationAdmin ca = (ConfigurationAdmin) getBundleContext().getService(
						event.getReference());
				try {
					Configuration config = ca.getConfiguration(event.getPid(), null);
					@SuppressWarnings("unchecked")
					Dictionary<String, ?> props = config.getProperties();
					log.debug("CA PID {} updated props: {}", event.getPid(), props);
					Enumeration<String> keys = props.keys();
					while ( keys.hasMoreElements() ) {
						String key = keys.nextElement();
						List<RegisteredService<TriggerAndJobDetail>> tjList = getRegisteredServices();
						synchronized (tjList) {
							for ( RegisteredService<TriggerAndJobDetail> rs : tjList ) {
								TriggerAndJobDetail tj = rs.getConfig();
								if ( key.equals(JobSettingSpecifierProvider.triggerKey(tj
										.getTrigger())) ) {
									scheduleJobForSetting(key, (String) props.get(key), tj);
								}
							}

						}
					}
				} catch (IOException e) {
					log.warn("Exception processing configuration update event", e);
				}
			}
		}
	}

	private void scheduleJobForSetting(String key, String newCronExpression,
			TriggerAndJobDetail tj) {
		// has the trigger value actually changed?
		CronTrigger ct = (CronTrigger) tj.getTrigger();
		boolean reschedule = false;
		try {
			CronTrigger runtimeTrigger = (CronTrigger) scheduler.getTrigger(ct.getName(),
					ct.getGroup());
			if ( runtimeTrigger != null ) {
				reschedule = true;
				ct = runtimeTrigger;
			}
		} catch (SchedulerException e) {
			log.warn("Error getting trigger {}.{}", new Object[] { ct.getGroup(), ct.getName(), e });
		}
		String currentCronExpression = ct.getCronExpression();
		if ( ct instanceof RandomizedCronTriggerBean ) {
			currentCronExpression = ((RandomizedCronTriggerBean) ct).getBaseCronExpression();
		}
		if ( !reschedule || !newCronExpression.equals(currentCronExpression) ) {
			if ( reschedule ) {
				log.info("Trigger {} cron changed from {} to {}", new Object[] { key,
					currentCronExpression, newCronExpression });
				CronTriggerBean newTrigger;
				if ( ct instanceof RandomizedCronTriggerBean ) {
					RandomizedCronTriggerBean oldR = (RandomizedCronTriggerBean) ct;
					RandomizedCronTriggerBean r = new RandomizedCronTriggerBean();
					r.setRandomSecond(oldR.isRandomSecond());
					newTrigger = r;
				} else {
					newTrigger = new CronTriggerBean();
				}
				newTrigger.setName(ct.getName());
				newTrigger.setGroup(ct.getGroup());
				newTrigger.setJobName(ct.getJobName());
				newTrigger.setJobGroup(ct.getJobGroup());
				newTrigger.setDescription(ct.getDescription());
				newTrigger.setMisfireInstruction(ct.getMisfireInstruction());
				try {
					newTrigger.setCronExpression(newCronExpression);
					scheduler.rescheduleJob(ct.getName(), ct.getGroup(), newTrigger);
				} catch (ParseException e) {
					log.error("Error in cron expression [{}]", newCronExpression, e);
				} catch (SchedulerException e) {
					log.error("Error re-scheduling trigger {} for job {}",
							new Object[] { ct.getName(), ct.getJobName(), e });
				}
			} else {
				log.info("Scheduling trigger {} as cron {}", new Object[] { key,
						currentCronExpression, newCronExpression });
				try {
					scheduler.scheduleJob(tj.getJobDetail(), ct);
				} catch (SchedulerException e) {
					log.error("Error scheduling trigger {} for job {}", new Object[] {
							ct.getName(), ct.getJobName(), e });
				}
			}
		}
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

}
