/* ==================================================================
 * TimeoutDatumFilterService.java - 10/12/2025 7:50:33 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Filter to monitor a source ID and generate a datum for that source if a datum
 * is not seen within a configurable timeout.
 *
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public class TimeoutDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, SettingsChangeObserver,
		DatumSourceIdProvider, ServiceLifecycleObserver, Runnable {

	/** The {@code persist} property default value. */
	public static final boolean DEFAULT_PERSIST = false;

	private final OptionalService<DatumQueue> datumQueue;
	private final InstantSource sampleClock;
	private String outputSourceId;
	private Duration timeout;
	private boolean persist = DEFAULT_PERSIST;
	private TaskScheduler taskScheduler;
	private int startupDelay;
	private String tagName;
	private String statusPropertyName;
	private String statusPropertyValue;

	private ScheduledFuture<?> startupFuture;
	private ScheduledFuture<?> timeoutFuture;

	/**
	 * Constructor.
	 *
	 * @param datumQueue
	 *        the datum queue
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public TimeoutDatumFilterService(OptionalService<DatumQueue> datumQueue) {
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
	public TimeoutDatumFilterService(InstantSource sampleClock, OptionalService<DatumQueue> datumQueue) {
		super();
		this.sampleClock = requireNonNullArgument(sampleClock, "sampleClock");
		this.datumQueue = requireNonNullArgument(datumQueue, "datumQueue");
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( startupFuture != null || timeoutFuture != null ) {
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
			resetTimeout();
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
		if ( timeoutFuture != null && !timeoutFuture.isDone() ) {
			timeoutFuture.cancel(true);
			timeoutFuture = null;
		}
	}

	/**
	 * Get the effective timeout.
	 *
	 * @return the timeout, or {@code null} if not applicable
	 */
	private Duration effectiveTimeout() {
		final Duration duration = getTimeout();
		if ( taskScheduler == null || duration == null || duration.compareTo(Duration.ZERO) <= 0 ) {
			return null;
		}
		return duration;
	}

	private synchronized void restartFilter(final int startupDelay) {
		stop();
		Runnable startupTask = new Runnable() {

			@Override
			public void run() {
				synchronized ( TimeoutDatumFilterService.this ) {
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

	private void resetTimeout() {
		final Duration duration = effectiveTimeout();
		if ( duration == null ) {
			return;
		}
		synchronized ( this ) {
			if ( startupFuture != null && !startupFuture.isDone() ) {
				startupFuture.cancel(true);
				startupFuture = null;
			}
			if ( timeoutFuture != null && !timeoutFuture.isDone() ) {
				timeoutFuture.cancel(true);
				timeoutFuture = null;
			}
			timeoutFuture = taskScheduler.schedule(this, Instant.now().plus(duration));
		}
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( startupFuture != null && !startupFuture.isDone() ) {
			resetTimeout();
		}
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		final String outputSourceId = resolvePlaceholders(getOutputSourceId(), parameters);
		if ( outputSourceId == null || outputSourceId.trim().isEmpty() ) {
			incrementIgnoredStats(start);
			return samples;
		}
		resetTimeout();
		incrementStats(start, samples, samples);
		return samples;
	}

	/**
	 * Generate a datum out of the collected samples.
	 */
	@Override
	public synchronized void run() {
		generateDatum(sampleClock.instant());
		final Duration duration = effectiveTimeout();
		if ( duration == null ) {
			return;
		}
		timeoutFuture = taskScheduler.schedule(this, now().plus(duration));
	}

	private void generateDatum(Instant timestamp) {
		final SimpleDatum d = SimpleDatum.nodeDatum(outputSourceId,
				timestamp != null ? timestamp : now(), new DatumSamples());

		final String tagName = nonEmptyString(getTagName());
		if ( tagName != null ) {
			d.addTag(tagName);
		}

		final String propName = nonEmptyString(getStatusPropertyName());
		final String propVal = nonEmptyString(getStatusPropertyValue());
		if ( propName != null && propVal != null ) {
			d.putSampleValue(DatumSamplesType.Status, propName, propVal);
		}

		log.info("Timeout on source [{}] after {}", getOutputSourceId(), getTimeout());
		final DatumQueue dq = service(datumQueue);
		if ( dq != null ) {
			dq.offer(d, persist);
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = getOutputSourceId();
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.timeout";
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

		result.add(new BasicTextFieldSettingSpecifier("timeoutMillis", null));

		result.add(new BasicTextFieldSettingSpecifier("tagName", null));

		result.add(new BasicTextFieldSettingSpecifier("statusPropertyName", null));

		result.add(new BasicTextFieldSettingSpecifier("statusPropertyValue", null));

		result.add(new BasicToggleSettingSpecifier("persist", DEFAULT_PERSIST));

		return result;
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
	 */
	public void setPersist(boolean persist) {
		this.persist = persist;
	}

	/**
	 * Get the timeout.
	 *
	 * @return the timeout
	 */
	public Duration getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout.
	 *
	 * @param timeout
	 *        the timeout to set
	 */
	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get the timeout as milliseconds.
	 *
	 * @return the timeout, in milliseconds
	 */
	public long getTimeoutMillis() {
		final Duration timeout = getTimeout();
		return timeout != null ? timeout.toMillis() : 0L;
	}

	/**
	 * Set the timeout as milliseconds.
	 *
	 * @param millis
	 *        the timeout to set, in milliseconds
	 */
	public void setTimeoutMillis(long millis) {
		setTimeout(millis > 0 ? Duration.ofMillis(millis) : null);
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the startup delay.
	 *
	 * @return the startup delay, in seconds
	 */
	public int getStartupDelay() {
		return startupDelay;
	}

	/**
	 * Set the startup delay.
	 *
	 * @param startupDelay
	 *        the delay to set, in seconds
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Get the tag name.
	 *
	 * @return the name
	 */
	public String getTagName() {
		return tagName;
	}

	/**
	 * Set the tag name.
	 *
	 * @param tagName
	 *        the name to set
	 */
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	/**
	 * Get the status property name.
	 *
	 * @return the name
	 */
	public String getStatusPropertyName() {
		return statusPropertyName;
	}

	/**
	 * Set the status property name.
	 *
	 * @param statusPropertyName
	 *        the name to set
	 */
	public void setStatusPropertyName(String statusPropertyName) {
		this.statusPropertyName = statusPropertyName;
	}

	/**
	 * Get the status property value.
	 *
	 * @return the value
	 */
	public String getStatusPropertyValue() {
		return statusPropertyValue;
	}

	/**
	 * Set the status property value.
	 *
	 * @param statusPropertyValue
	 *        the value to set
	 */
	public void setStatusPropertyValue(String statusPropertyValue) {
		this.statusPropertyValue = statusPropertyValue;
	}

}
