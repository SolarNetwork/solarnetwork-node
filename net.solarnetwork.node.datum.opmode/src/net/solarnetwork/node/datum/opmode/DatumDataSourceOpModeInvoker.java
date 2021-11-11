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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.job.JobUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.ServiceLifecycleObserver;
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
public class DatumDataSourceOpModeInvoker extends BasicIdentifiable implements SettingSpecifierProvider,
		DatumDataSourceScheduleService, EventHandler, ServiceLifecycleObserver {

	/**
	 * The group name used to schedule the invoker jobs as.
	 */
	public static final String DATUM_DATA_SOURCE_INVOKER_JOB_NAME = "DatumDataSourceInvoker";

	/**
	 * The group name used to schedule the invoker jobs as.
	 */
	public static final String DATUM_DATA_SOURCE_INVOKER_JOB_GROUP = "DatumDataSourceInvoker";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OperationalModesService opModesService;
	private final OptionalService<DatumQueue> datumQueue;
	private final OptionalServiceCollection<DatumDataSource> dataSources;
	private final OptionalServiceCollection<MultiDatumDataSource> multiDataSources;

	private String operationalMode;
	private TaskScheduler scheduler;
	private DatumDataSourceScheduleConfig[] configurations;
	private TaskExecutor taskExecutor;

	private final ConcurrentMap<Integer, ScheduledDatumDataSourceConfig> activeConfigurations = new ConcurrentHashMap<>(
			16, 0.9f, 1);

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the task scheduler
	 * @param opModesService
	 *        the op modes service
	 * @param datumQueue
	 *        the queue
	 * @param dataSources
	 *        the data sources
	 * @param multiDataSources
	 *        the multi data sources
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumDataSourceOpModeInvoker(TaskScheduler scheduler, OperationalModesService opModesService,
			OptionalService<DatumQueue> datumQueue,
			OptionalServiceCollection<DatumDataSource> dataSources,
			OptionalServiceCollection<MultiDatumDataSource> multiDataSources) {
		super();
		this.opModesService = requireNonNullArgument(opModesService, "opModesService");
		this.scheduler = requireNonNullArgument(scheduler, "scheduler");
		this.datumQueue = requireNonNullArgument(datumQueue, "datumQueue");
		this.dataSources = requireNonNullArgument(dataSources, "dataSources");
		this.multiDataSources = requireNonNullArgument(multiDataSources, "multiDataSources");
	}

	@Override
	public synchronized void serviceDidStartup() {
		String myOpMode = (this.operationalMode != null ? this.operationalMode.toLowerCase() : null);
		if ( myOpMode == null || myOpMode.isEmpty() ) {
			return;
		}
		Set<String> active = opModesService.activeOperationalModes();
		handleActivation(active, myOpMode);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		String myOpMode = (this.operationalMode != null ? this.operationalMode.toLowerCase() : null);
		if ( myOpMode == null || myOpMode.isEmpty() ) {
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
		handleActivation(activeOpModes, myOpMode);
	}

	private void handleActivation(final Set<String> activeOpModes, final String myOpMode) {
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
		int i = 0;
		for ( DatumDataSourceScheduleConfig config : configs ) {
			final String schedule = config.getSchedule();
			if ( schedule == null || schedule.isEmpty() ) {
				log.debug("Config {} has no schedule: cannot activate", i);
				continue;
			}

			final Trigger trigger = JobUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);
			if ( trigger != null ) {
				ScheduledFuture<?> future = scheduler
						.schedule(new DatumDataSourceInvokerJob(this, config), trigger);
				activeConfigurations.put(++i, new ScheduledDatumDataSourceConfig(config, future));
				log.info(
						"Scheduled operational mode [{}] config {} data source collection using schedule [{}]",
						operationalMode, i, schedule);
			}
		}
	}

	private synchronized void deactivate() {
		for ( ScheduledDatumDataSourceConfig config : activeConfigurations.values() ) {
			config.getTask().cancel(true);
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
		final boolean persist = config.isPersist();
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
					queue.offer(datum, persist);
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
							queue.offer(datum, persist);
						}
					}
				}
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
	public void setScheduler(TaskScheduler scheduler) {
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
