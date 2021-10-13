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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.job.ManagedJob;
import net.solarnetwork.node.job.ServiceProvider;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Service to dynamically register and schedule {@link ManagedJob} instances.
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
	private final Map<String, ScheduledJob> pidMap = new HashMap<>();

	private long jobStartDelayMs = DEFAULT_JOB_START_DELAY_MS;

	private ServiceRegistration<ConfigurationListener> configurationListenerRef;

	private static class ScheduledJob implements Runnable {

		private final String pid;
		private final ManagedJob job;
		private final List<ServiceRegistration<?>> registeredServices;
		private ScheduledFuture<?> future;
		private String triggerScheduleExpression;

		private ScheduledJob(String pid, ManagedJob job,
				List<ServiceRegistration<?>> registeredServices) {
			super();
			this.pid = requireNonNullArgument(pid, "pid");
			this.job = requireNonNullArgument(job, "job");
			this.registeredServices = registeredServices;
		}

		private synchronized void scheduled(ScheduledFuture<?> future,
				String triggerScheduleExpression) {
			this.future = future;
			this.triggerScheduleExpression = triggerScheduleExpression;
		}

		private synchronized void stop() {
			if ( future != null && !future.isDone() ) {
				future.cancel(true);
				log.debug("Unregistered job [{}]", pid);
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
					log.error("Error executing job {}: {}", job.getName(), t.getMessage());
				}
			}

		}

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
		log.debug("Register job [{}] with props {}", job.getSettingUid(), properties);
		final String pid = servicePid(properties);

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
						log.debug("Registering managed service {} as {} with props {}", service,
								Arrays.toString(interfaces), props);
						if ( refs == null ) {
							refs = new ArrayList<>(services.size());
						}
						refs.add(ref);
					} catch ( Exception e ) {
						log.error("Error registering managed service {} as {} with props {}: {}",
								service, Arrays.toString(interfaces), props, e.toString());
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

			pidMap.put(pid, sj);
		}

		String expr = job.getTriggerScheduleExpression();
		Trigger trigger = net.solarnetwork.node.job.JobUtils.triggerForExpression(expr);
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
		final String pid = servicePid(properties);
		log.debug("Unregister job [{}] with props {}", pid, properties);
		ScheduledJob sj = null;
		synchronized ( pidMap ) {
			sj = pidMap.remove(pid);
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
						log.info("Unregistered job [{}] managed service {}", pid, reg);
					} catch ( Exception e ) {
						log.warn("Error unregistering job [{}] managed service {}: {}", pid, reg,
								e.toString());
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
		ScheduledJob sj = null;
		synchronized ( pidMap ) {
			sj = pidMap.get(pid);
		}
		if ( sj == null ) {
			return;
		}

		synchronized ( sj ) {
			final String oldSchedule = sj.triggerScheduleExpression;
			String newSchedule = null;

			// even though the cron expression is also updated by ConfigurationAdmin, it can happen in a different thread
			// so it might not be updated yet so we must extract the current value from ConfigurationAdmin
			ServiceReference<ConfigurationAdmin> caRef = event.getReference();
			ConfigurationAdmin ca = bundleContext.getService(caRef);
			try {
				Configuration config = ca.getConfiguration(pid, null);
				Dictionary<String, ?> props = config.getProperties();

				// first look for expression on common attribute names
				String configScheduleExpression = (String) props.get("triggerScheduleExpression");

				if ( configScheduleExpression == null ) {
					// if cron expression not already found, search for any key containing "cronexpression"
					Enumeration<String> keyEnum = props.keys();
					while ( keyEnum.hasMoreElements() ) {
						String key = keyEnum.nextElement();

						if ( key.toLowerCase().contains("cronexpression") ) {
							configScheduleExpression = (String) props.get(key);
							break;
						}
					}
				}

				if ( configScheduleExpression != null ) {
					newSchedule = configScheduleExpression;
				}
			} catch ( IOException e ) {
				log.warn("Exception processing job [{}] configuration update event", pid, e);
			}
			if ( !oldSchedule.equalsIgnoreCase(newSchedule) ) {
				Trigger trigger = net.solarnetwork.node.job.JobUtils.triggerForExpression(newSchedule);
				if ( trigger != null ) {
					ScheduledFuture<?> f = taskScheduler.schedule(sj, trigger);
					sj.scheduled(f, newSchedule);
				} else if ( sj.future != null ) {
					sj.stop();
				}
			}
		}
	}

	private static String servicePid(Map<String, ?> properties) {
		Object o = properties.get(Constants.SERVICE_PID);
		return (o != null ? o.toString() : null);
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
