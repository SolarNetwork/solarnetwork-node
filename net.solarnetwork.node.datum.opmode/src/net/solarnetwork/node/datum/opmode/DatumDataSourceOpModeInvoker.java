/* ==================================================================
 * DatumDataSourceOpModeInvoker.java - 20/12/2018 1:46:23 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.opmode;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.quartz.CronTrigger;
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
import org.springframework.core.task.TaskExecutor;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Poll a set of {@link DatumDataSource} services at a configurable frequency
 * when an operational mode is active.
 * 
 * <p>
 * This service is configured with a <em>target</em> operational mode and a set
 * of configurations that each define a source ID filter and associated
 * schedule. When the target mode becomes active, it will schedule jobs for each
 * configuration to poll from all matching {@link DatumDataSource} service,
 * passing all returned datum to {@link DatumQueue#offer(NodeDatum, boolean)}
 * with {@code persisted} set to {@literal false}.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class DatumDataSourceOpModeInvoker extends BasicIdentifiable
		implements SettingSpecifierProvider, DatumDataSourceScheduleService, EventHandler {

	/**
	 * The group name used to schedule the invoker jobs as.
	 */
	public static final String DATUM_DATA_SOURCE_INVOKER_JOB_NAME = "DatumDataSourceInvoker";

	/**
	 * The group name used to schedule the invoker jobs as.
	 */
	public static final String DATUM_DATA_SOURCE_INVOKER_JOB_GROUP = "DatumDataSourceInvoker";

	private static final JobKey JOB_KEY = new JobKey(DATUM_DATA_SOURCE_INVOKER_JOB_NAME,
			DATUM_DATA_SOURCE_INVOKER_JOB_GROUP);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalService<DatumQueue> datumQueue;
	private final OptionalServiceCollection<DatumDataSource> dataSources;
	private final OptionalServiceCollection<MultiDatumDataSource> multiDataSources;

	private String operationalMode;
	private Scheduler scheduler;
	private DatumDataSourceScheduleConfig[] configurations;
	private TaskExecutor taskExecutor;

	private final ConcurrentMap<Integer, ScheduledDatumDataSourceConfig> activeConfigurations = new ConcurrentHashMap<>(
			16, 0.9f, 1);

	/**
	 * Constructor.
	 * 
	 * @param datumQueue
	 *        the queue
	 * @param dataSources
	 *        the data sources
	 * @param multiDataSources
	 *        the multi data sources
	 */
	public DatumDataSourceOpModeInvoker(OptionalService<DatumQueue> datumQueue,
			OptionalServiceCollection<DatumDataSource> dataSources,
			OptionalServiceCollection<MultiDatumDataSource> multiDataSources) {
		super();
		this.datumQueue = datumQueue;
		this.dataSources = dataSources;
		this.multiDataSources = multiDataSources;
	}

	/**
	 * Call when this service is no longer needed to clean up resources.
	 */
	public void shutdown() {
		deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		String myOpMode = (this.operationalMode != null ? this.operationalMode.toLowerCase() : null);
		if ( myOpMode == null ) {
			return;
		}
		String topic = event.getTopic();
		if ( !OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED.equalsIgnoreCase(topic) ) {
			return;
		}
		Object opModes = event.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES);
		if ( !(opModes instanceof Set<?>) ) {
			return;
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Set<String> activeOpModes = (Set) opModes;
		boolean active = activeOpModes.contains(myOpMode);
		log.info("DatumDataSource scheduler config [{}] operational mode [{}] {}",
				getUid() != null ? getUid() : this.toString(), myOpMode, active ? "active" : "inactive");
		Runnable task = new Runnable() {

			@Override
			public void run() {
				if ( active && activeConfigurations.isEmpty() ) {
					activate(getConfigurations());
				} else if ( !active && !activeConfigurations.isEmpty() ) {
					deactivate();
				}
			}
		};
		if ( taskExecutor != null ) {
			taskExecutor.execute(task);
		} else {
			task.run();
		}
	}

	private synchronized void activate(DatumDataSourceScheduleConfig[] configs) {
		if ( configs == null || configs.length < 1 ) {
			return;
		}
		String jobDesc = "DatumDataSource samples for operational mode " + operationalMode;
		int i = 0;
		for ( DatumDataSourceScheduleConfig config : configs ) {
			if ( config.getSchedule() == null || config.getSchedule().isEmpty() ) {
				log.debug("Config {} has no schedule: cannot activate", i);
				continue;
			}
			JobDataMap props = new JobDataMap();
			props.put("config", config);
			props.put("service", this);
			final TriggerKey triggerKey = new TriggerKey(UUID.randomUUID().toString(),
					DATUM_DATA_SOURCE_INVOKER_JOB_GROUP);
			Trigger trigger = null;
			try {
				int freq = Integer.parseInt(config.getSchedule());
				log.info("Scheduling config {} data source collection for every {}s as {}", i, freq,
						triggerKey);
				trigger = scheduleIntervalJob(scheduler, freq, triggerKey, props, jobDesc);
			} catch ( NumberFormatException e ) {
				// assume cron
				log.info("Scheduling config {} data source collection for cron [{}] as {}", i,
						config.getSchedule(), triggerKey);
				trigger = scheduleCronJob(scheduler, config.getSchedule(), triggerKey, props, jobDesc);
			}
			activeConfigurations.put(++i, new ScheduledDatumDataSourceConfig(config, trigger));
		}
	}

	private synchronized void deactivate() {
		for ( ScheduledDatumDataSourceConfig config : activeConfigurations.values() ) {
			try {
				scheduler.unscheduleJob(config.getTrigger().getKey());
			} catch ( SchedulerException e ) {
				log.error("Error unscheduling {} job", config.getTrigger().getKey(), e);
			}
		}
		activeConfigurations.clear();
	}

	private String displayNameForIdentifiable(Identifiable identifiable) {
		return (identifiable != null && identifiable.getUid() != null && !identifiable.getUid().isEmpty()
				? identifiable.getUid()
				: identifiable != null ? identifiable.toString() : null);
	}

	@Override
	public void invokeScheduleConfig(DatumDataSourceScheduleConfig config) {
		if ( config == null ) {
			return;
		}
		final DatumQueue queue = OptionalService.service(datumQueue);
		Set<Object> handled = new HashSet<>();
		if ( dataSources != null ) {
			for ( DatumDataSource dataSource : dataSources.services() ) {
				if ( handled.contains(dataSource) ) {
					continue;
				}
				handled.add(dataSource);
				String dsName = displayNameForIdentifiable(dataSource);
				log.trace("Inspecting DatumDataSource {} against config {}", dsName, config);
				if ( config.matches(dataSource) ) {
					NodeDatum datum = dataSource.readCurrentDatum();
					log.debug("Invoked DatumDataSource {} and got {}", dsName, datum);
					queue.offer(datum, false);
				}
			}
		}
		if ( multiDataSources != null ) {
			for ( MultiDatumDataSource dataSource : multiDataSources.services() ) {
				if ( handled.contains(dataSource) ) {
					continue;
				}
				handled.add(dataSource);
				String dsName = displayNameForIdentifiable(dataSource);
				log.trace("Inspecting MultiDatumDataSource {} against config {}", dsName, config);
				if ( config.matches(dataSource) ) {
					Collection<NodeDatum> datums = dataSource.readMultipleDatum();
					log.debug("Invoked MultiDatumDataSource {} and got {}", dsName, datums);
					if ( datums != null ) {
						for ( NodeDatum datum : datums ) {
							queue.offer(datum, false);
						}
					}
				}
			}
		}
	}

	private JobDetail getJobDetail(final Scheduler scheduler) throws SchedulerException {
		JobDetail jobDetail = scheduler.getJobDetail(JOB_KEY);
		if ( jobDetail == null ) {
			jobDetail = JobBuilder.newJob(DatumDataSourceInvokerJob.class).withIdentity(JOB_KEY)
					.storeDurably().build();
			scheduler.addJob(jobDetail, true);
		}
		return jobDetail;
	}

	private SimpleTrigger scheduleIntervalJob(final Scheduler scheduler, final int interval,
			final TriggerKey triggerKey, final JobDataMap jobData, final String jobDescription) {
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
				trigger = TriggerBuilder.newTrigger().withIdentity(trigger.getKey()).forJob(JOB_KEY)
						.startAt(new Date(System.currentTimeMillis() + interval)).usingJobData(jobData)
						.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(interval)).build();
				try {
					scheduler.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling {} job", jobDescription, e);
				}
				return trigger;
			}

			try {
				JobDetail jobDetail = getJobDetail(scheduler);
				trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobDetail.getKey())
						.startAt(new Date(
								System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(interval)))
						.usingJobData(jobData)
						.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(interval)
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

	private CronTrigger scheduleCronJob(final Scheduler scheduler, final String cronExpression,
			final TriggerKey triggerKey, final JobDataMap jobData, final String jobDescription) {
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
				trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(JOB_KEY)
						.startAt(new Date(System.currentTimeMillis())).usingJobData(jobData)
						.withSchedule(org.quartz.CronScheduleBuilder.cronSchedule(cronExpression)
								.withMisfireHandlingInstructionDoNothing())
						.build();
				try {
					scheduler.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling {} job", jobDescription, e);
				}
				return trigger;
			}

			try {
				JobDetail jobDetail = getJobDetail(scheduler);
				trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobDetail.getKey())
						.startAt(new Date(System.currentTimeMillis())).usingJobData(jobData)
						.withSchedule(org.quartz.CronScheduleBuilder.cronSchedule(cronExpression)
								.withMisfireHandlingInstructionDoNothing())
						.build();
				scheduler.scheduleJob(trigger);
				return trigger;
			} catch ( Exception e ) {
				log.error("Error scheduling {} job", jobDescription, e);
				return null;
			}
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.opmode.invoker";
	}

	@Override
	public String getDisplayName() {
		return "Datum Data Source Operational Mode Scheduler";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTitleSettingSpecifier("status", getStatusMessage(), true));
		results.addAll(basicIdentifiableSettings());
		results.add(new BasicTextFieldSettingSpecifier("operationalMode", null));

		DatumDataSourceScheduleConfig[] confs = getConfigurations();
		List<DatumDataSourceScheduleConfig> confsList = (confs != null ? asList(confs) : emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("configurations", confsList,
				new SettingUtils.KeyedListCallback<DatumDataSourceScheduleConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							DatumDataSourceScheduleConfig value, int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								DatumDataSourceScheduleConfig.settings(key + "."));
						return singletonList(configGroup);
					}
				}));

		return results;
	}

	private String getStatusMessage() {
		if ( activeConfigurations.isEmpty() ) {
			return "Not active";
		}
		return "Active";
	}

	/**
	 * Get the operational mode the settings apply to.
	 * 
	 * @return the operational mode
	 */
	public String getOperationalMode() {
		return operationalMode;
	}

	/**
	 * Set the operational mode the settings apply to.
	 * 
	 * @param operationalMode
	 *        the operational mode
	 */
	public void setOperationalMode(String operationalMode) {
		this.operationalMode = operationalMode;
	}

	/**
	 * Get the configurations to use.
	 * 
	 * @return the configurations
	 */
	public synchronized DatumDataSourceScheduleConfig[] getConfigurations() {
		return configurations;
	}

	/**
	 * Set the configurations to use.
	 * 
	 * @param configurations
	 *        the configurations
	 */
	public synchronized void setConfigurations(DatumDataSourceScheduleConfig[] configurations) {
		this.configurations = configurations;
	}

	/**
	 * Get the number of configured {@code configurations} elements.
	 * 
	 * @return the number of {@code configurations} elements
	 */
	public synchronized int getConfigurationsCount() {
		DatumDataSourceScheduleConfig[] confs = this.configurations;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code configuration} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link DatumDataSourceScheduleConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code configurations} elements.
	 */
	public synchronized void setConfigurationsCount(int count) {
		this.configurations = ArrayUtils.arrayWithLength(this.configurations, count,
				DatumDataSourceScheduleConfig.class, null);
	}

	/**
	 * Set the Scheduler to use.
	 * 
	 * @param scheduler
	 *        The scheduler to use.
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * An executor to handle events with.
	 * 
	 * @param taskExecutor
	 *        a task executor
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
