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
import static net.solarnetwork.util.CollectionUtils.dictionaryForMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.osgi.framework.Constants.SERVICE_PID;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
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
import org.springframework.scheduling.support.CronTrigger;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.job.ManagedJob;
import net.solarnetwork.node.job.ServiceProvider;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
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
 * @version 1.2
 */
public class ManagedJobScheduler implements ServiceLifecycleObserver, ConfigurationListener, PingTest {

	/** The {@code randomizedCron} property default value. */
	public static final boolean DEFAULT_RANDOMIZED_CRON = true;

	/** The {@code jobStartDelaySeconds} property default value. */
	public static final int DEFAULT_JOB_START_DELAY_SECS = 0;

	private static final Logger log = LoggerFactory.getLogger(ManagedJobScheduler.class);

	private final BundleContext bundleContext;
	private final TaskScheduler taskScheduler;
	private boolean randomizedCron = DEFAULT_RANDOMIZED_CRON;
	private MessageSource messageSource;
	private int jobStartDelaySeconds = DEFAULT_JOB_START_DELAY_SECS;

	private long startTime = 0;
	private ScheduledFuture<?> startupTask;

	// our runtime registration database; with nested map so jobs can be bundled into
	// a single pid value, for example when a plugin registers several related jobs
	private final Map<String, ScheduledJobs> pidMap = new HashMap<>();

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

		@Override
		public <T> T unwrap(Class<T> type) {
			T result = null;
			for ( ScheduledJob sj : jobMap.values() ) {
				result = sj.job.unwrap(type);
				if ( result != null ) {
					break;
				}
			}
			return result;
		}

	}

	private static final class ScheduledJob implements Runnable {

		private final String pid;
		private final ManagedJob job;
		private final List<ServiceRegistration<?>> registeredServices;
		private final String identifier;
		private ScheduledFuture<?> future;
		private String schedule;
		private String triggerSchedule;
		private Throwable throwable;

		private ScheduledJob(String pid, ManagedJob job,
				List<ServiceRegistration<?>> registeredServices) {
			super();
			this.pid = requireNonNullArgument(pid, "pid");
			this.job = requireNonNullArgument(job, "job");
			this.registeredServices = registeredServices;
			this.identifier = jobIdentifier(job, this.pid);
		}

		private void scheduled(ScheduledFuture<?> future, String schedule, Trigger trigger) {
			this.future = future;
			String newTriggerSchedule = triggerSchedule(trigger, schedule);
			if ( this.schedule == null ) {
				log.info("Scheduled job [{}] at [{}]", identifier, newTriggerSchedule);
			} else {
				log.info("Rescheduled job [{}] from [{}] to [{}]", identifier, this.triggerSchedule,
						newTriggerSchedule);
			}
			this.schedule = schedule;
			this.triggerSchedule = newTriggerSchedule;
		}

		private void stop() {
			stop(false);
		}

		private void stop(boolean preserveRegisteredServices) {
			if ( future != null && !future.isDone() ) {
				future.cancel(true);
				log.info("Unscheduled job [{}]", identifier);
				if ( !preserveRegisteredServices && registeredServices != null
						&& !registeredServices.isEmpty() ) {
					for ( ServiceRegistration<?> reg : registeredServices ) {
						try {
							reg.unregister();
							log.info("Unregistered job [{}] managed service {}", identifier, reg);
						} catch ( Exception e ) {
							log.warn("Error unregistering job [{}] managed service {}: {}", identifier,
									reg, e.toString());
						}
					}
					registeredServices.clear();
				}
			}
			future = null;
		}

		@Override
		public void run() {
			JobService js = job.getJobService();
			if ( js != null ) {
				try {
					throwable = null;
					job.getJobService().executeJobService();
				} catch ( IOException e ) {
					throwable = e;
					log.warn("Communication error executing job {}: {}", identifier, e.toString());
				} catch ( Throwable t ) {
					throwable = t;
					log.error("Error executing job {}: {}", identifier, t.getMessage(), t);
				}
			}
		}

		@Override
		public String toString() {
			return String.format("ScheduledJob{%s @ %s}", identifier, schedule);
		}

	}

	private final class StartupTask implements Runnable {

		private final int delaySecs = getJobStartDelaySeconds();

		@Override
		public void run() {
			log.info("Scheduling jobs after startup delay of {}s", delaySecs);
			scheduleAllJobs();
		}

	}

	private static String triggerSchedule(Trigger trigger, String schedule) {
		if ( trigger instanceof CronTrigger ) {
			return ((CronTrigger) trigger).getExpression();
		}
		return schedule;
	}

	private static String jobIdentifier(final ManagedJob job, final String pid) {
		final String settingUid = job.getSettingUid();
		String name = job.getDisplayName();
		StringBuilder buf = new StringBuilder();
		buf.append(settingUid);
		if ( !pid.equals(settingUid) ) {
			if ( pid.startsWith(settingUid) ) {
				buf.append(pid.substring(settingUid.length()));
			} else {
				buf.append('-').append(pid);
			}
		} else if ( name != null && !name.isEmpty() ) {
			buf.append('-').append(name);
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
	public synchronized void serviceDidStartup() {
		startTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(jobStartDelaySeconds);
		if ( startupTask != null ) {
			startupTask.cancel(true);
		}
		startupTask = taskScheduler.schedule(new StartupTask(), new Date(startTime));
	}

	@Override
	public synchronized void serviceDidShutdown() {
		for ( Entry<String, ScheduledJobs> e : pidMap.entrySet() ) {
			ScheduledJobs sjs = e.getValue();
			for ( String uid : new HashSet<>(sjs.jobMap.keySet()) ) {
				ScheduledJob sj = sjs.removeJob(uid);
				sj.stop();
			}
		}
		pidMap.clear();
		if ( configurationListenerRef != null ) {
			configurationListenerRef.unregister();
			configurationListenerRef = null;
		}
		if ( startupTask != null ) {
			startupTask.cancel(true);
		}
	}

	private synchronized void scheduleAllJobs() {
		for ( ScheduledJobs sjs : pidMap.values() ) {
			for ( ScheduledJob sj : sjs.jobMap.values() ) {
				scheduleJob(sj);
			}
		}
		this.startupTask = null;
	}

	/**
	 * Register a job.
	 *
	 * @param job
	 *        the job to register and schedule for execution
	 * @param properties
	 *        optional service properties
	 */
	public synchronized void registerJob(ManagedJob job, Map<String, ?> properties) {
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
						log.info("Job [{}] registered managed service {} as {} with props {}",
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
		if ( configurationListenerRef == null ) {
			configurationListenerRef = bundleContext.registerService(ConfigurationListener.class, this,
					null);
		}

		ScheduledJobs sjs = pidMap.computeIfAbsent(pid, k -> new ScheduledJobs());
		sjs.addJob(sj);
		if ( sjs.settingProviderReg == null ) {
			Dictionary<String, Object> settingProps = new Hashtable<>();
			if ( properties.containsKey(SERVICE_PID) ) {
				settingProps.put(SERVICE_PID, properties.get(SERVICE_PID));
			}
			settingProps.put(SETTING_PID, job.getSettingUid());
			sjs.settingProviderReg = bundleContext.registerService(SettingSpecifierProvider.class, sjs,
					settingProps);
		}
		if ( isSchedulerActive() ) {
			scheduleJob(sj);
		}
	}

	private void scheduleJob(final ScheduledJob sj) {
		String expr = sj.job.getSchedule();
		Trigger trigger = triggerForSchedule(expr);
		if ( trigger != null ) {
			ScheduledFuture<?> f = taskScheduler.schedule(sj, trigger);
			sj.scheduled(f, expr, trigger);
		}
	}

	private boolean isSchedulerActive() {
		return (jobStartDelaySeconds < 1 || (startTime > 0 && startTime < System.currentTimeMillis()));
	}

	/**
	 * Unregister a job.
	 *
	 * @param job
	 *        the job to unregister and un-schedule for execution
	 * @param properties
	 *        optional service properties
	 */
	public synchronized void unregisterJob(ManagedJob job, Map<String, ?> properties) {
		if ( job == null ) {
			return;
		}
		final String pid = servicePid(properties, job.getSettingUid());
		if ( pid == null ) {
			return;
		}
		log.debug("Unregister job [{}] with props {}", pid, properties);
		ScheduledJob sj = null;
		ScheduledJobs sjs = pidMap.get(pid);
		if ( sjs != null ) {
			sj = sjs.removeJob(job.getUid());
			if ( sjs.isEmpty() ) {
				pidMap.remove(pid);
			}
		}
		if ( sj == null ) {
			log.warn("Attempted to unregister job [{}] that wasn't registered", pid);
			return;
		}
		sj.stop();
	}

	@Override
	public synchronized void configurationEvent(ConfigurationEvent event) {
		if ( event.getType() != ConfigurationEvent.CM_UPDATED ) {
			return;
		}
		final String pid = event.getPid();
		List<ScheduledJob> sjList = new ArrayList<>(1);
		ScheduledJobs sjs = pidMap.get(pid);
		if ( sjs != null ) {
			sjList.addAll(sjs.jobMap.values());
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
			final String oldSchedule = sj.schedule;
			String newSchedule = null;
			Object configScheduleExpression = props.get(sj.job.getScheduleSettingKey());
			if ( configScheduleExpression != null ) {
				newSchedule = configScheduleExpression.toString();
			}

			if ( isSchedulerActive() && newSchedule != null
					&& (oldSchedule == null || !oldSchedule.equalsIgnoreCase(newSchedule)) ) {
				Trigger trigger = triggerForSchedule(newSchedule);
				if ( trigger != null ) {
					sj.stop(true);
					ScheduledFuture<?> f = taskScheduler.schedule(sj, trigger);
					sj.scheduled(f, newSchedule, trigger);
				} else if ( sj.future != null ) {
					sj.stop();
					log.info("Stopped job [{}]", sj.identifier, oldSchedule, newSchedule);
				}
			}
		}
	}

	private Trigger triggerForSchedule(String schedule) {
		return triggerForExpression(schedule, TimeUnit.MILLISECONDS, randomizedCron);
	}

	private static String servicePid(Map<String, ?> properties, String defaultPid) {
		Object o = properties.get(SERVICE_PID);
		return (o != null ? o.toString() : defaultPid);
	}

	@Override
	public String getPingTestId() {
		return "net.solarnetwork.node.runtime.ManagedJobScheduler";
	}

	@Override
	public String getPingTestName() {
		return "Managed Job Scheduler";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 2000;
	}

	@Override
	public synchronized Result performPingTest() throws Exception {
		boolean ok = true;
		Map<String, String> errors = new TreeMap<>();
		Map<String, String> all = new TreeMap<>();
		for ( Entry<String, ScheduledJobs> e : pidMap.entrySet() ) {
			ScheduledJobs sjs = e.getValue();
			for ( Entry<String, ScheduledJob> sje : sjs.jobMap.entrySet() ) {
				ScheduledJob sj = sje.getValue();
				final String ident = sj.identifier;
				final Throwable t = sj.throwable;
				if ( t != null ) {
					Throwable root = t;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					errors.put(ident, root.toString());
				}
				all.put(ident, String.format("%s @ %s", sj.identifier, sj.schedule));
			}
		}
		Map<String, Object> props = new LinkedHashMap<>();
		if ( !errors.isEmpty() ) {
			props.put("errors", errors);
		}
		if ( !all.isEmpty() ) {
			props.put("jobs", all);
		}
		String msg = messageSource.getMessage("msg.jobCountStatus",
				new Object[] { all.size(), errors.size() }, "Scheduler running.", Locale.getDefault());
		return new PingTestResult(ok, msg, props);
	}

	/**
	 * Get the randomized cron flag.
	 *
	 * @return {@literal true} to randomize the second property of jobs using
	 *         cron schedules; defaults to {@link #DEFAULT_RANDOMIZED_CRON}
	 */
	public boolean isRandomizedCron() {
		return randomizedCron;
	}

	/**
	 * Set the randomized cron flag.
	 *
	 * @param randomizedCron
	 *        {@literal true} to randomize the second property of jobs using
	 *        cron schedules
	 */
	public void setRandomizedCron(boolean randomizedCron) {
		this.randomizedCron = randomizedCron;
	}

	/**
	 * Get the message source.
	 *
	 * @return the message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set the message source.
	 *
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the number of seconds to delay the first execution of a registered
	 * job.
	 *
	 * @return the number of seconds to delay the first execution of a
	 *         registered job; defaults to {@link #DEFAULT_JOB_START_DELAY_SECS}
	 */
	public int getJobStartDelaySeconds() {
		return jobStartDelaySeconds;
	}

	/**
	 * Set the number of seconds to delay the first execution of a registered
	 * job.
	 *
	 * @param jobStartDelaySeconds
	 *        the number of seconds to delay the first execution of a registered
	 *        job
	 */
	public void setJobStartDelaySeconds(int jobStartDelaySeconds) {
		this.jobStartDelaySeconds = jobStartDelaySeconds;
	}

}
