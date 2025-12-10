/* ==================================================================
 * JoinDatumFilterService.java - 17/02/2022 8:35:38 AM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.std;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumSourceIdProvider;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Datum filter service that joins multiple datum into a new datum stream.
 *
 * @author matt
 * @version 1.4
 */
public class JoinDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, SettingsChangeObserver,
		DatumSourceIdProvider, ServiceLifecycleObserver, Runnable {

	/** The template parameter name for a property name to be mapped. */
	public static final String PROPERTY_NAME_PARAMETER_KEY = "p";

	private static final String PROPERTY_NAME_PARAMETER = "{p}";

	/** The {@code coalesceThreshold} property default value. */
	public static final int DEFAULT_COALESCE_THRESHOLD = 1;

	/** The {@code swallowInput} property default value. */
	public static final boolean DEFAULT_SWALLOW_INPUT = false;

	/**
	 * The {@code persist} property default value.
	 *
	 * @since 1.3
	 */
	public static final boolean DEFAULT_PERSIST = true;

	private final DatumSamples mergedSamples = new DatumSamples();
	private final Set<String> coalescedSourceIds = new HashSet<>(4, 0.9f);

	private final OptionalService<DatumQueue> datumQueue;
	private final InstantSource sampleClock;
	private String outputSourceId;
	private int coalesceThreshold = DEFAULT_COALESCE_THRESHOLD;
	private Duration coalesceTimeout;
	private boolean swallowInput = DEFAULT_SWALLOW_INPUT;
	private boolean persist = DEFAULT_PERSIST;
	private PatternKeyValuePair[] propertySourceMappings;
	private TaskScheduler taskScheduler;
	private int startupDelay;

	private ScheduledFuture<?> startupFuture;
	private ScheduledFuture<?> coalesceFuture;

	/**
	 * Constructor.
	 *
	 * @param datumQueue
	 *        the datum queue
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JoinDatumFilterService(OptionalService<DatumQueue> datumQueue) {
		this(Clock.systemUTC(), datumQueue);
	}

	/**
	 * Constructor.
	 *
	 * @param sampleClock
	 *        the sample clock
	 * @param datumQueue
	 *        the datum queue
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JoinDatumFilterService(InstantSource sampleClock, OptionalService<DatumQueue> datumQueue) {
		super();
		this.sampleClock = requireNonNullArgument(sampleClock, "sampleClock");
		this.datumQueue = requireNonNullArgument(datumQueue, "datumQueue");
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( startupFuture != null || coalesceFuture != null ) {
			log.info("Restarting Join Filter [{}] from configuration change", getUid());
		}
		restartFilter(properties == null ? startupDelay : 0);
	}

	@Override
	public synchronized void serviceDidStartup() {
		restartFilter(startupDelay);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		stop();
	}

	/**
	 * Start the filter.
	 */
	public synchronized void start() {
		if ( startupFuture != null ) {
			return;
		}
		try {
			resetCoalesceTimeout();
		} catch ( Exception e ) {
			String msg = String.format("Error starting Join Filter [%s]", getUid());
			log.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * Shut down the filter.
	 */
	public synchronized void stop() {
		if ( startupFuture != null && !startupFuture.isDone() ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		if ( coalesceFuture != null && !coalesceFuture.isDone() ) {
			coalesceFuture.cancel(true);
			coalesceFuture = null;
		}
	}

	/**
	 * Get the effective coalesce timeout.
	 *
	 * @return the timeout, or {@code null} if not applicable
	 */
	private Duration effectiveCoalesceTimeout() {
		final Duration coalesceDuration = getCoalesceTimeout();
		if ( taskScheduler == null || coalesceThreshold < 2 || coalesceDuration == null
				|| coalesceDuration.compareTo(Duration.ZERO) <= 0 ) {
			return null;
		}
		return coalesceDuration;
	}

	private synchronized void restartFilter(final int startupDelay) {
		stop();
		Runnable startupTask = new Runnable() {

			@Override
			public void run() {
				synchronized ( JoinDatumFilterService.this ) {
					startupFuture = null;
					start();
				}
			}
		};
		if ( taskScheduler != null ) {
			startupFuture = taskScheduler.schedule(startupTask,
					Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(startupDelay));
		} else {
			startupTask.run();
		}
	}

	private void resetCoalesceTimeout() {
		final Duration coalesceDuration = effectiveCoalesceTimeout();
		if ( coalesceDuration == null ) {
			return;
		}
		synchronized ( this ) {
			if ( startupFuture != null && !startupFuture.isDone() ) {
				startupFuture.cancel(true);
				startupFuture = null;
			}
			if ( coalesceFuture != null && !coalesceFuture.isDone() ) {
				coalesceFuture.cancel(true);
				coalesceFuture = null;
			}
			coalesceFuture = taskScheduler.schedule(this, Instant.now().plus(coalesceDuration));
		}
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final String outputSourceId = resolvePlaceholders(getOutputSourceId(), parameters);
		if ( outputSourceId == null || outputSourceId.trim().isEmpty()
				|| outputSourceId.equals(datum.getSourceId()) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		resetCoalesceTimeout();
		final String[] propSourceMapping = propertySourceMapping(datum);
		synchronized ( mergedSamples ) {
			coalescedSourceIds.add(datum.getSourceId());
			if ( propSourceMapping == null ) {
				// simple case
				mergedSamples.mergeFrom(samples);
			} else {
				// mapping case
				for ( DatumSamplesType type : DatumSamplesType.values() ) {
					if ( type != DatumSamplesType.Tag ) {
						Map<String, ?> data = samples.getSampleData(type);
						if ( data != null && !data.isEmpty() ) {
							for ( Entry<String, ?> e : data.entrySet() ) {
								String p = PatternKeyValuePair.expand(e.getKey(), propSourceMapping);
								mergedSamples.putSampleValue(type, p, e.getValue());
							}
						}
					}
				}
			}
			if ( coalescedSourceIds.size() >= coalesceThreshold ) {
				generateDatum(datum.getTimestamp());
			}
		}
		DatumSamplesOperations result = (swallowInput ? null : samples);
		incrementStats(start, samples, result);
		return result;
	}

	/**
	 * Generate a datum out of the collected samples.
	 */
	@Override
	public void run() {
		synchronized ( mergedSamples ) {
			generateDatum(sampleClock.instant());
		}
		final Duration coalesceDuration = effectiveCoalesceTimeout();
		if ( coalesceDuration == null ) {
			return;
		}
		coalesceFuture = taskScheduler.schedule(this, now().plus(coalesceDuration));
	}

	private void generateDatum(Instant timestamp) {
		SimpleDatum d = SimpleDatum.nodeDatum(outputSourceId, timestamp != null ? timestamp : now(),
				new DatumSamples(mergedSamples));
		log.debug("Generated merged datum {}", d);
		DatumQueue dq = service(datumQueue);
		if ( dq != null ) {
			dq.offer(d, persist);
		}
		coalescedSourceIds.clear();
		if ( coalesceThreshold > 1 ) {
			mergedSamples.clear();
		}
	}

	/**
	 * Return an array with the property mapping value an optional regular
	 * expression parameter values.
	 *
	 * @param datum
	 *        the datum to find the property source mapping for
	 * @return the mapping data, or {@literal null}
	 */
	private String[] propertySourceMapping(Datum datum) {
		final String inputSourceId = datum.getSourceId();
		final PatternKeyValuePair[] mappings = getPropertySourceMappings();
		if ( mappings != null && mappings.length > 0 ) {
			for ( PatternKeyValuePair mapping : mappings ) {
				String t = mapping.getValue();
				if ( t == null || t.isEmpty() || !t.contains(PROPERTY_NAME_PARAMETER) ) {
					continue;
				}
				String[] result = mapping.keyMatches(inputSourceId);
				if ( result != null ) {
					result[0] = t;
					return result;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = getOutputSourceId();
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.join";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return settingSpecifiers(false);
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return settingSpecifiers(true);
	}

	private List<SettingSpecifier> settingSpecifiers(final boolean template) {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);
		populateStatusSettings(result);

		result.add(new BasicTextFieldSettingSpecifier("outputSourceId", null));

		result.add(new BasicTextFieldSettingSpecifier("coalesceThreshold",
				String.valueOf(DEFAULT_COALESCE_THRESHOLD)));
		result.add(new BasicTextFieldSettingSpecifier("coalesceTimeoutMillis", null));

		result.add(new BasicToggleSettingSpecifier("swallowInput", DEFAULT_SWALLOW_INPUT));
		result.add(new BasicToggleSettingSpecifier("persist", DEFAULT_PERSIST));

		PatternKeyValuePair[] mappingConfs = getPropertySourceMappings();
		List<PatternKeyValuePair> mappingConfList = (template
				? Collections.singletonList(new PatternKeyValuePair())
				: (mappingConfs != null ? asList(mappingConfs) : emptyList()));
		result.add(SettingUtils.dynamicListSettingSpecifier("propertySourceMappings", mappingConfList,
				new SettingUtils.KeyedListCallback<PatternKeyValuePair>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(PatternKeyValuePair value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								PatternKeyValuePair.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the source coalesce threshold.
	 *
	 * @return the threshold; defaults to {@link #DEFAULT_COALESCE_THRESHOLD}
	 */
	public int getCoalesceThreshold() {
		return coalesceThreshold;
	}

	/**
	 * Set the source coalesce threshold.
	 *
	 * @param coalesceThreshold
	 *        the coalesceThreshold to set
	 */
	public void setCoalesceThreshold(int coalesceThreshold) {
		this.coalesceThreshold = coalesceThreshold;
	}

	/**
	 * Get the "swallow input" mode.
	 *
	 * @return {@literal true} if input datum should be discarded after merging
	 *         their properties into the output stream, {@literal false} to
	 *         leave input datum unchanged; defaults to
	 *         {@link #DEFAULT_SWALLOW_INPUT}
	 */
	public boolean isSwallowInput() {
		return swallowInput;
	}

	/**
	 * Set the "swallow input" mode.
	 *
	 * @param swallowInput
	 *        {@literal true} if input datum should be discarded after merging
	 *        their properties into the output stream, {@literal false} to leave
	 *        input datum unchanged
	 */
	public void setSwallowInput(boolean swallowInput) {
		this.swallowInput = swallowInput;
	}

	/**
	 * Get the property source mappings.
	 *
	 * @return the mappings, or {@literal null}
	 */
	public PatternKeyValuePair[] getPropertySourceMappings() {
		return propertySourceMappings;
	}

	/**
	 * Set the property source mappings.
	 *
	 * @param propertySourceMappings
	 *        the mappings to set
	 */
	public void setPropertySourceMappings(PatternKeyValuePair[] propertySourceMappings) {
		this.propertySourceMappings = propertySourceMappings;
	}

	/**
	 * Get the number of configured property source mappings.
	 *
	 * @return the number of property source mappings
	 */
	public int getPropertySourceMappingsCount() {
		final PatternKeyValuePair[] mappings = getPropertySourceMappings();
		return (mappings != null ? mappings.length : 0);
	}

	/**
	 * Set the number of configured property source mappings.
	 *
	 * @param count
	 *        the number of mappings to set
	 */
	public void setPropertySourceMappingsCount(int count) {
		setPropertySourceMappings(ArrayUtils.arrayWithLength(getPropertySourceMappings(), count,
				PatternKeyValuePair.class, PatternKeyValuePair::new));
	}

	/**
	 * Get the generated datum source ID.
	 *
	 * @return the source ID to use for generated datum
	 */
	public String getOutputSourceId() {
		return outputSourceId;
	}

	/**
	 * Set the generated datum source ID.
	 *
	 * @param outputSourceId
	 *        the source ID to use for generated datum
	 */
	public void setOutputSourceId(String outputSourceId) {
		this.outputSourceId = outputSourceId;
	}

	/**
	 * Get the persist mode.
	 *
	 * @return {@code true} to persist the output datum
	 * @since 1.3
	 */
	public boolean isPersist() {
		return persist;
	}

	/**
	 * Set the persist mode.
	 *
	 * <p>
	 * This affects how the generated datum are offered to the
	 * {@link DatumQueue}.
	 * </p>
	 *
	 * @param persist
	 *        {@code true} to persist the output datum
	 * @since 1.3
	 */
	public void setPersist(boolean persist) {
		this.persist = persist;
	}

	/**
	 * Get the coalesce timeout.
	 *
	 * @return the coalesce timeout
	 * @since 1.4
	 */
	public Duration getCoalesceTimeout() {
		return coalesceTimeout;
	}

	/**
	 * Set the coalesce timeout.
	 *
	 * @param coalesceTimeout
	 *        the coalesce timeout to set
	 * @since 1.4
	 */
	public void setCoalesceTimeout(Duration coalesceTimeout) {
		this.coalesceTimeout = coalesceTimeout;
	}

	/**
	 * Get the coalesce timeout as milliseconds.
	 *
	 * @return the coalesce timeout, in milliseconds
	 * @since 1.4
	 */
	public long getCoalesceTimeoutMillis() {
		final Duration timeout = getCoalesceTimeout();
		return timeout != null ? timeout.toMillis() : 0L;
	}

	/**
	 * Set the coalesce timeout as milliseconds.
	 *
	 * @param millis
	 *        the coalesce timeout to set, in milliseconds
	 * @since 1.4
	 */
	public void setCoalesceTimeoutMillis(long millis) {
		setCoalesceTimeout(millis > 0 ? Duration.ofMillis(millis) : null);
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 * @since 1.4
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 * @since 1.4
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the startup delay.
	 *
	 * @return the startup delay, in seconds
	 * @since 1.4
	 */
	public int getStartupDelay() {
		return startupDelay;
	}

	/**
	 * Set the startup delay.
	 *
	 * @param startupDelay
	 *        the delay to set, in seconds
	 * @since 1.4
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

}
