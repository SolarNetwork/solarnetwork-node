/* ==================================================================
 * ManagedJobScheduler.java - 13/10/2021 5:02:17 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.node.Constants.SETTING_PID;
import static net.solarnetwork.node.job.JobUtils.triggerForExpression;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.osgi.framework.Constants.SERVICE_PID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.job.ManagedJob;
import net.solarnetwork.node.job.ServiceProvider;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * Service to dynamically register and schedule {@link ManagedJob} instances.
 * 
 * <p>
 * This service schedules jobs that are passed to
 * {@link #registerJob(ManagedJob, Map)} with the configured
 * {@link TaskScheduler}. Each job is expected to be backed by a
 * {@link ConfigurationAdmin} configuration, identified by a unique {@code pid}.
 * The {@code pid} can be provided via the job properties, using the key
 * {@literal service.pid}. This is useful for jobs created via a managed
 * configuration factory. If no {@literal service.pid} job property is provided,
 * the {@link SettingSpecifierProvider#getSettingUid()} value provided by the
 * job will be used as a fallback. This can be useful for directly created jobs.
 * </p>
 * 
 * <p>
 * A {@link SettingSpecifierProvider} service will be registered for each
 * {@code pid}. Multiple jobs sharing the same {@code pid} are allowed. In that
 * case, all jobs will share the same configuration but can rely on different
 * setting keys to define different schedules. The
 * {@link ManagedJob#getScheduleSettingKey()} defines the setting key to use,
 * which defaults to {@literal schedule}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ManagedJobScheduler implements ServiceLifecycleObserver, ConfigurationListener {

	/** The {@code jobStartDelayMs} property default value. */
	public static final long DEFAULT_JOB_START_DELAY_MS = TimeUnit.SECONDS.toMillis(30);

	private static final Logger log = LoggerFactory.getLogger(ManagedJobScheduler.class);

	private final BundleContext bundleContext;
	private final TaskScheduler taskScheduler;

	// our runtime registration database; with nested map so jobs can be bundled into 
	// a single pid value, for example when a plugin registers several related jobs
	private final Map<String, ScheduledJobs> pidMap = new HashMap<>();

	private long jobStartDelayMs = DEFAULT_JOB_START_DELAY_MS;

	private ServiceRegistration<ConfigurationListener> configurationListenerRef;

	private static class ScheduledJobs implements SettingSpecifierProvider {

		private final Map<String, ScheduledJob> jobMap = new HashMap<>(2);
		private ServiceRegistration<SettingSpecifierProvider> settingProviderReg;
		private String settingUid;

		@Override
		public String toString() {
			return String.format("ScheduledJobs{%s}", settingUid);
		}

		private boolean isEmpty() {
			return jobMap.isEmpty();
		}

		private void addJob(ScheduledJob sj) {
			settingUid = sj.job.getSettingUid();
			jobMap.put(sj.job.getUid(), sj);
		}

		private ScheduledJob removeJob(String uid) {
			ScheduledJob sj = jobMap.remove(uid);
			if ( jobMap.isEmpty() ) {
				if ( settingProviderReg != null ) {
					try {
						settingProviderReg.unregister();
					} catch ( Exception e ) {
						log.warn("Error unregistering settings provider for job [{}]: {}", sj.identifier,
								e.toString());
					}
				}
			}
			return sj;
		}

		private ScheduledJob anyScheduledJob() {
			if ( jobMap.isEmpty() ) {
				return null;
			}
			// return first job; assume if mulitple jobs they share the same settings
			Iterator<ScheduledJob> itr = jobMap.values().iterator();
			if ( itr.hasNext() ) {
				ScheduledJob sj = itr.next();
				if ( sj != null ) {
					return sj;
				}
			}
			return null;
		}

		@Override
		public String getSettingUid() {
			return settingUid;
		}

		@Override
		public String getDisplayName() {
			ScheduledJob sj = anyScheduledJob();
			return (sj != null ? sj.job.getDisplayName() : null);
		}

		@Override
		public MessageSource getMessageSource() {
			ScheduledJob sj = anyScheduledJob();
			return (sj != null ? sj.job.getMessageSource() : null);
		}

		@Override
		public List<SettingSpecifier> getSettingSpecifiers() {
			List<SettingSpecifier> result = new ArrayList<>(8);
			for ( ScheduledJob sj : jobMap.values() ) {
				result.addAll(sj.job.getSettingSpecifiers());
			}
			return result;
		}

	}

	private static class ScheduledJob implements Runnable {

		private final String pid;
		private final ManagedJob job;
		private final List<ServiceRegistration<?>> registeredServices;
		private final String identifier;
		private ScheduledFuture<?> future;
		private String schedule;

		private ScheduledJob(String pid, ManagedJob job,
				List<ServiceRegistration<?>> registeredServices) {
			super();
			this.pid = requireNonNullArgument(pid, "pid");
			this.job = requireNonNullArgument(job, "job");
			this.registeredServices = registeredServices;
			this.identifier = jobIdentifier(job, this.pid);
		}

		private synchronized void scheduled(ScheduledFuture<?> future, String schedule) {
			this.future = future;
			if ( this.schedule == null ) {
				log.info("Scheduled job [{}] at [{}]", identifier, schedule);
			} else {
				log.info("Rescheduled job [{}] from [{}] to [{}]", identifier, this.schedule, schedule);
			}
			this.schedule = schedule;
		}

		private synchronized void stop() {
			if ( future != null && !future.isDone() ) {
				future.cancel(true);
				log.debug("Unscheduled job [{}]", identifier);
			}
			future = null;
		}

		@Override
		public void run() {
			JobService js = job.getJobService();
			if ( js != null ) {
				try {
					job.getJobService().executeJobService();
				} catch ( Throwable t ) {
					log.error("Error executing job {}: {}", identifier, t.getMessage());
				}
			}

		}

		@Override
		public String toString() {
			return String.format("ScheduledJob{%s @ %s}", identifier, schedule);
		}

	}

	private static String jobIdentifier(ManagedJob job, String pid) {
		if ( job.getDisplayName() != null && !job.getDisplayName().isEmpty() ) {
			return job.getDisplayName();
		}
		StringBuilder buf = new StringBuilder();
		buf.append(job.getSettingUid());
		if ( !pid.equals(job.getSettingUid()) ) {
			if ( pid.startsWith(job.getSettingUid()) ) {
				buf.append(pid.substring(job.getSettingUid().length()));
			} else {
				buf.append(pid);
			}
		}
		return buf.toString();
	}

	/**
	 * Constructor.
	 * 
	 * @param bundleContext
	 *        the bundle context
	 * @param taskScheduler
	 *        the task scheduler
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ManagedJobScheduler(BundleContext bundleContext, TaskScheduler taskScheduler) {
		super();
		this.bundleContext = requireNonNullArgument(bundleContext, "bundleContext");
		this.taskScheduler = requireNonNullArgument(taskScheduler, "taskScheduler");
	}

	@Override
	public void serviceDidStartup() {
		// nothing		
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( configurationListenerRef != null ) {
			configurationListenerRef.unregister();
			configurationListenerRef = null;
		}
	}

	/**
	 * Register a job.
	 * 
	 * @param job
	 *        the job to register and schedule for execution
	 * @param properties
	 *        optional service properties
	 */
	public void registerJob(ManagedJob job, Map<String, ?> properties) {
		if ( job == null ) {
			return;
		}
		if ( job.getSettingUid() == null ) {
			throw new RuntimeException("Managed job must provide a settingUid value.");
		}
		if ( job.getUid() == null ) {
			throw new RuntimeException("Managed job must provide a uid value.");
		}
		final String pid = servicePid(properties, job.getSettingUid());
		final String identifier = jobIdentifier(job, pid);
		log.debug("Register job [{}] with props {}", identifier, properties);

		List<ServiceRegistration<?>> refs = null;
		Collection<ServiceProvider.ServiceConfiguration> services = job.getServiceConfigurations();
		if ( services != null ) {
			for ( ServiceProvider.ServiceConfiguration conf : services ) {
				Object service = conf.getService();
				String[] interfaces = conf.getInterfaces();
				Dictionary<String, ?> props = dictionaryForMap(conf.getProperties());
				if ( service != null && interfaces != null && interfaces.length > 0 ) {
					try {
						ServiceRegistration<?> ref = bundleContext.registerService(interfaces, service,
								props);
						log.debug("Job [{}] registered managed service {} as {} with props {}",
								identifier, service, Arrays.toString(interfaces), props);
						if ( refs == null ) {
							refs = new ArrayList<>(services.size());
						}
						refs.add(ref);
					} catch ( Exception e ) {
						log.error(
								"Error registering job [{}] managed service {} as {} with props {}: {}",
								identifier, service, Arrays.toString(interfaces), props, e.toString());
					}
				}
			}
		}

		// register configuration listener
		final ScheduledJob sj = new ScheduledJob(pid, job, refs);
		synchronized ( pidMap ) {
			if ( configurationListenerRef == null ) {
				configurationListenerRef = bundleContext.registerService(ConfigurationListener.class,
						this, null);
			}

			ScheduledJobs sjs = pidMap.computeIfAbsent(pid, k -> new ScheduledJobs());
			sjs.addJob(sj);
			if ( sjs.settingProviderReg == null ) {
				Dictionary<String, Object> settingProps = new Hashtable<>();
				if ( properties.containsKey(SERVICE_PID) ) {
					settingProps.put(SERVICE_PID, properties.get(SERVICE_PID));
				}
				settingProps.put(SETTING_PID, job.getSettingUid());
				sjs.settingProviderReg = bundleContext.registerService(SettingSpecifierProvider.class,
						sjs, settingProps);
			}
		}

		String expr = job.getSchedule();
		Trigger trigger = triggerForExpression(expr, TimeUnit.MILLISECONDS, true);
		if ( trigger != null ) {
			ScheduledFuture<?> f = taskScheduler.schedule(sj, trigger);
			sj.scheduled(f, expr);
		}
	}

	/**
	 * Unregister a job.
	 * 
	 * @param job
	 *        the job to unregister and un-schedule for execution
	 * @param properties
	 *        optional service properties
	 */
	public void unregisterJob(ManagedJob job, Map<String, ?> properties) {
		if ( job == null ) {
			return;
		}
		final String pid = servicePid(properties, job.getSettingUid());
		if ( pid == null ) {
			return;
		}
		log.debug("Unregister job [{}] with props {}", pid, properties);
		ScheduledJob sj = null;
		synchronized ( pidMap ) {
			ScheduledJobs sjs = pidMap.get(pid);
			if ( sjs != null ) {
				sj = sjs.removeJob(job.getUid());
				if ( sjs.isEmpty() ) {
					pidMap.remove(pid);
				}
			}
		}
		if ( sj == null ) {
			log.warn("Attempted to unregister job [{}] that wasn't registered", pid);
			return;
		}
		synchronized ( sj ) {
			sj.stop();
			List<ServiceRegistration<?>> refs = sj.registeredServices;
			if ( refs != null ) {
				for ( ServiceRegistration<?> reg : refs ) {
					try {
						reg.unregister();
						log.info("Unregistered job [{}] managed service {}", sj.identifier, reg);
					} catch ( Exception e ) {
						log.warn("Error unregistering job [{}] managed service {}: {}", sj.identifier,
								reg, e.toString());
					}
				}
			}
		}
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		if ( event.getType() != ConfigurationEvent.CM_UPDATED ) {
			return;
		}
		final String pid = event.getPid();
		List<ScheduledJob> sjList = new ArrayList<>(1);
		synchronized ( pidMap ) {
			ScheduledJobs sjs = pidMap.get(pid);
			if ( sjs != null ) {
				sjList.addAll(sjs.jobMap.values());
			}
		}
		if ( sjList.isEmpty() ) {
			return;
		}

		// even though the cron expression is also updated by ConfigurationAdmin, 
		// it can happen in a different thread so it might not be updated yet so
		// we must extract the current value from ConfigurationAdmin
		Dictionary<String, ?> props = null;
		try {
			ServiceReference<ConfigurationAdmin> caRef = event.getReference();
			ConfigurationAdmin ca = bundleContext.getService(caRef);
			Configuration config = ca.getConfiguration(pid, null);
			props = config.getProperties();
		} catch ( Exception e ) {
			log.warn("Exception processing job [{}] configuration update event", pid, e);
		}

		for ( ScheduledJob sj : sjList ) {
			synchronized ( sj ) {
				final String oldSchedule = sj.schedule;
				String newSchedule = null;
				Object configScheduleExpression = props.get(sj.job.getScheduleSettingKey());
				if ( configScheduleExpression != null ) {
					newSchedule = configScheduleExpression.toString();
				}

				if ( !oldSchedule.equalsIgnoreCase(newSchedule) ) {
					Trigger trigger = triggerForExpression(newSchedule, TimeUnit.MILLISECONDS, true);
					if ( trigger != null ) {
						sj.stop();
						ScheduledFuture<?> f = taskScheduler.schedule(sj, trigger);
						sj.scheduled(f, newSchedule);
					} else if ( sj.future != null ) {
						sj.stop();
						log.info("Stopped job [{}]", sj.identifier, oldSchedule, newSchedule);
					}
				}
			}
		}
	}

	private static String servicePid(Map<String, ?> properties, String defaultPid) {
		Object o = properties.get(SERVICE_PID);
		return (o != null ? o.toString() : defaultPid);
	}

	private static Dictionary<String, ?> dictionaryForMap(Map<String, ?> map) {
		if ( map == null ) {
			return null;
		}
		return new Hashtable<>(map);
	}

	/**
	 * Get the job start delay milliseconds.
	 * 
	 * @return the job start delay, in milliseconds; defaults to
	 *         {@link #DEFAULT_JOB_START_DELAY_MS}
	 */
	public long getJobStartDelayMs() {
		return jobStartDelayMs;
	}

	/**
	 * Set the job start delay milliseconds.
	 * 
	 * @param jobStartDelayMs
	 *        the job start delay, in milliseconds
	 */
	public void setJobStartDelayMs(long jobStartDelayMs) {
		this.jobStartDelayMs = jobStartDelayMs;
	}

}
